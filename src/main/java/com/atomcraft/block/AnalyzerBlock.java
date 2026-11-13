package com.atomcraft.block;

import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Analisador: pesquisa itens para o Replicador. 2 blocos de altura (ver {@link TallMachineBlock}). */
public class AnalyzerBlock extends TallMachineBlock {

    public static final MapCodec<AnalyzerBlock> CODEC = simpleCodec(AnalyzerBlock::new);

    public AnalyzerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntity createMachine(BlockPos pos, BlockState state) {
        return new AnalyzerBlockEntity(pos, state);
    }
}
