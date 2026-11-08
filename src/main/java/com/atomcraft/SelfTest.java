package com.atomcraft;

import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.atomcraft.block.entity.AtomizerBlockEntity;
import com.atomcraft.block.entity.CarbonGeneratorBlockEntity;
import com.atomcraft.block.entity.CombustionGeneratorBlockEntity;
import com.atomcraft.block.entity.FusionReactorBlockEntity;
import com.atomcraft.fusion.FusionStructure;
import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.block.entity.SynthesizerBlockEntity;
import com.atomcraft.block.entity.TerminalBlockEntity;
import com.atomcraft.upgrade.UpgradeType;
import com.atomcraft.block.entity.DriveBlockEntity;
import com.atomcraft.block.entity.ReplicatorBlockEntity;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.chemistry.Fuel;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModItems;
import com.atomcraft.research.ResearchData;
import com.atomcraft.storage.ElementNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Autoteste do ciclo completo (atomizar → armazenar → pesquisar → replicar).
 * Só roda quando a variável de ambiente ATOMCRAFT_SELFTEST está definida (usado no CI).
 */
final class SelfTest {

    private static final UUID TESTER = UUID.fromString("00000000-0000-0000-0000-00000000a70c");

    private SelfTest() {}

    static void runIfRequested(MinecraftServer server) {
        if (System.getenv("ATOMCRAFT_SELFTEST") == null) return;
        List<String> failures = new ArrayList<>();
        try {
            run(server, failures);
        } catch (Throwable t) {
            AtomCraft.LOGGER.error("AtomCraft SELFTEST exceção", t);
            failures.add("exceção: " + t);
        }
        if (failures.isEmpty()) AtomCraft.LOGGER.info("AtomCraft SELFTEST PASS");
        else AtomCraft.LOGGER.warn("AtomCraft SELFTEST FAIL: {}", String.join(" | ", failures));
    }

    private static void check(List<String> failures, boolean condition, String message) {
        if (!condition) failures.add(message);
        AtomCraft.LOGGER.info("AtomCraft SELFTEST {} {}", condition ? "ok  " : "FAIL", message);
    }

    private static long amount(Item item, ChemicalElement element) {
        AtomicValue value = AtomicValues.get(item, false);
        return value == null ? -1 : value.composition().get(element);
    }

    private static void run(MinecraftServer server, List<String> failures) {
        // 1. Valores base e derivados das receitas
        check(failures, amount(Items.IRON_INGOT, ChemicalElement.IRON) == 9, "barra de ferro = Fe9 (" + amount(Items.IRON_INGOT, ChemicalElement.IRON) + ")");
        check(failures, amount(Items.IRON_BLOCK, ChemicalElement.IRON) == 81, "bloco de ferro = Fe81 (" + amount(Items.IRON_BLOCK, ChemicalElement.IRON) + ")");
        check(failures, amount(Items.IRON_NUGGET, ChemicalElement.IRON) == 1, "pepita de ferro = Fe1 (" + amount(Items.IRON_NUGGET, ChemicalElement.IRON) + ")");
        check(failures, amount(Items.BUCKET, ChemicalElement.IRON) == 27, "balde = Fe27 (" + amount(Items.BUCKET, ChemicalElement.IRON) + ")");
        check(failures, amount(Items.DIAMOND_BLOCK, ChemicalElement.CARBON) == 512 * 9, "bloco de diamante = C4608");
        check(failures, AtomicValues.get(Items.OAK_PLANKS, false) != null, "tábuas de carvalho têm valor (derivado)");
        check(failures, AtomicValues.get(Items.GLASS, false) != null, "vidro tem valor (fundição)");
        check(failures, AtomicValues.get(Items.IRON_PICKAXE, false) != null, "picareta de ferro tem valor");
        AtomicValue cake = AtomicValues.get(Items.CAKE, false);
        check(failures, cake == null || cake.composition().get(ChemicalElement.IRON) == 0, "bolo não inclui o ferro dos baldes devolvidos");
        AtomCraft.LOGGER.info("AtomCraft SELFTEST {} itens com valor", AtomicValues.serverValues().size());
        // Compatibilidade: composições por tags comuns "c:" (All The Ores, Mekanism, Thermal...)
        var compat = com.atomcraft.chemistry.CompositionManager.entries().stream()
                .filter(e -> e.tag().isPresent() && e.tag().get().toString().equals("c:ingots/tin")).findFirst();
        check(failures, compat.isPresent() && compat.get().elements().get(ChemicalElement.TIN) == 9, "tag c:ingots/tin = Sn9");
        var uranium = com.atomcraft.chemistry.CompositionManager.entries().stream()
                .filter(e -> e.tag().isPresent() && e.tag().get().toString().equals("c:storage_blocks/uranium")).findFirst();
        check(failures, uranium.isPresent() && uranium.get().elements().get(ChemicalElement.URANIUM) == 81, "tag c:storage_blocks/uranium = U81");
        check(failures, amount(Items.IRON_INGOT, ChemicalElement.IRON) == 9, "tags comuns não mudam a barra de ferro");
        for (String recipe : List.of("atomizer", "analyzer", "replicator", "drive", "atomic_cable",
                "element_disk_16k", "element_disk_64k", "element_disk_256k", "element_disk_1m",
                "terminal", "combustion_generator", "carbon_generator", "fusion_reactor", "synthesizer",
                "reactor_casing", "reactor_glass", "magnetic_coil", "central_solenoid", "reactor_port")) {
            boolean exists = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, AtomCraft.id(recipe))).isPresent();
            check(failures, exists, "receita " + recipe + " carregada");
        }

        // 2. Monta a rede: Drive no meio, máquinas encostadas
        ServerLevel level = server.overworld();
        BlockPos drivePos = new BlockPos(0, 120, 0);
        level.getChunk(drivePos);
        place(level, drivePos, ModBlocks.DRIVE.get());
        place(level, drivePos.east(), ModBlocks.ATOMIZER.get());
        place(level, drivePos.west(), ModBlocks.REPLICATOR.get());
        place(level, drivePos.north(), ModBlocks.CABLE.get());
        place(level, drivePos.north(2), ModBlocks.ANALYZER.get());

        DriveBlockEntity drive = (DriveBlockEntity) level.getBlockEntity(drivePos);
        AtomizerBlockEntity atomizer = (AtomizerBlockEntity) level.getBlockEntity(drivePos.east());
        ReplicatorBlockEntity replicator = (ReplicatorBlockEntity) level.getBlockEntity(drivePos.west());
        AnalyzerBlockEntity analyzer = (AnalyzerBlockEntity) level.getBlockEntity(drivePos.north(2));
        drive.disks().set(0, ItemResource.of(ModItems.DISK_16K.get()), 1);

        // 3. Atomizar 4 barras de ferro
        atomizer.setOwner(TESTER, "tester");
        atomizer.buffer().set(0, ItemResource.of(Items.IRON_INGOT), 1);
        for (int i = 0; i < 100; i++) atomizer.serverTick();
        check(failures, atomizer.buffer().getAmountAsInt(0) == 1 || Config.ATOMIZE_FE_PER_MASS.get() == 0,
                "sem energia, o atomizador não consome itens");
        atomizer.energy().set(100_000);
        atomizer.buffer().set(0, ItemResource.of(Items.IRON_INGOT), 4);
        for (int i = 0; i < 400; i++) atomizer.serverTick();
        ElementNetwork network = ElementNetwork.scan(level, drivePos);
        check(failures, atomizer.buffer().getAmountAsInt(0) == 0, "atomizador consumiu as 4 barras");
        check(failures, network.totals().get(ChemicalElement.IRON) == 36, "rede tem Fe36 (" + network.totals().get(ChemicalElement.IRON) + ")");

        // 4. Pesquisar barra de ferro pelo Analisador (ligado por cabo)
        analyzer.setOwner(TESTER, "tester");
        check(failures, !ResearchData.get(server).isDiscovered(TESTER, ChemicalElement.IRON), "ferro começa desconhecido");
        int required = ResearchData.required(Items.IRON_INGOT);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int accepted = analyzer.automation().insert(ItemResource.of(Items.IRON_INGOT), 99, transaction);
            check(failures, accepted == required, "analisador aceita só o que falta (" + accepted + "/" + required + ")");
        }
        analyzer.buffer().set(0, ItemResource.of(Items.IRON_INGOT), Math.min(99, required));
        for (int i = 0; i < required * (Config.ANALYZE_TICKS.get() + 1) + 20; i++) analyzer.serverTick();
        ResearchData research = ResearchData.get(server);
        check(failures, research.isDiscovered(TESTER, ChemicalElement.IRON) && !research.isDiscovered(TESTER, ChemicalElement.CARBON),
                "analisar a barra descobriu o ferro (e só ele)");
        check(failures, research.isResearched(TESTER, Items.IRON_INGOT),
                "barra de ferro pesquisada (" + research.getProgress(TESTER, Items.IRON_INGOT) + "/" + required + ")");

        // 5. Sem energia, o Replicador não produz
        replicator.startJob(TESTER, Items.IRON_INGOT, 2);
        for (int i = 0; i < 50; i++) replicator.serverTick();
        check(failures, replicator.output().getAmountAsInt(0) == 0, "sem energia, nada é replicado");

        // 6. Com energia, replica 2 barras e consome Fe18 da rede
        replicator.energy().set(1_000_000);
        for (int i = 0; i < 600; i++) replicator.serverTick();
        network = ElementNetwork.scan(level, drivePos);
        check(failures, replicator.output().getAmountAsInt(0) == 2, "replicou 2 barras (" + replicator.output().getAmountAsInt(0) + ")");
        check(failures, network.totals().get(ChemicalElement.IRON) == 18, "rede ficou com Fe18 (" + network.totals().get(ChemicalElement.IRON) + ")");
        check(failures, replicator.energy().getAmountAsLong() < 1_000_000, "energia foi consumida");

        // 7. Sem elementos suficientes: pedido fica parado
        replicator.startJob(TESTER, Items.IRON_BLOCK, 1);
        research.unlock(TESTER, Items.IRON_BLOCK);
        for (int i = 0; i < 100; i++) replicator.serverTick();
        check(failures, replicator.output().getAmountAsInt(1) == 0, "bloco de ferro (Fe81) não replicado com Fe18");
        replicator.cancelJob();

        // Limpeza
        for (BlockPos pos : List.of(drivePos, drivePos.east(), drivePos.west(), drivePos.north(), drivePos.north(2))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        generators(level, failures);
        controlsAndSynthesis(server, level, failures);
    }

    /** Botão de ligar/desligar, chips de upgrade, Sintetizador e loot das schematics. */
    private static void controlsAndSynthesis(MinecraftServer server, ServerLevel level, List<String> failures) {
        BlockPos drivePos = new BlockPos(0, 120, 36);
        level.getChunk(drivePos);
        place(level, drivePos, ModBlocks.DRIVE.get());
        place(level, drivePos.east(), ModBlocks.ATOMIZER.get());
        place(level, drivePos.west(), ModBlocks.SYNTHESIZER.get());
        place(level, drivePos.north(), ModBlocks.ANALYZER.get());
        DriveBlockEntity drive = (DriveBlockEntity) level.getBlockEntity(drivePos);
        AtomizerBlockEntity atomizer = (AtomizerBlockEntity) level.getBlockEntity(drivePos.east());
        SynthesizerBlockEntity synthesizer = (SynthesizerBlockEntity) level.getBlockEntity(drivePos.west());
        AnalyzerBlockEntity analyzer = (AnalyzerBlockEntity) level.getBlockEntity(drivePos.north());
        drive.disks().set(0, ItemResource.of(ModItems.DISK_64K.get()), 1);

        // Desligado: não trabalha
        atomizer.energy().set(100_000);
        atomizer.buffer().set(0, ItemResource.of(Items.IRON_INGOT), 1);
        atomizer.setEnabled(false);
        for (int i = 0; i < 200; i++) atomizer.tick();
        check(failures, atomizer.buffer().getAmountAsInt(0) == 1 && atomizer.status() == MachineStatus.DISABLED,
                "atomizador desligado não consome itens");
        atomizer.setEnabled(true);

        // Chips: o atomizador com 4 chips de velocidade termina bem antes
        int normal = ticksToAtomize(atomizer);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            check(failures, atomizer.upgrades().insert(ItemResource.of(ModItems.SPEED_UPGRADE.get()), 4, transaction) == 4,
                    "atomizador aceita chips de velocidade");
            check(failures, analyzer.upgrades().insert(ItemResource.of(ModItems.EFFICIENCY_UPGRADE.get()), 1, transaction) == 0,
                    "analisador recusa chip de eficiência");
            transaction.commit();
        }
        int fast = ticksToAtomize(atomizer);
        check(failures, atomizer.upgradeCount(UpgradeType.SPEED) == 4 && fast < normal,
                "chips de velocidade aceleram o atomizador (" + normal + " → " + fast + " ticks)");

        // Sintetizador: sem schematic não fabrica; com ela, fabrica um chip com elementos da rede
        ElementNetwork network = ElementNetwork.scan(level, drivePos);
        network.insert(new Composition(java.util.Map.of(ChemicalElement.SILICON, 100L, ChemicalElement.GOLD, 20L,
                ChemicalElement.REDSTONIUM, 50L, ChemicalElement.COPPER, 20L)), 1);
        synthesizer.energy().set(100_000);
        synthesizer.select(ModItems.SPEED_UPGRADE.get());
        check(failures, synthesizer.selected() == null, "sem schematic, o sintetizador não aceita a receita");
        synthesizer.library().set(0, ItemResource.of(ModItems.SPEED_UPGRADE_SCHEMATIC.get()), 1);
        synthesizer.select(ModItems.SPEED_UPGRADE.get());
        for (int i = 0; i < 260; i++) synthesizer.tick();
        network = ElementNetwork.scan(level, drivePos);
        check(failures, synthesizer.output().getAmountAsInt(0) >= 1,
                "sintetizador fabricou chip de velocidade (" + synthesizer.output().getAmountAsInt(0) + ")");
        check(failures, network.totals().get(ChemicalElement.SILICON) <= 68, "sintetizador consumiu Si da rede ("
                + network.totals().get(ChemicalElement.SILICON) + ")");
        // Chips instalados no meio da produção já aceleram o item atual
        network.insert(new Composition(java.util.Map.of(ChemicalElement.GOLD, 20L)), 1);
        synthesizer.energy().set(1_000_000);
        int before = synthesizer.output().getAmountAsInt(0);
        synthesizer.upgrades().set(0, ItemResource.of(ModItems.SPEED_UPGRADE.get()), 4);
        synthesizer.upgrades().set(1, ItemResource.of(ModItems.SPEED_UPGRADE.get()), 4);
        int extra = 0;
        while (synthesizer.output().getAmountAsInt(0) == before && extra < 300) {
            synthesizer.tick();
            extra++;
        }
        check(failures, extra <= 40, "chips no meio da produção aceleram o item atual (" + extra + " ticks)");
        synthesizer.library().set(0, ItemResource.EMPTY, 0);
        for (int i = 0; i < 5; i++) synthesizer.tick();
        check(failures, synthesizer.selected() == null, "retirar a schematic cancela a receita");

        // Loot: tabelas das schematics carregadas
        for (String table : List.of("village", "fortress", "bastion", "end", "ancient_city")) {
            var key = ResourceKey.create(Registries.LOOT_TABLE, AtomCraft.id("chests/schematics_" + table));
            check(failures, server.reloadableRegistries().getLootTable(key) != net.minecraft.world.level.storage.loot.LootTable.EMPTY,
                    "tabela de loot schematics_" + table);
        }

        for (BlockPos pos : List.of(drivePos, drivePos.east(), drivePos.west(), drivePos.north())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static int ticksToAtomize(AtomizerBlockEntity atomizer) {
        atomizer.energy().set(100_000);
        atomizer.buffer().set(0, ItemResource.of(Items.IRON_INGOT), 1);
        for (int i = 1; i <= 400; i++) {
            atomizer.tick();
            if (atomizer.buffer().getAmountAsInt(0) == 0) return i;
        }
        return 400;
    }

    /** Geradores: combustão com carvão, carbono da rede alimentando o Atomizador e fusão H → He. */
    private static void generators(ServerLevel level, List<String> failures) {
        long coal = Fuel.energy(Items.COAL, false), log = Fuel.energy(Items.OAK_LOG, false);
        check(failures, coal > log && log > 0, "carvão rende mais que tronco (" + coal + " > " + log + ")");
        check(failures, Fuel.energy(Items.IRON_INGOT, false) == 0, "ferro não é combustível");

        // Gerador de Combustão sozinho
        BlockPos combustionPos = new BlockPos(0, 120, 12);
        level.getChunk(combustionPos);
        place(level, combustionPos, ModBlocks.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity combustion = (CombustionGeneratorBlockEntity) level.getBlockEntity(combustionPos);
        for (int i = 0; i < 20; i++) combustion.serverTick();
        check(failures, combustion.energy().getAmountAsInt() == 0, "sem combustível, nada é gerado");
        combustion.fuel().set(0, ItemResource.of(Items.COAL), 2);
        for (int i = 0; i < 100; i++) combustion.serverTick();
        check(failures, combustion.fuel().getAmountAsInt(0) == 1, "gerador queimou 1 carvão (" + combustion.fuel().getAmountAsInt(0) + ")");
        check(failures, combustion.energy().getAmountAsInt() == 100 * Config.COMBUSTION_RATE.get(),
                "combustão gerou " + combustion.energy().getAmountAsInt() + " FE");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            check(failures, combustion.automation().insert(ItemResource.of(Items.IRON_INGOT), 1, transaction) == 0,
                    "gerador recusa itens sem carbono");
        }

        // Rede: Drive + Gerador de Carbono + Atomizador + Reator de Fusão
        BlockPos drivePos = new BlockPos(0, 120, 24);
        level.getChunk(drivePos);
        place(level, drivePos, ModBlocks.DRIVE.get());
        place(level, drivePos.east(), ModBlocks.CARBON_GENERATOR.get());
        place(level, drivePos.west(), ModBlocks.ATOMIZER.get());
        place(level, drivePos.north(), ModBlocks.CABLE.get());
        place(level, drivePos.south(), ModBlocks.TERMINAL.get());
        DriveBlockEntity drive = (DriveBlockEntity) level.getBlockEntity(drivePos);
        CarbonGeneratorBlockEntity carbon = (CarbonGeneratorBlockEntity) level.getBlockEntity(drivePos.east());
        AtomizerBlockEntity atomizer = (AtomizerBlockEntity) level.getBlockEntity(drivePos.west());
        check(failures, level.getBlockEntity(drivePos.south()) instanceof TerminalBlockEntity, "terminal tem block entity");

        for (int i = 0; i < 20; i++) carbon.serverTick();
        check(failures, carbon.energy().getAmountAsInt() == 0, "sem disco, o gerador de carbono fica parado");

        drive.disks().set(0, ItemResource.of(ModItems.DISK_16K.get()), 1);
        ElementNetwork network = ElementNetwork.scan(level, drivePos);
        network.insert(new Composition(java.util.Map.of(ChemicalElement.CARBON, 100L, ChemicalElement.HYDROGEN, 400L)), 1);
        check(failures, network.receivers().contains(atomizer), "atomizador é receptor de energia da rede");

        for (int i = 0; i < 100; i++) carbon.serverTick();
        network = ElementNetwork.scan(level, drivePos);
        long carbonLeft = network.totals().get(ChemicalElement.CARBON);
        check(failures, carbonLeft < 100 && carbonLeft > 0, "gerador de carbono consumiu C da rede (C" + carbonLeft + ")");
        check(failures, atomizer.energy().getAmountAsInt() > 0, "atomizador recebeu energia pela rede (" + atomizer.energy().getAmountAsInt() + " FE)");

        for (BlockPos pos : List.of(combustionPos, drivePos, drivePos.east(), drivePos.west(), drivePos.north(), drivePos.south())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        fusionReactor(level, failures);
    }

    /** Monta a estrutura do Reator de Fusão. A peça "pulada" fica faltando (para testar a validação). */
    public static void buildReactor(net.minecraft.world.level.Level level, BlockPos controller, net.minecraft.core.Direction facing,
                                    java.util.Set<BlockPos> ports, @org.jspecify.annotations.Nullable BlockPos skip) {
        for (FusionStructure.Slot slot : FusionStructure.blueprint(controller, facing)) {
            if (slot.pos().equals(skip)) continue;
            Block block = switch (slot.part()) {
                case CASING -> ports.contains(slot.pos()) ? ModBlocks.REACTOR_PORT.get() : ModBlocks.REACTOR_CASING.get();
                case GLASS -> ModBlocks.REACTOR_GLASS.get();
                case COIL -> ModBlocks.MAGNETIC_COIL.get();
                case SOLENOID -> ModBlocks.CENTRAL_SOLENOID.get();
                case CONTROLLER -> null;
                default -> Blocks.AIR;
            };
            if (block != null) level.setBlock(slot.pos(), block.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (!controller.equals(skip) && !(level.getBlockState(controller).getBlock() instanceof com.atomcraft.block.FusionReactorBlock)) {
            level.setBlock(controller, ModBlocks.FUSION_REACTOR.get().defaultBlockState().setValue(com.atomcraft.block.MachineBlock.FACING, facing),
                    Block.UPDATE_ALL);
        }
    }

    /** Reator de Fusão multibloco: validação, fusão H → He, módulos e desmontagem. */
    private static void fusionReactor(ServerLevel level, List<String> failures) {
        net.minecraft.core.Direction facing = net.minecraft.core.Direction.NORTH;
        BlockPos controller = new BlockPos(40, 122, 0);
        level.getChunk(controller);
        level.getChunk(controller.south(8));
        BlockPos port = FusionStructure.toWorld(controller, facing, 1, 2, 0);
        BlockPos coil = FusionStructure.toWorld(controller, facing, 2, 2, 2);
        BlockPos drivePos = port.relative(facing);
        buildReactor(level, controller, facing, java.util.Set.of(port), coil);
        place(level, drivePos, ModBlocks.DRIVE.get());
        DriveBlockEntity drive = (DriveBlockEntity) level.getBlockEntity(drivePos);
        drive.disks().set(0, ItemResource.of(ModItems.DISK_64K.get()), 1);
        ElementNetwork.scan(level, drivePos).insert(new Composition(java.util.Map.of(ChemicalElement.HYDROGEN, 8000L)), 1);
        FusionReactorBlockEntity fusion = (FusionReactorBlockEntity) level.getBlockEntity(controller);
        fusion.setOwner(TESTER, "tester");

        // Falta uma bobina: não forma
        for (int i = 0; i < 20; i++) fusion.tick();
        FusionStructure.Result result = FusionStructure.check(level, controller, facing);
        check(failures, !fusion.isFormed() && fusion.status() == MachineStatus.INCOMPLETE && result.missing().size() == 1
                && result.missing().getFirst().part() == com.atomcraft.fusion.ReactorPart.COIL,
                "reator sem uma bobina não forma (faltam " + result.missing().size() + ")");

        // Com a bobina: forma, as peças somem e a porta repassa a energia
        place(level, coil, ModBlocks.MAGNETIC_COIL.get());
        for (int i = 0; i < 10; i++) fusion.tick();
        check(failures, fusion.isFormed() && level.getBlockState(coil).getValue(com.atomcraft.block.ReactorPartBlock.FORMED)
                        && level.getBlockState(controller).getValue(com.atomcraft.block.FusionReactorBlock.FORMED),
                "reator forma e marca as peças");
        check(failures, level.getBlockState(coil).getLightDampening() == 0
                        && level.getBlockState(controller).getLightDampening() == 0
                        && level.getBlockState(port).getLightDampening() > 0,
                "peças formadas deixam a luz passar (modelo não fica escuro); a porta continua sólida");
        check(failures, level.getBlockEntity(port) instanceof com.atomcraft.block.entity.ReactorPortBlockEntity entity
                && entity.energy() == fusion.energy(), "porta repassa a energia do reator");

        for (int i = 0; i < 400; i++) fusion.tick();
        ElementNetwork network = ElementNetwork.scan(level, drivePos);
        long hydrogen = network.totals().get(ChemicalElement.HYDROGEN), helium = network.totals().get(ChemicalElement.HELIUM);
        check(failures, hydrogen < 8000 && helium > 0 && (8000 - hydrogen) == helium * 4,
                "fusão pela porta consumiu H e produziu He (H" + hydrogen + " He" + helium + ")");
        int slowHeat = fusion.heat();
        check(failures, slowHeat > 0 && slowHeat < 200, "plasma aquece devagar sem módulo (" + slowHeat + "‰)");
        check(failures, fusion.energy().getAmountAsInt() > 0, "reator gerou energia (" + fusion.energy().getAmountAsInt() + " FE)");
        check(failures, ResearchData.get(level.getServer()).isDiscovered(TESTER, ChemicalElement.HELIUM),
                "o dono do reator descobriu o hélio");

        // Módulos: só o maior nível de cada tipo vale; chips não entram
        var modules = fusion.upgrades();
        modules.set(0, ItemResource.of(ModItems.MODULES.get(com.atomcraft.fusion.ModuleType.CONTAINMENT).get(2).get()), 1);
        modules.set(1, ItemResource.of(ModItems.MODULES.get(com.atomcraft.fusion.ModuleType.CONTAINMENT).get(0).get()), 1);
        modules.set(2, ItemResource.of(ModItems.MODULES.get(com.atomcraft.fusion.ModuleType.INJECTOR).get(2).get()), 1);
        modules.set(3, ItemResource.of(ModItems.MODULES.get(com.atomcraft.fusion.ModuleType.IGNITION).get(2).get()), 1);
        check(failures, fusion.moduleTier(com.atomcraft.fusion.ModuleType.CONTAINMENT) == 3
                && fusion.energyPerCycle() == Config.FUSION_FE_PER_CYCLE.get() * 5 && fusion.cycleTicks() == 4
                && fusion.warmupTicks() == Config.FUSION_WARMUP_TICKS.get() / 10, "módulos III: +400% FE, ciclo 4t, aquecimento 30s");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            check(failures, modules.insert(ItemResource.of(ModItems.SPEED_UPGRADE.get()), 1, transaction) == 0, "reator recusa chips");
        }
        fusion.energy().set(0);
        for (int i = 0; i < 200; i++) fusion.tick();
        check(failures, fusion.heat() > slowHeat + 200, "ignição III aquece bem mais rápido (" + fusion.heat() + "‰)");

        // Quebrar uma peça desfaz o reator e as peças voltam a aparecer
        level.setBlock(coil, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 10; i++) fusion.tick();
        BlockPos casing = FusionStructure.toWorld(controller, facing, 1, 0, 1);
        check(failures, !fusion.isFormed() && !level.getBlockState(casing).getValue(com.atomcraft.block.ReactorPartBlock.FORMED),
                "quebrar uma bobina desfaz o reator");

        for (FusionStructure.Slot slot : FusionStructure.blueprint(controller, facing)) {
            level.setBlock(slot.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(drivePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void place(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
    }
}
