package com.atomcraft.fusion;

/**
 * Módulos do Reator de Fusão. Só o módulo de nível mais alto de cada tipo vale.
 * Os valores são indexados pelo nível (0 = sem módulo, 1..3 = I..III).
 */
public enum ModuleType {
    /** Confinamento Magnético: multiplica a energia de cada fusão. */
    CONTAINMENT,
    /** Injetor de Combustível: encurta o ciclo (mais fusões por segundo, mais H gasto). */
    INJECTOR,
    /** Reciclador de Hidrogênio: chance de devolver 1 H por fusão. */
    RECYCLER,
    /** Ignição a Plasma: o plasma aquece mais rápido. */
    IGNITION;

    private static final double[] CONTAINMENT_BONUS = {0, 1.0, 2.0, 4.0};
    private static final int[] CYCLE_TICKS = {10, 8, 6, 4};
    private static final double[] RECYCLE_CHANCE = {0, 0.25, 0.50, 0.75};
    /** Fração do tempo de aquecimento padrão (5 min): 2 min, 1 min, 30 s. */
    private static final double[] WARMUP_FACTOR = {1, 0.4, 0.2, 0.1};

    public static final int MAX_TIER = 3;

    public String id() {
        return name().toLowerCase();
    }

    public String translationKey() {
        return "module.atomcraft." + id();
    }

    public static double energyMultiplier(int tier) {
        return 1 + CONTAINMENT_BONUS[tier];
    }

    public static int cycleTicks(int tier) {
        return CYCLE_TICKS[tier];
    }

    public static double recycleChance(int tier) {
        return RECYCLE_CHANCE[tier];
    }

    public static double warmupFactor(int tier) {
        return WARMUP_FACTOR[tier];
    }

    /** Texto curto do efeito no nível (para tooltips e a interface). */
    public String effect(int tier) {
        return switch (this) {
            case CONTAINMENT -> "+" + Math.round(CONTAINMENT_BONUS[tier] * 100) + "%";
            case INJECTOR -> CYCLE_TICKS[tier] + "t";
            case RECYCLER -> Math.round(RECYCLE_CHANCE[tier] * 100) + "%";
            case IGNITION -> {
                int seconds = (int) Math.round(300 * WARMUP_FACTOR[tier]);
                yield seconds >= 60 ? (seconds / 60) + "min" : seconds + "s";
            }
        };
    }

    public static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "-";
        };
    }
}
