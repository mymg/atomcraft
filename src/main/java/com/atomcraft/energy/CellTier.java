package com.atomcraft.energy;

import com.atomcraft.Config;

/** Níveis de armazenamento de energia: três células (blocos) e a Matriz de Energia (multibloco 3×5×3). */
public enum CellTier {
    BASIC, ADVANCED, ELITE, MATRIX;

    private static final CellTier[] VALUES = values();

    public int capacity() {
        return switch (this) {
            case BASIC -> Config.CELL_BASIC_CAPACITY.get();
            case ADVANCED -> Config.CELL_ADVANCED_CAPACITY.get();
            case ELITE -> Config.CELL_ELITE_CAPACITY.get();
            case MATRIX -> Config.MATRIX_CORE_CAPACITY.get() * MatrixStructure.CORES;
        };
    }

    /** FE por tick (entrada e saída de cada lado ou porta). */
    public int rate() {
        return switch (this) {
            case BASIC -> Config.CELL_BASIC_RATE.get();
            case ADVANCED -> Config.CELL_ADVANCED_RATE.get();
            case ELITE -> Config.CELL_ELITE_RATE.get();
            case MATRIX -> Config.MATRIX_RATE.get();
        };
    }

    public static CellTier byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : BASIC;
    }
}
