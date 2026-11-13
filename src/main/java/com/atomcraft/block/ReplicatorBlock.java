package com.atomcraft.block;

import com.atomcraft.block.entity.ReplicatorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Replicador Atômico: cria itens pesquisados a partir dos elementos. 2 blocos de altura (ver {@link TallMachineBlock}). */
public class ReplicatorBlock extends TallMachineBlock {

    public static final MapCodec<ReplicatorBlock> CODEC = simpleCodec(ReplicatorBlock::new);

    public ReplicatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntity createMachine(BlockPos pos, BlockState state) {
        return new ReplicatorBlockEntity(pos, state);
    }
}
