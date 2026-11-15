package com.atomcraft.replication;

/** Peças da estrutura do Replicador Quântico (a cor marca a peça na prévia da estrutura). */
public enum QuantumPart {
    CASING(0xFF8FA3C8),
    GLASS(0xFF7FE0FF),
    CORE(0xFFE070FF),
    PORT(0xFF5AD0A0),
    CONTROLLER(0xFFFFFFFF);

    private final int color;

    QuantumPart(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    public String translationKey() {
        return switch (this) {
            case CASING -> "block.atomcraft.quantum_casing";
            case GLASS -> "block.atomcraft.quantum_glass";
            case CORE -> "block.atomcraft.quantum_core";
            case PORT -> "block.atomcraft.quantum_port";
            case CONTROLLER -> "block.atomcraft.quantum_replicator";
        };
    }
}
