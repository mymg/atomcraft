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

    // Complexidade: quantos elementos diferentes um item pode ter para cada nível de replicador (-1 = sem limite)
    public static final ModConfigSpec.IntValue REPLICATOR_MAX_ELEMENTS = BUILDER
            .comment("Replicador Atômico: máximo de elementos diferentes no item (-1 = sem limite)")
            .defineInRange("maxElements", 4, -1, 128);

    public static final ModConfigSpec.BooleanValue REPLICATOR_EXOTICS = BUILDER
            .comment("Replicador Atômico: pode replicar itens com elementos exóticos (Rs, Lm, En...)")
            .define("allowExotics", false);

    static {
        BUILDER.pop().push("advancedReplicator");
    }

    public static final ModConfigSpec.IntValue ADVANCED_MAX_ELEMENTS = BUILDER
            .comment("Replicador Avançado: máximo de elementos diferentes no item (-1 = sem limite)")
            .defineInRange("maxElements", 6, -1, 128);

    public static final ModConfigSpec.BooleanValue ADVANCED_EXOTICS = BUILDER
            .comment("Replicador Avançado: pode replicar itens com elementos exóticos")
            .define("allowExotics", true);

    static {
        BUILDER.pop().push("quantumReplicator");
    }

    public static final ModConfigSpec.IntValue QUANTUM_MAX_ELEMENTS = BUILDER
            .comment("Replicador Quântico: máximo de elementos diferentes no item (-1 = sem limite)")
            .defineInRange("maxElements", -1, -1, 128);

    public static final ModConfigSpec.BooleanValue QUANTUM_EXOTICS = BUILDER
            .comment("Replicador Quântico: pode replicar itens com elementos exóticos")
            .define("allowExotics", true);

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

    public static final ModConfigSpec.IntValue FUSION_WARMUP_TICKS = BUILDER
            .comment("Reator de Fusão: ticks para o plasma ir de frio à temperatura máxima (padrão: 5 minutos)")
            .defineInRange("fusionWarmupTicks", 6000, 20, 1_000_000);

    public static final ModConfigSpec.IntValue FUSION_CAPACITY = BUILDER
            .comment("Reator de Fusão: capacidade de energia (FE)")
            .defineInRange("fusionCapacity", 10_000_000, 1_000, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue FUSION_MAX_OUTPUT = BUILDER
            .comment("Reator de Fusão: máximo de FE enviado por tick")
            .defineInRange("fusionMaxOutput", 200_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue GENERATOR_CAPACITY = BUILDER
            .comment("Capacidade de energia dos geradores (FE)")
            .defineInRange("generatorCapacity", 500_000, 1_000, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue GENERATOR_MAX_OUTPUT = BUILDER
            .comment("Máximo de FE que cada gerador envia por tick")
            .defineInRange("generatorMaxOutput", 20_000, 1, Integer.MAX_VALUE);

    static {
        BUILDER.pop().push("energyCells");
    }

    public static final ModConfigSpec.IntValue CELL_BASIC_CAPACITY = BUILDER
            .comment("Célula de Energia Básica: capacidade (FE)").defineInRange("basicCapacity", 1_000_000, 1_000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue CELL_BASIC_RATE = BUILDER
            .comment("Célula de Energia Básica: FE por tick (entrada e saída)").defineInRange("basicRate", 2_000, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue CELL_ADVANCED_CAPACITY = BUILDER
            .comment("Célula de Energia Avançada: capacidade (FE)").defineInRange("advancedCapacity", 8_000_000, 1_000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue CELL_ADVANCED_RATE = BUILDER
            .comment("Célula de Energia Avançada: FE por tick").defineInRange("advancedRate", 8_000, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue CELL_ELITE_CAPACITY = BUILDER
            .comment("Célula de Energia Elite: capacidade (FE)").defineInRange("eliteCapacity", 64_000_000, 1_000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue CELL_ELITE_RATE = BUILDER
            .comment("Célula de Energia Elite: FE por tick").defineInRange("eliteRate", 32_000, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue MATRIX_CORE_CAPACITY = BUILDER
            .comment("Matriz de Energia: capacidade de cada um dos 3 núcleos (FE)").defineInRange("matrixCoreCapacity", 160_000_000, 1_000, 700_000_000);
    public static final ModConfigSpec.IntValue MATRIX_RATE = BUILDER
            .comment("Matriz de Energia: FE por tick em cada porta (e pela rede)").defineInRange("matrixRate", 256_000, 1, Integer.MAX_VALUE);

    static {
        BUILDER.pop().push("wireless");
    }

    public static final ModConfigSpec.IntValue CARD_BASIC_RANGE = BUILDER
            .comment("Alcance do Cartão de Rede (básico) em blocos, do transmissor até o jogador")
            .defineInRange("basicCardRange", 64, 1, 100_000);
    public static final ModConfigSpec.IntValue CARD_ADVANCED_RANGE = BUILDER
            .comment("Alcance do Cartão de Rede Avançado em blocos (o Infinito não tem limite na mesma dimensão; o Dimensional funciona em qualquer dimensão)")
            .defineInRange("advancedCardRange", 512, 1, 1_000_000);
    public static final ModConfigSpec.IntValue TRANSMITTER_CAPACITY = BUILDER
            .comment("Transmissor Sem Fio: energia guardada para os equipamentos (FE)")
            .defineInRange("transmitterCapacity", 1_000_000, 1_000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue DISINTEGRATOR_ENERGY = BUILDER
            .comment("Desintegrador Atômico: FE por bloco quebrado")
            .defineInRange("disintegratorEnergyPerBlock", 200, 0, 1_000_000);

    static {
        BUILDER.pop().push("equipment");
    }

    public static final ModConfigSpec.IntValue EXO_CHARGE = BUILDER
            .comment("Exoesqueleto: FE por segundo que cada peça recebe do Transmissor Sem Fio")
            .defineInRange("exoChargePerSecond", 20_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SHIELD_ENERGY = BUILDER
            .comment("Escudo de energia do Exoesqueleto: FE gastos por ponto de dano absorvido")
            .defineInRange("shieldEnergyPerDamage", 1_000, 1, 1_000_000);
    public static final ModConfigSpec.IntValue JETPACK_TANK = BUILDER
            .comment("Jetpack: capacidade do tanque (átomos de hidrogênio)")
            .defineInRange("jetpackTank", 16_000, 100, 10_000_000);
    public static final ModConfigSpec.IntValue JETPACK_USE = BUILDER
            .comment("Jetpack: hidrogênio por tick subindo (×(1 + nível do Propulsor)); flutuando gasta este valor")
            .defineInRange("jetpackUsePerTick", 1, 0, 1_000);
    public static final ModConfigSpec.IntValue JETPACK_REFUEL = BUILDER
            .comment("Jetpack: hidrogênio por segundo tirado dos Drives pelo Transmissor Sem Fio")
            .defineInRange("jetpackRefuelPerSecond", 400, 0, 1_000_000);
    public static final ModConfigSpec.IntValue BENCH_CAPACITY = BUILDER
            .comment("Bancada de Módulos: energia guardada (FE)")
            .defineInRange("benchCapacity", 500_000, 1_000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue BENCH_CHARGE = BUILDER
            .comment("Bancada de Módulos: FE por tick para recarregar o equipamento e a armadura vestida")
            .defineInRange("benchChargePerTick", 10_000, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue MAGNET_RANGE = BUILDER
            .comment("Anel Magnético: raio em blocos")
            .defineInRange("magnetRange", 8, 1, 32);

    static {
        BUILDER.pop().push("chunkLoader");
    }

    public static final ModConfigSpec.BooleanValue CHUNK_LOADER_ENABLED = BUILDER
            .comment("Carregador de Chunks ativo (desligue em modpacks com outro sistema, como FTB Chunks)")
            .define("enabled", true);
    public static final ModConfigSpec.IntValue CHUNK_LOADER_RADIUS = BUILDER
            .comment("Raio da grade em chunks em volta do carregador (0 = só a chunk dele; 2 = grade 5×5)")
            .defineInRange("radius", 2, 0, 2);
    public static final ModConfigSpec.IntValue CHUNK_LOADER_MAX_CHUNKS = BUILDER
            .comment("Máximo de chunks carregadas por carregador")
            .defineInRange("maxChunks", 9, 1, 25);
    public static final ModConfigSpec.IntValue CHUNK_LOADER_ENERGY = BUILDER
            .comment("FE por tick para cada chunk carregada (0 = de graça)")
            .defineInRange("energyPerChunk", 25, 0, 1_000_000);
    public static final ModConfigSpec.IntValue CHUNK_LOADER_CAPACITY = BUILDER
            .comment("Energia guardada no carregador (FE)")
            .defineInRange("capacity", 500_000, 1_000, Integer.MAX_VALUE);

    static {
        BUILDER.pop().push("pump");
    }

    public static final ModConfigSpec.IntValue PUMP_ENERGY = BUILDER
            .comment("Bomba: FE gastos por balde (1.000 mB) bombeado")
            .defineInRange("energyPerBucket", 200, 0, 1_000_000);
    public static final ModConfigSpec.IntValue PUMP_TICKS = BUILDER
            .comment("Bomba: ticks entre um balde e outro")
            .defineInRange("ticksPerBucket", 10, 1, 1_200);
    public static final ModConfigSpec.IntValue PUMP_RANGE = BUILDER
            .comment("Bomba: distância máxima (em blocos) das fontes que ela alcança")
            .defineInRange("range", 32, 1, 128);
    public static final ModConfigSpec.BooleanValue PUMP_INFINITE_WATER = BUILDER
            .comment("Bomba: não remove fontes de água (a água é infinita, como num poço)")
            .define("infiniteWater", true);
    public static final ModConfigSpec.IntValue PUMP_CAPACITY = BUILDER
            .comment("Energia guardada na bomba (FE)")
            .defineInRange("capacity", 100_000, 1_000, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
