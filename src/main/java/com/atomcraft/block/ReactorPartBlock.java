package com.atomcraft.block;

import com.atomcraft.fusion.ReactorPart;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Peça da estrutura do Reator de Fusão (carcaça, vidro, bobina, solenoide).
 * Com o reator formado a peça fica invisível: quem desenha é o modelo grande do controlador.
 */
public class ReactorPartBlock extends Block {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final ReactorPart part;
    private final MapCodec<ReactorPartBlock> codec;

    public ReactorPartBlock(ReactorPart part, Properties properties) {
        super(properties);
        this.part = part;
        this.codec = simpleCodec(p -> new ReactorPartBlock(part, p));
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    public ReactorPart part() {
        return part;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    // Formado, o bloco fica invisível: não pode escurecer o modelo grande nem esconder as faces
    // dos blocos vizinhos (senão dá para ver através do chão).
    protected boolean hiddenWhenFormed(BlockState state) {
        return state.hasProperty(FORMED) && state.getValue(FORMED);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state) {
        return hiddenWhenFormed(state) ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return hiddenWhenFormed(state) ? 0 : super.getLightDampening(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return hiddenWhenFormed(state) || super.propagatesSkylightDown(state);
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return hiddenWhenFormed(state) ? 1.0f : super.getShadeBrightness(state, level, pos);
    }
}
