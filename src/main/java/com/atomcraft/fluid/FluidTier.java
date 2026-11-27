package com.atomcraft.fluid;

/**
 * Níveis dos tanques e canos de fluido (valores parecidos com os do Mekanism).
 * Tanque: capacidade em mB. Cano: vazão em mB por tick.
 */
public enum FluidTier {
    BASIC("basic", 32_000, 250, 0xFF9AA4AE),
    ADVANCED("advanced", 64_000, 1_000, 0xFFE0A23A),
    ELITE("elite", 128_000, 4_000, 0xFF46C8DC),
    ULTIMATE("ultimate", 256_000, 16_000, 0xFFB45AE6);

    private final String id;
    private final int tankCapacity;
    private final int pipeRate;
    private final int color;

    FluidTier(String id, int tankCapacity, int pipeRate, int color) {
        this.id = id;
        this.tankCapacity = tankCapacity;
        this.pipeRate = pipeRate;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public int tankCapacity() {
        return tankCapacity;
    }

    public int pipeRate() {
        return pipeRate;
    }

    public int color() {
        return color;
    }
}
