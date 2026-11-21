package com.atomcraft.block.entity;

import net.minecraft.network.chat.Component;

/** Controle por redstone das máquinas (aba da interface). */
public enum RedstoneMode {
    /** Trabalha sem parar (ignora redstone). */
    IGNORED,
    /** Só trabalha com sinal de redstone. */
    HIGH,
    /** Só trabalha sem sinal (sinal desliga). */
    LOW,
    /** Cada pulso de redstone liga ou desliga. */
    PULSE;

    private static final RedstoneMode[] VALUES = values();

    public static RedstoneMode byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : IGNORED;
    }

    public RedstoneMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public Component label() {
        return Component.translatable("redstone.atomcraft." + name().toLowerCase());
    }

    public Component description() {
        return Component.translatable("redstone.atomcraft." + name().toLowerCase() + ".desc");
    }
}
