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
        BUILDER.pop().push("generators");
    }

    public static final ModConfigSpec.IntValue COMBUSTION_FE_PER_UNIT = BUILDER
            .comment("Gerador de Combustão: FE por unidade de combustível (C + H/4 − O). Carvão (C24) = 24 unidades")
            .defineInRange("combustionFePerUnit", 1600, 1, 1_000_000);

    public static final ModConfigSpec.IntValue COMBUSTION_RATE = BUILDER
            .comment("Gerador de Combustão: FE gerado por tick")
            .defineInRange("combustionRate", 60, 1, 1_000_000);

    public static final ModConfigSpec.IntValue CARBON_FE_PER_ATOM = BUILDER
            .comment("Gerador de Carbono: FE por átomo de carbono queimado da rede")
            .defineInRange("carbonFePerAtom", 2000, 1, 1_000_000);

    public static final ModConfigSpec.IntValue CARBON_RATE = BUILDER
            .comment("Gerador de Carbono: FE gerado por tick")
            .defineInRange("carbonRate", 200, 1, 1_000_000);

    public static final ModConfigSpec.IntValue FUSION_FE_PER_CYCLE = BUILDER
            .comment("Reator de Fusão: FE por ciclo (4 H → 1 He) na temperatura máxima. Um ciclo dura 10 ticks")
            .defineInRange("fusionFePerCycle", 20_000, 1, 100_000_000);

    public static final ModConfigSpec.IntValue FUSION_WARMUP_CYCLES = BUILDER
            .comment("Reator de Fusão: ciclos para ir de frio à temperatura máxima")
            .defineInRange("fusionWarmupCycles", 100, 1, 10_000);

    public static final ModConfigSpec.IntValue GENERATOR_CAPACITY = BUILDER
            .comment("Capacidade de energia dos geradores (FE)")
            .defineInRange("generatorCapacity", 500_000, 1_000, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue GENERATOR_MAX_OUTPUT = BUILDER
            .comment("Máximo de FE que cada gerador envia por tick")
            .defineInRange("generatorMaxOutput", 20_000, 1, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
