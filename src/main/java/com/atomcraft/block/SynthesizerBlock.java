package com.atomcraft.block;

import com.atomcraft.block.entity.SynthesizerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Sintetizador: fabrica itens exclusivos a partir de schematics. */
public class SynthesizerBlock extends MachineBlock {

    public static final MapCodec<SynthesizerBlock> CODEC = simpleCodec(SynthesizerBlock::new);

    public SynthesizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SynthesizerBlockEntity(pos, state);
    }
}
