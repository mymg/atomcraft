package com.atomcraft.block;

import com.atomcraft.block.entity.ReactorPortBlockEntity;
import com.atomcraft.fusion.ReactorPart;
import com.atomcraft.storage.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Porta do Reator: conecta a estrutura à rede de elementos (Cabo Atômico) e a
 * cabos de energia de qualquer mod. Pode ficar no lugar de qualquer carcaça.
 */
public class ReactorPortBlock extends ReactorPartBlock implements EntityBlock, NetworkNode {

    public ReactorPortBlock(Properties properties) {
        super(ReactorPart.PORT, properties);
    }

    /** A porta continua visível com o reator formado, para o jogador saber onde ligar os cabos. */
    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorPortBlockEntity(pos, state);
    }
}
