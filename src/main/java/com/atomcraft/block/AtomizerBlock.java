package com.atomcraft.block;

import com.atomcraft.block.entity.AtomizerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Atomizador: transforma itens em elementos. */
public class AtomizerBlock extends MachineBlock {

    public static final MapCodec<AtomizerBlock> CODEC = simpleCodec(AtomizerBlock::new);

    public AtomizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtomizerBlockEntity(pos, state);
    }
}
