package com.atomcraft.block;

import com.atomcraft.block.entity.CombustionGeneratorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Gerador de Combustão: queima itens com carbono. */
public class CombustionGeneratorBlock extends MachineBlock {

    public static final MapCodec<CombustionGeneratorBlock> CODEC = simpleCodec(CombustionGeneratorBlock::new);

    public CombustionGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CombustionGeneratorBlockEntity(pos, state);
    }
}
