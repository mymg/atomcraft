package com.atomcraft.block;

import com.atomcraft.block.entity.ReplicatorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Controlador do Replicador Quântico (multibloco 3×3×3, ver
 * {@link com.atomcraft.replication.QuantumStructure}). É ele que conecta na rede de
 * elementos e recebe energia; replica qualquer item pesquisado, inclusive com exóticos.
 */
public class QuantumReplicatorBlock extends MachineBlock {

    public static final MapCodec<QuantumReplicatorBlock> CODEC = simpleCodec(QuantumReplicatorBlock::new);
    /** Estrutura montada (a frente acende). */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public QuantumReplicatorBlock(Properties properties) {
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
        return new ReplicatorBlockEntity(pos, state);
    }
}
