package com.atomcraft.block;

import com.atomcraft.block.entity.AlloyForgeBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Forja de Ligas: produz ligas e metais a partir dos elementos da rede. */
public class AlloyForgeBlock extends MachineBlock {

    public static final MapCodec<AlloyForgeBlock> CODEC = simpleCodec(AlloyForgeBlock::new);

    public AlloyForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlloyForgeBlockEntity(pos, state);
    }
}
