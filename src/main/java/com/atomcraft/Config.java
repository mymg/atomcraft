package com.atomcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configurações do mod (config/atomcraft-common.toml). */
public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("replicator");
    }

    public static final ModConfigSpec.DoubleValue FE_PER_MASS = BUILDER
            .comment("FE gasto por unidade de massa atômica ao replicar (multiplicado pela raridade)")
            .defineInRange("fePerAtomicMass", 2.0, 0.0, 1_000_000.0);

    public static final ModConfigSpec.DoubleValue REPLICATION_TIME = BUILDER
            .comment("Multiplicador do tempo de replicação")
            .defineInRange("timeMultiplier", 1.0, 0.01, 100.0);

    public static final ModConfigSpec.IntValue REPLICATOR_CAPACITY = BUILDER
            .comment("Capacidade de energia do Replicador Atômico (FE)")
            .defineInRange("energyCapacity", 2_000_000, 1_000, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue REPLICATOR_MAX_INPUT = BUILDER
            .comment("Máximo de FE recebido por tick")
            .defineInRange("maxEnergyInput", 50_000, 1, Integer.MAX_VALUE);

    static {
        BUILDER.pop().push("atomizer");
    }

    public static final ModConfigSpec.DoubleValue ATOMIZE_FE_PER_MASS = BUILDER
            .comment("FE gasto por unidade de massa atômica ao atomizar (0 desliga o consumo de energia)")
            .defineInRange("fePerAtomicMass", 0.25, 0.0, 1_000_000.0);

    public static final ModConfigSpec.IntValue ATOMIZER_CAPACITY = BUILDER
            .comment("Capacidade de energia do Atomizador (FE)")
            .defineInRange("energyCapacity", 200_000, 1_000, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ATOMIZER_MAX_INPUT = BUILDER
            .comment("Máximo de FE recebido por tick pelo Atomizador")
            .defineInRange("maxEnergyInput", 10_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue ATOMIZE_TIME = BUILDER
            .comment("Multiplicador do tempo de atomização")
            .defineInRange("timeMultiplier", 1.0, 0.01, 100.0);

    static {
        BUILDER.pop().push("analyzer");
    }

    public static final ModConfigSpec.IntValue ANALYZE_TICKS = BUILDER
            .comment("Ticks para analisar cada unidade de item (20 ticks = 1 segundo)")
            .defineInRange("ticksPerItem", 10, 1, 1200);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
