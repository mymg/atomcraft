package com.atomcraft.block;

import com.atomcraft.block.entity.EnergyCellBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Controlador da Matriz de Energia (multibloco 3×5×3, ver {@link com.atomcraft.energy.MatrixStructure}),
 * no meio da frente da camada de baixo. A energia entra e sai pelas Interfaces da Matriz.
 */
public class EnergyMatrixBlock extends MachineBlock {

    public static final MapCodec<EnergyMatrixBlock> CODEC = simpleCodec(EnergyMatrixBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Tem energia guardada (a bobina do modelo acende). */
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

    public EnergyMatrixBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(CHARGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED, CHARGED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** Cabos só ligam nas Interfaces da Matriz; o controlador entra na rede por elas. */
    @Override
    public boolean isNetworkNode(BlockState state) {
        return false;
    }

    /** Formada, quem desenha é o modelo grande (o controlador é a frente da base do modelo). */
    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? net.minecraft.world.level.block.RenderShape.INVISIBLE : net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state) {
        return state.getValue(FORMED) ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return state.getValue(FORMED) ? 0 : super.getLightDampening(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(FORMED) || super.propagatesSkylightDown(state);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCellBlockEntity(pos, state);
    }
}
