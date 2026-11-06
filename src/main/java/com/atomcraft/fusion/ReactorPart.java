package com.atomcraft.fusion;

/** Peças da estrutura do Reator de Fusão. */
public enum ReactorPart {
    CASING("reactor_casing", 0xFF8FA3B5),
    GLASS("reactor_glass", 0xFF7FE6FF),
    COIL("magnetic_coil", 0xFFE08A3A),
    SOLENOID("central_solenoid", 0xFFB070FF),
    PORT("reactor_port", 0xFF5CE07A),
    CONTROLLER("fusion_reactor", 0xFFFF5EC8),
    /** Espaço do canal de plasma: precisa estar vazio. */
    AIR("air", 0xFFFFFFFF);

    private final String id;
    private final int color;

    ReactorPart(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    /** Cor da prévia da estrutura (partículas). */
    public int color() {
        return color;
    }

    public String translationKey() {
        return this == AIR ? "gui.atomcraft.part_air" : "block.atomcraft." + id;
    }
}
