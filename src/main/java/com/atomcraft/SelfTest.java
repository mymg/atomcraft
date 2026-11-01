package com.atomcraft;

import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.atomcraft.block.entity.AtomizerBlockEntity;
import com.atomcraft.block.entity.CarbonGeneratorBlockEntity;
import com.atomcraft.block.entity.CombustionGeneratorBlockEntity;
import com.atomcraft.block.entity.FusionReactorBlockEntity;
import com.atomcraft.block.entity.TerminalBlockEntity;
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
        for (String recipe : List.of("atomizer", "analyzer", "replicator", "drive", "atomic_cable",
                "element_disk_16k", "element_disk_64k", "element_disk_256k", "element_disk_1m",
                "terminal", "combustion_generator", "carbon_generator", "fusion_reactor")) {
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
        int required = ResearchData.required(Items.IRON_INGOT);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int accepted = analyzer.automation().insert(ItemResource.of(Items.IRON_INGOT), 99, transaction);
            check(failures, accepted == required, "analisador aceita só o que falta (" + accepted + "/" + required + ")");
        }
        analyzer.buffer().set(0, ItemResource.of(Items.IRON_INGOT), Math.min(99, required));
        for (int i = 0; i < required * (Config.ANALYZE_TICKS.get() + 1) + 20; i++) analyzer.serverTick();
        ResearchData research = ResearchData.get(server);
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
        place(level, drivePos.north(2), ModBlocks.FUSION_REACTOR.get());
        place(level, drivePos.south(), ModBlocks.TERMINAL.get());
        DriveBlockEntity drive = (DriveBlockEntity) level.getBlockEntity(drivePos);
        CarbonGeneratorBlockEntity carbon = (CarbonGeneratorBlockEntity) level.getBlockEntity(drivePos.east());
        AtomizerBlockEntity atomizer = (AtomizerBlockEntity) level.getBlockEntity(drivePos.west());
        FusionReactorBlockEntity fusion = (FusionReactorBlockEntity) level.getBlockEntity(drivePos.north(2));
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

        for (int i = 0; i < 400; i++) fusion.serverTick();
        network = ElementNetwork.scan(level, drivePos);
        long hydrogen = network.totals().get(ChemicalElement.HYDROGEN), helium = network.totals().get(ChemicalElement.HELIUM);
        check(failures, hydrogen < 400 && helium > 0 && (400 - hydrogen) == helium * 4,
                "fusão consumiu H e produziu He (H" + hydrogen + " He" + helium + ")");
        check(failures, fusion.heat() > 0, "plasma aqueceu (" + fusion.heat() + ")");

        for (BlockPos pos : List.of(combustionPos, drivePos, drivePos.east(), drivePos.west(), drivePos.north(), drivePos.north(2), drivePos.south())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void place(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
    }
}
