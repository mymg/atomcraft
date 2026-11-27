package com.atomcraft.fluid;

import net.minecraft.util.StringRepresentable;

/**
 * Estado de cada lado de um cano: sem nada, ligado (a outro cano, ou entregando a um bloco), puxando
 * do bloco ao lado, ou desligado pelo jogador (agachado + mão vazia).
 */
public enum PipeConnection implements StringRepresentable {
    NONE("none"),
    NORMAL("normal"),
    PULL("pull"),
    DISABLED("disabled");

    private final String name;

    PipeConnection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean hasArm() {
        return this == NORMAL || this == PULL;
    }
}
