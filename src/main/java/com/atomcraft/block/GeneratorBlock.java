package com.atomcraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Base dos geradores: como a fornalha, têm o estado "lit" enquanto geram energia.
 * O blockstate troca a frente para a textura {@code <gerador>_front_on} e o bloco emite luz.
 */
public abstract class GeneratorBlock extends MachineBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    protected GeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    /** Luz emitida: 13 aceso (como a fornalha), 0 apagado. */
    public static int lightLevel(BlockState state) {
        return state.hasProperty(LIT) && state.getValue(LIT) ? 13 : 0;
    }
}
