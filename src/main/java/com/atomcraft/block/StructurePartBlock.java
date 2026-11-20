package com.atomcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Peça de multibloco que some com a estrutura formada (quem desenha é o modelo grande do controlador).
 * Invisível, ela não pode escurecer o modelo nem esconder as faces dos vizinhos.
 */
public abstract class StructurePartBlock extends Block {

    public static final BooleanProperty FORMED = ReactorPartBlock.FORMED;

    protected StructurePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    /** Fica escondida quando formada (as portas/interfaces continuam visíveis). */
    protected boolean hiddenWhenFormed(BlockState state) {
        return state.hasProperty(FORMED) && state.getValue(FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return hiddenWhenFormed(state) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return hiddenWhenFormed(state) ? Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return hiddenWhenFormed(state) ? 0 : super.getLightDampening(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return hiddenWhenFormed(state) || super.propagatesSkylightDown(state);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return hiddenWhenFormed(state) ? 1.0f : super.getShadeBrightness(state, level, pos);
    }

    /** Marca/desmarca as peças da lista como formadas. */
    public static void setFormed(net.minecraft.world.level.Level level, Iterable<BlockPos> positions, boolean value) {
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof StructurePartBlock && state.getValue(FORMED) != value) {
                level.setBlock(pos, state.setValue(FORMED, value), Block.UPDATE_CLIENTS);
            }
        }
    }
}
