package com.atomcraft.storage;

import net.minecraft.core.BlockPos;

import java.util.Collection;

/**
 * Ligação "invisível" entre blocos da rede que não se encostam: as portas de um multibloco
 * ligam-se ao controlador (e o controlador às portas). Assim um Cabo Atômico ligado a qualquer
 * porta põe o multibloco inteiro na rede — elementos, energia e itens.
 */
public interface NetworkLink {

    /** Outros nós da rede ligados a este (vazio se o multibloco não estiver formado). */
    Collection<BlockPos> linkedNodes();
}
