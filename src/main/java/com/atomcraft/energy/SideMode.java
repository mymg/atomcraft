package com.atomcraft.energy;

import net.minecraft.network.chat.Component;

/** O que um lado da célula faz com a energia. */
public enum SideMode {
    INPUT(0xFF3A8FE0), OUTPUT(0xFFE08A2E), BOTH(0xFF2EB86E), NONE(0xFF4A4F5A);

    private static final SideMode[] VALUES = values();

    private final int color;

    SideMode(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    public boolean receives() {
        return this == INPUT || this == BOTH;
    }

    public boolean sends() {
        return this == OUTPUT || this == BOTH;
    }

    /** Ordem do clique: entrada → saída → ambos → desligado. */
    public SideMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public Component label() {
        return Component.translatable("side_mode.atomcraft." + name().toLowerCase());
    }

    public static SideMode byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }
}
