package com.atomcraft.block.entity;

import com.atomcraft.AtomCraft;
import com.atomcraft.Config;
import com.atomcraft.menu.ChunkLoaderMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.EnergyReceiver;
import com.atomcraft.upgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Carregador de Chunks: na interface, o jogador escolhe quais chunks em volta (grade de até 5×5, a do meio
 * sempre carregada) ficam carregadas. Cada chunk custa energia por tick; sem energia, desligado, sem
 * permissão da redstone ou desativado na configuração, as chunks são liberadas.
 */
public class ChunkLoaderBlockEntity extends MachineBlockEntity implements EnergyReceiver {

    /** Ticket do NeoForge para as chunks forçadas (validado ao carregar o mundo). */
    public static final TicketController TICKETS = new TicketController(AtomCraft.id("chunk_loader"), (level, helper) -> {
        for (BlockPos owner : java.util.List.copyOf(helper.getBlockTickets().keySet())) {
            if (!Config.CHUNK_LOADER_ENABLED.get() || !(level.getBlockEntity(owner) instanceof ChunkLoaderBlockEntity)) {
                helper.removeAllTickets(owner);
            }
        }
    });

    public static final int MAX_RADIUS = 2, GRID = MAX_RADIUS * 2 + 1, CENTER = GRID * GRID / 2;
    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAPACITY_LO = 2, DATA_CAPACITY_HI = 3,
            DATA_MASK_LO = 4, DATA_MASK_HI = 5, DATA_LOADED = 6, DATA_RADIUS = 7, DATA_MAX = 8, DATA_COST = 9, DATA_COUNT = 10;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(Config.CHUNK_LOADER_CAPACITY.get(),
            Math.max(1, Config.CHUNK_LOADER_CAPACITY.get() / 20), 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    /** Chunks escolhidas: um bit por célula da grade (linha × GRID + coluna), a do meio sempre ligada. */
    private int mask = 1 << CENTER;
    /** Chunks forçadas agora (pacotes de ChunkPos). */
    private final Set<Long> forced = new HashSet<>();
    /** Na primeira atualização depois de carregar, solta tickets que ficaram salvos no mundo. */
    private boolean cleaned;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int stored = energy.getAmountAsInt(), capacity = energy.getCapacityAsInt();
            return switch (index) {
                case DATA_ENERGY_LO -> stored & 0xFFFF;
                case DATA_ENERGY_HI -> (stored >>> 16) & 0xFFFF;
                case DATA_CAPACITY_LO -> capacity & 0xFFFF;
                case DATA_CAPACITY_HI -> (capacity >>> 16) & 0xFFFF;
                case DATA_MASK_LO -> mask & 0xFFFF;
                case DATA_MASK_HI -> (mask >>> 16) & 0xFFFF;
                case DATA_LOADED -> forced.size();
                case DATA_RADIUS -> radius();
                case DATA_MAX -> Config.CHUNK_LOADER_MAX_CHUNKS.get();
                case DATA_COST -> Config.CHUNK_LOADER_ENERGY.get();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHUNK_LOADER.get(), pos, state);
    }

    public static int radius() {
        return Math.min(MAX_RADIUS, Config.CHUNK_LOADER_RADIUS.get());
    }

    @Override
    public SimpleEnergyHandler energy() {
        return energy;
    }

    public ContainerData data() {
        return data;
    }

    public int mask() {
        return mask;
    }

    public int selectedCount() {
        return Integer.bitCount(mask);
    }

    public Set<Long> forced() {
        return forced;
    }

    /** Liga/desliga a célula da grade (a do meio não sai; respeita o raio e o máximo da configuração). */
    public void toggle(int cell) {
        if (cell < 0 || cell >= GRID * GRID || cell == CENTER) return;
        int dx = cell % GRID - MAX_RADIUS, dz = cell / GRID - MAX_RADIUS;
        if (Math.abs(dx) > radius() || Math.abs(dz) > radius()) return;
        boolean on = (mask & (1 << cell)) != 0;
        if (!on && selectedCount() >= Config.CHUNK_LOADER_MAX_CHUNKS.get()) return;
        mask ^= 1 << cell;
        setChanged();
        updateTickets();
    }

    /** Energia por tick com as chunks escolhidas. */
    public int costPerTick() {
        return selectedCount() * Config.CHUNK_LOADER_ENERGY.get();
    }

    /** Solta qualquer ticket salvo deste carregador (a lista "forced" não é salva; é refeita no tick). */
    private void cleanSaved() {
        if (cleaned || !(level instanceof ServerLevel server)) return;
        cleaned = true;
        ChunkPos center = ChunkPos.containing(worldPosition);
        for (int dx = -MAX_RADIUS; dx <= MAX_RADIUS; dx++) {
            for (int dz = -MAX_RADIUS; dz <= MAX_RADIUS; dz++) {
                TICKETS.forceChunk(server, worldPosition, center.x() + dx, center.z() + dz, false, true);
            }
        }
    }

    @Override
    public void serverTick() {
        cleanSaved();
        if (!Config.CHUNK_LOADER_ENABLED.get()) {
            status = MachineStatus.DISABLED;
            release();
            return;
        }
        int cost = costPerTick();
        if (energy.getAmountAsInt() < cost) {
            status = MachineStatus.NO_ENERGY;
            release();
            return;
        }
        energy.set(energy.getAmountAsInt() - cost);
        status = MachineStatus.WORKING;
        if (forced.size() != selectedCount()) updateTickets();
    }

    @Override
    protected void disabledTick() {
        cleanSaved();
        release();
    }

    /** Força as chunks escolhidas e solta as que saíram da seleção. */
    private void updateTickets() {
        if (!(level instanceof ServerLevel server) || status != MachineStatus.WORKING) return;
        ChunkPos center = ChunkPos.containing(worldPosition);
        Set<Long> wanted = new HashSet<>();
        int r = radius();
        for (int cell = 0; cell < GRID * GRID; cell++) {
            if ((mask & (1 << cell)) == 0) continue;
            int dx = cell % GRID - MAX_RADIUS, dz = cell / GRID - MAX_RADIUS;
            if (Math.abs(dx) > r || Math.abs(dz) > r) continue;
            wanted.add(ChunkPos.pack(center.x() + dx, center.z() + dz));
        }
        for (Long chunk : Set.copyOf(forced)) {
            if (!wanted.contains(chunk)) {
                TICKETS.forceChunk(server, worldPosition, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
                forced.remove(chunk);
            }
        }
        for (Long chunk : wanted) {
            if (forced.add(chunk)) TICKETS.forceChunk(server, worldPosition, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
        }
    }

    /** Solta todas as chunks forçadas. */
    public void release() {
        if (forced.isEmpty() || !(level instanceof ServerLevel server)) return;
        for (Long chunk : forced) TICKETS.forceChunk(server, worldPosition, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
        forced.clear();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        release();
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.chunk_loader");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ChunkLoaderMenu(id, inventory, this);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
        mask = input.getIntOr("chunks", 1 << CENTER) | (1 << CENTER);
        forced.clear();
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        output.putInt("chunks", mask);
    }
}
