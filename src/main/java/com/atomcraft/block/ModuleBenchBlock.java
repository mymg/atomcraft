package com.atomcraft.block;

import com.atomcraft.block.entity.ModuleBenchBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bancada de Módulos: instala e remove módulos dos equipamentos (exoesqueleto, jetpack, desintegrador),
 * mostra a armadura vestida e recarrega os equipamentos com a energia própria (ligue ao Cabo Atômico ou a
 * cabos de energia de qualquer mod).
 */
public class ModuleBenchBlock extends MachineBlock {

    public static final MapCodec<ModuleBenchBlock> CODEC = simpleCodec(ModuleBenchBlock::new);

    public ModuleBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModuleBenchBlockEntity(pos, state);
    }
}
