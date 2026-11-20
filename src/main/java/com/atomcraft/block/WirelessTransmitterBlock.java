package com.atomcraft.block;

import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Transmissor Sem Fio: leva a rede (elementos e energia) até os equipamentos de quem tem um Cartão de Rede vinculado. */
public class WirelessTransmitterBlock extends MachineBlock {

    public static final MapCodec<WirelessTransmitterBlock> CODEC = simpleCodec(WirelessTransmitterBlock::new);

    public WirelessTransmitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessTransmitterBlockEntity(pos, state);
    }
}
