package com.atomcraft.storage;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Marca blocos que fazem parte da rede de elementos: cabos, Drives e máquinas.
 * Blocos marcados que se encostam ficam conectados.
 */
public interface NetworkNode {

    /** Se este estado do bloco conecta na rede (as máquinas de 2 blocos só conectam pela metade de baixo). */
    default boolean isNetworkNode(BlockState state) {
        return true;
    }

    static boolean connects(BlockState state) {
        return state.getBlock() instanceof NetworkNode node && node.isNetworkNode(state);
    }
}
