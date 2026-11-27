package com.atomcraft.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rede de canos ligados (mesmo nível). Não guarda fluido: a cada tick move até a vazão do nível, direto
 * dos blocos em modo "puxar" (e do que for inserido de fora) para os blocos em modo normal, em rodízio.
 * Assim nada se perde quando a rede muda ou o mundo é salvo.
 */
public final class PipeNetwork {

    /** Bloco com fluido ligado à rede: a posição dele, o lado por onde o cano encosta e o modo. */
    record Endpoint(BlockPos pos, Direction face, boolean pull) {}

    private final FluidTier tier;
    private final Set<BlockPos> pipes;
    private final List<Endpoint> endpoints;
    private boolean valid = true;
    private long lastTick = Long.MIN_VALUE;
    private int rotation;
    private final Budget budget = new Budget();

    private PipeNetwork(FluidTier tier, Set<BlockPos> pipes, List<Endpoint> endpoints) {
        this.tier = tier;
        this.pipes = pipes;
        this.endpoints = endpoints;
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        valid = false;
    }

    public int size() {
        return pipes.size();
    }

    public List<Endpoint> endpoints() {
        return endpoints;
    }

    /** Refaz as redes do cano em {@code pos} e dos canos vizinhos (colocado, quebrado ou lado trocado). */
    public static void invalidateAround(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipe && pipe.network != null) pipe.network.invalidate();
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof PipeBlockEntity pipe && pipe.network != null) {
                pipe.network.invalidate();
            }
        }
    }

    static PipeNetwork build(@Nullable Level level, BlockPos start, FluidTier tier) {
        Set<BlockPos> pipes = new HashSet<>();
        List<Endpoint> endpoints = new ArrayList<>();
        if (level == null) return new PipeNetwork(tier, pipes, endpoints);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        pipes.add(start);
        List<PipeBlockEntity> members = new ArrayList<>();
        while (!queue.isEmpty() && pipes.size() < 4096) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PipeBlock)) continue;
            if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) members.add(pipe);
            for (Direction direction : Direction.values()) {
                PipeConnection connection = PipeBlock.side(state, direction);
                if (!connection.hasArm()) continue;
                BlockPos next = pos.relative(direction);
                BlockState neighbor = level.getBlockState(next);
                if (neighbor.getBlock() instanceof PipeBlock pipe) {
                    if (pipe.tier() == tier && PipeBlock.side(neighbor, direction.getOpposite()) == PipeConnection.NORMAL && pipes.add(next)) {
                        queue.add(next);
                    }
                } else {
                    endpoints.add(new Endpoint(next, direction.getOpposite(), connection == PipeConnection.PULL));
                }
            }
        }
        PipeNetwork network = new PipeNetwork(tier, pipes, endpoints);
        for (PipeBlockEntity member : members) {
            if (member.network != null && member.network != network) member.network.invalidate();
            member.network = network;
        }
        return network;
    }

    private static @Nullable ResourceHandler<FluidResource> handler(Level level, Endpoint endpoint) {
        if (!level.isLoaded(endpoint.pos())) return null;
        return level.getCapability(Capabilities.Fluid.BLOCK, endpoint.pos(), endpoint.face());
    }

    private void newTick(Level level) {
        long now = level.getGameTime();
        if (now != lastTick) {
            lastTick = now;
            budget.used = 0;
        }
    }

    /** Uma vez por tick (o primeiro cano da rede a rodar faz o trabalho). */
    void tick(Level level) {
        if (level.getGameTime() == lastTick) return;
        newTick(level);
        rotation++;
        for (Endpoint source : endpoints) {
            if (!source.pull()) continue;
            if (budget.left() <= 0) break;
            ResourceHandler<FluidResource> handler = handler(level, source);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.size() && budget.left() > 0; slot++) {
                FluidResource resource = handler.getResource(slot);
                if (resource.isEmpty() || handler.getAmountAsLong(slot) <= 0) continue;
                int movable;
                try (Transaction simulation = Transaction.openRoot()) {
                    int extracted = handler.extract(resource, budget.left(), simulation);
                    movable = extracted <= 0 ? 0 : route(level, resource, extracted, source.pos(), simulation);
                }
                if (movable <= 0) continue;
                try (Transaction transaction = Transaction.openRoot()) {
                    int extracted = handler.extract(resource, movable, transaction);
                    int inserted = route(level, resource, extracted, source.pos(), transaction);
                    if (inserted == extracted && inserted > 0) {
                        budget.used += inserted;
                        transaction.commit();
                    }
                }
            }
        }
    }

    /** Fluido inserido de fora (bomba, balde): vai direto para os destinos, dentro da vazão do tick. */
    int insertFromOutside(Level level, FluidResource resource, int amount, @Nullable BlockPos source, TransactionContext transaction) {
        newTick(level);
        int allowed = Math.min(amount, budget.left());
        if (allowed <= 0) return 0;
        int inserted = route(level, resource, allowed, source, transaction);
        if (inserted > 0) budget.consume(inserted, transaction);
        return inserted;
    }

    /** Entrega aos blocos em modo normal (menos a origem), começando por um diferente a cada tick. */
    private int route(Level level, FluidResource resource, int amount, @Nullable BlockPos except, TransactionContext transaction) {
        int inserted = 0;
        int count = endpoints.size();
        for (int i = 0; i < count && inserted < amount; i++) {
            Endpoint target = endpoints.get(Math.floorMod(rotation + i, count));
            if (target.pull() || target.pos().equals(except)) continue;
            ResourceHandler<FluidResource> handler = handler(level, target);
            if (handler == null) continue;
            inserted += handler.insert(resource, amount - inserted, transaction);
        }
        return inserted;
    }

    /** Quanto já passou pela rede neste tick, desfeito se a transação de quem inseriu for cancelada. */
    private final class Budget extends SnapshotJournal<Integer> {
        int used;

        int left() {
            return tier.pipeRate() - used;
        }

        void consume(int amount, TransactionContext transaction) {
            updateSnapshots(transaction);
            used += amount;
        }

        @Override
        protected Integer createSnapshot() {
            return used;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            used = snapshot;
        }
    }
}
