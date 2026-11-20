package com.atomcraft.energy;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Modo de uma Interface da Matriz de Energia: só recebe, só envia, ou os dois (padrão). */
public enum PortMode implements StringRepresentable {
    BOTH("both", true, true, 0xFFE0473C),
    INPUT("input", true, false, 0xFF3A8FE0),
    OUTPUT("output", false, true, 0xFFF08A24);

    private final String name;
    private final boolean receives, sends;
    private final int color;

    PortMode(String name, boolean receives, boolean sends, int color) {
        this.name = name;
        this.receives = receives;
        this.sends = sends;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean receives() {
        return receives;
    }

    /** Envia ativamente para os vizinhos (só no modo saída; "ambos" deixa tirar, mas não empurra). */
    public boolean pushes() {
        return this == OUTPUT;
    }

    public boolean sends() {
        return sends;
    }

    public int color() {
        return color;
    }

    public PortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Component label() {
        return Component.translatable("port_mode.atomcraft." + name);
    }
}
