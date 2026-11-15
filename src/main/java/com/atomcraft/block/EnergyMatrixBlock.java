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
 * Controlador da Matriz de Energia (multibloco 3×5×3, ver {@link com.atomcraft.energy.MatrixStructure}).
 * A energia entra e sai pelas Portas da Matriz e pelo Cabo Atômico ligado ao controlador.
 */
public class EnergyMatrixBlock extends MachineBlock {

    public static final MapCodec<EnergyMatrixBlock> CODEC = simpleCodec(EnergyMatrixBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public EnergyMatrixBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCellBlockEntity(pos, state);
    }
}
