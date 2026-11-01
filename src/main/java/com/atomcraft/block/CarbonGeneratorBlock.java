package com.atomcraft.block;

import com.atomcraft.block.entity.CarbonGeneratorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Gerador de Carbono: queima o carbono guardado na rede. */
public class CarbonGeneratorBlock extends MachineBlock {

    public static final MapCodec<CarbonGeneratorBlock> CODEC = simpleCodec(CarbonGeneratorBlock::new);

    public CarbonGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CarbonGeneratorBlockEntity(pos, state);
    }
}
