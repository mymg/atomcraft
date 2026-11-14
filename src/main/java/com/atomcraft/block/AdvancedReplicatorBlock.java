package com.atomcraft.block;

import com.atomcraft.block.entity.AdvancedReplicatorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Replicador Avançado: replica itens mais complexos que o Replicador Atômico. 2 blocos de altura (ver {@link TallMachineBlock}). */
public class AdvancedReplicatorBlock extends TallMachineBlock {

    public static final MapCodec<AdvancedReplicatorBlock> CODEC = simpleCodec(AdvancedReplicatorBlock::new);

    public AdvancedReplicatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntity createMachine(BlockPos pos, BlockState state) {
        return new AdvancedReplicatorBlockEntity(pos, state);
    }
}
