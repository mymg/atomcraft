package com.atomcraft.fluid;

import com.atomcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/** Cano de Fluido: guarda só a referência para a rede de canos (refeita quando algo muda). */
public class PipeBlockEntity extends BlockEntity {

    @Nullable PipeNetwork network;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, state);
    }

    public FluidTier tier() {
        return getBlockState().getBlock() instanceof PipeBlock pipe ? pipe.tier() : FluidTier.BASIC;
    }

    public PipeNetwork network() {
        if (network == null || !network.isValid()) network = PipeNetwork.build(level, worldPosition, tier());
        return network;
    }

    public void serverTick() {
        if (level != null) network().tick(level);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (network != null) network.invalidate();
    }

    /** O que outros blocos veem ao inserir fluido neste cano (bomba empurrando, balde, outro mod). */
    public @Nullable ResourceHandler<FluidResource> handler(@Nullable Direction side) {
        if (side != null && PipeBlock.side(getBlockState(), side) == PipeConnection.DISABLED) return null;
        return new Inlet(side);
    }

    private final class Inlet implements ResourceHandler<FluidResource> {
        private final @Nullable Direction side;

        Inlet(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            return FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return tier().pipeRate();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return true;
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (level == null || resource.isEmpty() || amount <= 0) return 0;
            BlockPos source = side == null ? null : worldPosition.relative(side);
            return network().insertFromOutside(level, resource, amount, source, transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
