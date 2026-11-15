package com.atomcraft.block;

import com.atomcraft.block.entity.EnergyCellBlockEntity;
import com.atomcraft.energy.CellTier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Célula de Energia (Básica, Avançada ou Elite): guarda FE, com entrada/saída configurável por lado. */
public class EnergyCellBlock extends MachineBlock {

    public static final MapCodec<EnergyCellBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.xmap(CellTier::valueOf, CellTier::name).fieldOf("tier").forGetter(EnergyCellBlock::tier),
            propertiesCodec()).apply(i, (tier, properties) -> new EnergyCellBlock(properties, tier)));

    private final CellTier tier;

    public EnergyCellBlock(Properties properties, CellTier tier) {
        super(properties);
        this.tier = tier;
    }

    public CellTier tier() {
        return tier;
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
