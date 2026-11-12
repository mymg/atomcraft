package com.atomcraft.block;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Block;
import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Analisador: pesquisa itens para o Replicador. */
public class AnalyzerBlock extends MachineBlock {

    public static final MapCodec<AnalyzerBlock> CODEC = simpleCodec(AnalyzerBlock::new);

    /** Analisando (troca a animação de "idle" para "analyzing"). */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public AnalyzerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    /** O modelo animado é desenhado pelo GeckoLib. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnalyzerBlockEntity(pos, state);
    }
}
