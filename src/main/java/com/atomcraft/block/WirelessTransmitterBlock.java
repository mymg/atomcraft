package com.atomcraft.block;

import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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

    /** Base e antena (modelo do Blockbench {@code wireless_transmitter}); não é um cubo inteiro. */
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16), Block.box(3, 3, 3, 13, 15, 13));

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                   net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    // Não bloqueia luz: as faces de dentro do modelo usam a luz do próprio bloco e ficariam pretas.
    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessTransmitterBlockEntity(pos, state);
    }
}
