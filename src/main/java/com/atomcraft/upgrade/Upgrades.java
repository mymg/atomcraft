package com.atomcraft.upgrade;

/**
 * Efeito dos chips. Cada máquina aceita até {@link #MAX_PER_TYPE} chips de cada tipo.
 * <ul>
 *   <li>Velocidade: tempo ÷ (1 + n); cada chip aumenta o custo de energia em 25%.</li>
 *   <li>Eficiência: custo de energia × 0,8ⁿ.</li>
 *   <li>Geradores: velocidade multiplica a geração por (1 + n/2); eficiência rende +25% de FE por combustível por chip.</li>
 * </ul>
 */
public final class Upgrades {

    public static final int SLOTS = 2;
    public static final int MAX_PER_SLOT = 4;
    public static final int MAX_PER_TYPE = 8;

    private Upgrades() {}

    /** Duração de uma operação com {@code speed} chips de velocidade. */
    public static int ticks(int baseTicks, int speed) {
        return Math.max(1, (int) Math.ceil(baseTicks / (1.0 + speed)));
    }

    /** Custo de energia de uma operação. */
    public static long energy(long baseEnergy, int speed, int efficiency) {
        return Math.max(0, Math.round(baseEnergy * (1 + 0.25 * speed) * Math.pow(0.8, efficiency)));
    }

    /** Multiplicador de geração por tick dos geradores. */
    public static double generatorRate(int speed) {
        return 1 + 0.5 * speed;
    }

    /** Multiplicador de FE por unidade de combustível dos geradores. */
    public static double generatorYield(int efficiency) {
        return 1 + 0.25 * efficiency;
    }
}
