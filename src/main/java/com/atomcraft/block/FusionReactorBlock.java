package com.atomcraft.block;

import com.atomcraft.block.entity.FusionReactorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Reator de Fusão: funde hidrogênio em hélio. */
public class FusionReactorBlock extends MachineBlock {

    public static final MapCodec<FusionReactorBlock> CODEC = simpleCodec(FusionReactorBlock::new);

    public FusionReactorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionReactorBlockEntity(pos, state);
    }
}
