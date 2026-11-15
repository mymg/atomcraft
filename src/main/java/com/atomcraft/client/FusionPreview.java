package com.atomcraft.client;

import com.atomcraft.block.MachineBlock;
import com.atomcraft.block.FusionReactorBlock;
import com.atomcraft.fusion.FusionStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashSet;
import java.util.Set;

/**
 * Prévia da estrutura do Reator de Fusão (e do Replicador Quântico): partículas coloridas marcam onde falta
 * cada peça (a cor indica o tipo). Some sozinha quando o reator forma.
 */
public final class FusionPreview {

    private static final Set<BlockPos> ACTIVE = new HashSet<>();
    private static int timer;

    private FusionPreview() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FusionPreview::onTick);
    }

    public static boolean isShowing(BlockPos controller) {
        return ACTIVE.contains(controller);
    }

    public static void toggle(BlockPos controller) {
        if (!ACTIVE.remove(controller)) ACTIVE.add(controller.immutable());
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
        ACTIVE.removeIf(pos -> {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof com.atomcraft.block.EnergyMatrixBlock) {
                // Matriz de Energia: estrutura 3×5×3
                if (state.getValue(com.atomcraft.block.EnergyMatrixBlock.FORMED)) return true;
                if (minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64 * 64) return true;
                for (var slot : com.atomcraft.energy.MatrixStructure.check(level, pos, state.getValue(MachineBlock.FACING)).missing()) {
                    BlockPos p = slot.pos();
                    level.addParticle(new DustParticleOptions(slot.part().color() & 0xFFFFFF, 1.2f), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, 0, 0);
                }
                return false;
            }
            if (state.getBlock() instanceof com.atomcraft.block.QuantumReplicatorBlock) {
                // Replicador Quântico: mesma prévia, com a estrutura 3×3×3
                if (state.getValue(com.atomcraft.block.QuantumReplicatorBlock.FORMED)) return true;
                if (minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64 * 64) return true;
                for (var slot : com.atomcraft.replication.QuantumStructure.check(level, pos, state.getValue(MachineBlock.FACING)).missing()) {
                    BlockPos p = slot.pos();
                    level.addParticle(new DustParticleOptions(slot.part().color() & 0xFFFFFF, 1.2f), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, 0, 0);
                }
                return false;
            }
            if (!(state.getBlock() instanceof FusionReactorBlock) || state.getValue(FusionReactorBlock.FORMED)) return true;
            if (minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 64 * 64) return true;
            Direction facing = state.getValue(MachineBlock.FACING);
            for (FusionStructure.Slot slot : FusionStructure.check(level, pos, facing).missing()) {
                BlockPos p = slot.pos();
                int color = slot.part().color() & 0xFFFFFF;
                level.addParticle(new DustParticleOptions(color, 1.2f), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, 0, 0);
            }
            return false;
        });
    }
}
