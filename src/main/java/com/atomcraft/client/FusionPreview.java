package com.atomcraft.client;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.EnergyMatrixBlock;
import com.atomcraft.block.FusionReactorBlock;
import com.atomcraft.block.MachineBlock;
import com.atomcraft.block.QuantumReplicatorBlock;
import com.atomcraft.energy.MatrixStructure;
import com.atomcraft.fusion.FusionStructure;
import com.atomcraft.fusion.ReactorPart;
import com.atomcraft.replication.QuantumStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prévia (holograma) das estruturas multibloco: Reator de Fusão, Replicador Quântico e Matriz de Energia.
 * Ligada pelo botão "Mostrar estrutura" da interface do controlador; cada peça que falta aparece como
 * um bloco fantasma no lugar certo (desenhado pelo renderizador do controlador, ver {@link
 * com.atomcraft.client.render.Holograms}). Some sozinha quando a estrutura forma.
 */
public final class FusionPreview {

    /** Peça que falta: o item do bloco a colocar, ou vazio quando o lugar precisa ficar livre. */
    public record Ghost(BlockPos pos, ItemStack stack) {}

    private static final Map<BlockPos, List<Ghost>> ACTIVE = new HashMap<>();
    private static int timer;

    private FusionPreview() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FusionPreview::onTick);
    }

    public static boolean isShowing(BlockPos controller) {
        return ACTIVE.containsKey(controller);
    }

    public static void toggle(BlockPos controller) {
        if (ACTIVE.remove(controller) == null) {
            ACTIVE.put(controller.immutable(), List.of());
            timer = 99;
        }
    }

    /** Peças que faltam na estrutura do controlador (vazio se a prévia estiver desligada). */
    public static List<Ghost> ghosts(BlockPos controller) {
        return ACTIVE.getOrDefault(controller, List.of());
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.player == null) {
            ACTIVE.clear();
            return;
        }
        if (ACTIVE.isEmpty() || ++timer < 8) return;
        timer = 0;
        var iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            BlockPos pos = entry.getKey();
            List<Ghost> ghosts = compute(level, pos);
            if (ghosts == null || minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64 * 64) iterator.remove();
            else entry.setValue(ghosts);
        }
    }

    /** null quando não há mais estrutura a mostrar (formou ou o controlador sumiu). */
    private static List<Ghost> compute(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(MachineBlock.FACING)) return null;
        Direction facing = state.getValue(MachineBlock.FACING);
        List<Ghost> ghosts = new ArrayList<>();
        if (state.getBlock() instanceof EnergyMatrixBlock) {
            if (state.getValue(EnergyMatrixBlock.FORMED)) return null;
            for (var slot : MatrixStructure.check(level, pos, facing).missing()) ghosts.add(new Ghost(slot.pos(), stackFor(slot.part().translationKey())));
        } else if (state.getBlock() instanceof QuantumReplicatorBlock) {
            if (state.getValue(QuantumReplicatorBlock.FORMED)) return null;
            for (var slot : QuantumStructure.check(level, pos, facing).missing()) ghosts.add(new Ghost(slot.pos(), stackFor(slot.part().translationKey())));
        } else if (state.getBlock() instanceof FusionReactorBlock) {
            if (state.getValue(FusionReactorBlock.FORMED)) return null;
            for (FusionStructure.Slot slot : FusionStructure.check(level, pos, facing).missing()) {
                ghosts.add(new Ghost(slot.pos(), slot.part() == ReactorPart.AIR ? ItemStack.EMPTY : stackFor(slot.part().translationKey())));
            }
        } else {
            return null;
        }
        return ghosts;
    }

    private static ItemStack stackFor(String translationKey) {
        String id = translationKey.substring(translationKey.lastIndexOf('.') + 1);
        var block = BuiltInRegistries.BLOCK.getValue(AtomCraft.id(id));
        return new ItemStack(block.asItem());
    }
}
