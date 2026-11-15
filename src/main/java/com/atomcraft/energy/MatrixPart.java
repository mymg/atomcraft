package com.atomcraft.energy;

/** Peças da Matriz de Energia (a cor marca a peça na prévia da estrutura). */
public enum MatrixPart {
    CASING(0xFF8A96A8),
    GLASS(0xFF7FE0FF),
    CORE(0xFFFFD34A),
    PORT(0xFF3A8FE0),
    CONTROLLER(0xFFFFFFFF);

    private final int color;

    MatrixPart(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    public String translationKey() {
        return switch (this) {
            case CASING -> "block.atomcraft.matrix_casing";
            case GLASS -> "block.atomcraft.matrix_glass";
            case CORE -> "block.atomcraft.matrix_core";
            case PORT -> "block.atomcraft.matrix_port";
            case CONTROLLER -> "block.atomcraft.energy_matrix";
        };
    }
}
