package com.atomcraft.block;

import com.atomcraft.block.entity.FusionReactorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Controlador do Reator de Fusão. Fica no centro da parede da frente da
 * estrutura 7×7×5 (camada do meio), virado para fora. Com a estrutura formada,
 * o bloco some e o controlador desenha o modelo grande do reator.
 */
public class FusionReactorBlock extends MachineBlock {

    public static final MapCodec<FusionReactorBlock> CODEC = simpleCodec(FusionReactorBlock::new);
    public static final BooleanProperty FORMED = ReactorPartBlock.FORMED;
    /** Plasma aceso (para a animação e a luz). */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public FusionReactorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED, ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionReactorBlockEntity(pos, state);
    }

    // Formado, o bloco fica invisível: não pode escurecer o modelo grande nem esconder as faces
    // dos blocos vizinhos (senão dá para ver através do chão).
    protected boolean hiddenWhenFormed(BlockState state) {
        return state.hasProperty(FORMED) && state.getValue(FORMED);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state) {
        return hiddenWhenFormed(state) ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected int getLightBlock(BlockState state) {
        return hiddenWhenFormed(state) ? 0 : super.getLightBlock(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return hiddenWhenFormed(state) || super.propagatesSkylightDown(state);
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return hiddenWhenFormed(state) ? 1.0f : super.getShadeBrightness(state, level, pos);
    }
}
