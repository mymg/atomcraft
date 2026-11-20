package com.atomcraft;

import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.atomcraft.block.entity.AtomizerBlockEntity;
import com.atomcraft.registry.ModComponents;
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
import com.atomcraft.storage.ElementDiskItem;
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
import net.minecraft.world.level.block.state.BlockState;
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

        partitions(level, failures);
        tallAnalyzer(level, failures);
        generators(level, failures);
        controlsAndSynthesis(server, level, failures);
        materials(server, level, failures);
        replicatorTiers(level, failures);
        energyStorage(level, failures);
        toolsAndWireless(level, failures);
        equipment(level, failures);
    }

    /** Ferramentas/armaduras de liga, Cartão de Rede + Transmissor Sem Fio e o Desintegrador Atômico. */
    private static void toolsAndWireless(ServerLevel level, List<String> failures) {
        var steelPick = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.PICKAXES.get(com.atomcraft.tools.ModTools.Tier.STEEL).get());
        var titaniumPick = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.PICKAXES.get(com.atomcraft.tools.ModTools.Tier.TITANIUM_ALLOY).get());
        var tungstenPick = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.PICKAXES.get(com.atomcraft.tools.ModTools.Tier.TUNGSTEN_CARBIDE).get());
        var obsidian = Blocks.OBSIDIAN.defaultBlockState();
        check(failures, steelPick.getMaxDamage() == 500 && steelPick.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState())
                && !steelPick.isCorrectToolForDrops(obsidian), "picareta de aço: 500 de durabilidade, minera ferro, não obsidiana");
        check(failures, titaniumPick.isCorrectToolForDrops(obsidian) && tungstenPick.isCorrectToolForDrops(obsidian),
                "picaretas de titânio e tungstênio mineram obsidiana");
        check(failures, tungstenPick.getDestroySpeed(Blocks.STONE.defaultBlockState()) > titaniumPick.getDestroySpeed(Blocks.STONE.defaultBlockState()),
                "carbeto de tungstênio minera mais rápido que titânio");
        var chest = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.ARMOR.get(1).get());
        check(failures, chest.get(net.minecraft.core.component.DataComponents.EQUIPPABLE) != null, "peitoral de aço é vestível");

        // Transmissor + cartão vinculado a um jogador falso
        BlockPos drivePos = new BlockPos(0, 120, 108);
        level.getChunk(drivePos);
        place(level, drivePos, ModBlocks.DRIVE.get());
        place(level, drivePos.east(), com.atomcraft.tools.ModTools.WIRELESS_TRANSMITTER.get());
        ((DriveBlockEntity) level.getBlockEntity(drivePos)).disks().set(0, ItemResource.of(ModItems.DISK_64K.get()), 1);
        var transmitter = (com.atomcraft.block.entity.WirelessTransmitterBlockEntity) level.getBlockEntity(drivePos.east());
        transmitter.energy().set(50_000);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.fromString("00000000-0000-0000-0000-00000000a70c"), "atomcraft_tester"));
        player.setPos(drivePos.getX() + 5, drivePos.getY(), drivePos.getZ());
        player.getInventory().clearContent();
        check(failures, com.atomcraft.wireless.Wireless.find(player) == null, "sem cartão não há rede sem fio");
        var card = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.NETWORK_CARD.get());
        card.set(ModComponents.WIRELESS_LINK.get(), new com.atomcraft.wireless.WirelessLink(level.dimension(), drivePos.east(),
                java.util.UUID.randomUUID(), "outro"));
        player.getInventory().setItem(0, card);
        check(failures, com.atomcraft.wireless.Wireless.find(player) == null, "cartão de outro jogador não funciona");
        card.set(ModComponents.WIRELESS_LINK.get(), new com.atomcraft.wireless.WirelessLink(level.dimension(), drivePos.east(),
                player.getUUID(), player.getName().getString()));
        check(failures, com.atomcraft.wireless.Wireless.find(player) == transmitter, "cartão vinculado alcança o transmissor");
        player.setPos(drivePos.getX() + 1000, drivePos.getY(), drivePos.getZ());
        check(failures, com.atomcraft.wireless.Wireless.find(player) == null, "fora do alcance não funciona");
        player.setPos(drivePos.getX() + 5, drivePos.getY(), drivePos.getZ());

        // Desintegrador: carrega pelo transmissor e manda o bloco quebrado para a rede
        var disintegrator = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.DISINTEGRATOR.get());
        check(failures, disintegrator.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 1f, "desintegrador sem energia é lento");
        int moved = com.atomcraft.tools.AtomicDisintegratorItem.charge(disintegrator, transmitter, 20_000);
        check(failures, moved == 20_000 && transmitter.energy().getAmountAsInt() == 30_000, "desintegrador carrega pelo transmissor");
        check(failures, disintegrator.getDestroySpeed(Blocks.STONE.defaultBlockState()) > 10f
                && disintegrator.isCorrectToolForDrops(Blocks.OAK_LOG.defaultBlockState()), "desintegrador carregado é rápido e serve para tudo");
        long before = transmitter.network().totals().elements().values().stream().mapToLong(Long::longValue).sum();
        check(failures, com.atomcraft.tools.AtomicDisintegratorItem.atomize(transmitter, new net.minecraft.world.item.ItemStack(Items.COBBLESTONE, 4)),
                "pedregulho desintegrado vai para a rede");
        long after = transmitter.network().totals().elements().values().stream().mapToLong(Long::longValue).sum();
        check(failures, after > before, "rede ganhou os elementos do pedregulho (" + before + " → " + after + ")");
    }

    /** Bancada de Módulos, Exoesqueleto, Jetpack, Anel Magnético, Desintegrador com módulos e Curios. */
    private static void equipment(ServerLevel level, List<String> failures) {
        BlockPos drivePos = new BlockPos(0, 120, 116);
        level.getChunk(drivePos);
        place(level, drivePos, ModBlocks.DRIVE.get());
        place(level, drivePos.east(), com.atomcraft.tools.ModTools.WIRELESS_TRANSMITTER.get());
        place(level, drivePos.west(), com.atomcraft.equipment.ModEquipment.MODULE_BENCH.get());
        ((DriveBlockEntity) level.getBlockEntity(drivePos)).disks().set(0, ItemResource.of(ModItems.DISK_64K.get()), 1);
        var transmitter = (com.atomcraft.block.entity.WirelessTransmitterBlockEntity) level.getBlockEntity(drivePos.east());
        transmitter.energy().set(500_000);
        transmitter.network().insert(new com.atomcraft.chemistry.Composition(java.util.Map.of(com.atomcraft.chemistry.ChemicalElement.HYDROGEN, 5_000L)), 1);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.fromString("00000000-0000-0000-0000-00000000e9a1"), "atomcraft_equipment"));
        player.setPos(drivePos.getX() + 2, drivePos.getY(), drivePos.getZ() + 1);
        player.getInventory().clearContent();
        var modules = com.atomcraft.equipment.ModEquipment.MODULES;

        // Bancada: instala, tira o equipamento (módulos vão junto), devolve e lê de novo
        var bench = new com.atomcraft.menu.ModuleBenchMenu(0, player.getInventory(), drivePos.west());
        var legs = new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.EXO_LEGGINGS.get());
        bench.insertEquipment(legs);
        check(failures, !bench.accepts(0, new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.NIGHT_VISION).get())),
                "bancada recusa visão noturna nas calças");
        check(failures, !bench.accepts(2, new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.SPEED).get())),
                "calças têm só 2 slots");
        check(failures, bench.slots.get(1).mayPlace(new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.SPEED).get()))
                && bench.slots.get(2).isActive() && !bench.slots.get(3).isActive(), "slots da bancada: 1º aceita velocidade, 3º fica inativo nas calças");
        bench.insertModule(0, new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.SPEED).get(), 2));
        bench.insertModule(1, new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.STEP_ASSIST).get()));
        check(failures, !bench.accepts(1, new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.SPEED).get())),
                "bancada não repete módulo em dois slots");
        check(failures, com.atomcraft.equipment.ModularItem.level(legs, com.atomcraft.equipment.EquipmentModule.SPEED) == 2
                && com.atomcraft.equipment.ModularItem.level(legs, com.atomcraft.equipment.EquipmentModule.STEP_ASSIST) == 1,
                "módulos gravados nas calças (velocidade II, degraus)");
        bench.insertEquipment(net.minecraft.world.item.ItemStack.EMPTY);
        check(failures, bench.slots.get(1).getItem().isEmpty() && bench.slots.get(2).getItem().isEmpty(), "tirar o equipamento leva os módulos");
        bench.insertEquipment(legs);
        check(failures, bench.slots.get(1).getItem().getCount() == 2, "módulos voltam ao recolocar o equipamento");
        legs = legs.copy();
        bench.removed(player);
        check(failures, bench.equipment().isEmpty() && bench.slots.get(1).getItem().isEmpty(), "fechar a bancada esvazia os slots (equipamento devolvido)");
        player.getInventory().clearContent();

        // Exoesqueleto: velocidade e degraus com energia; sem energia, param
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, legs);
        com.atomcraft.equipment.Battery.set(legs, 1_000, com.atomcraft.equipment.ExoArmorItem.CAPACITY);
        com.atomcraft.equipment.EquipmentEvents.tickExo(player, 10);
        var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var step = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        check(failures, speed.hasModifier(AtomCraft.id("exo_speed")) && step.getValue() > 1.0, "calças com energia: velocidade e degraus");
        check(failures, com.atomcraft.equipment.Battery.energy(legs) == 1_000 - (2 * 2 + 1) * 10, "módulos gastam energia (" + com.atomcraft.equipment.Battery.energy(legs) + ")");
        com.atomcraft.equipment.Battery.set(legs, 0, com.atomcraft.equipment.ExoArmorItem.CAPACITY);
        com.atomcraft.equipment.EquipmentEvents.tickExo(player, 10);
        check(failures, !speed.hasModifier(AtomCraft.id("exo_speed")), "sem energia os módulos param");

        // Escudo no peitoral e recarga sem fio
        var chest = new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.EXO_CHESTPLATE.get());
        com.atomcraft.equipment.ModularItem.setModules(chest, List.of(new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.SHIELD).get()),
                new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.PLATING).get(), 3),
                net.minecraft.world.item.ItemStack.EMPTY));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        com.atomcraft.equipment.Battery.set(chest, 5_000, com.atomcraft.equipment.ExoArmorItem.CAPACITY);
        float left = com.atomcraft.equipment.EquipmentEvents.absorb(player, 4f);
        check(failures, left == 0f && com.atomcraft.equipment.Battery.energy(chest) == 1_000, "escudo absorve 4 de dano com 4.000 FE");
        com.atomcraft.equipment.EquipmentEvents.tickExo(player, 10);
        check(failures, player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).hasModifier(AtomCraft.id("exo_plating_chest")),
                "blindagem III no peitoral dá armadura");
        var card = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.NETWORK_CARD.get());
        card.set(ModComponents.WIRELESS_LINK.get(), new com.atomcraft.wireless.WirelessLink(level.dimension(), drivePos.east(),
                player.getUUID(), player.getName().getString()));
        player.getInventory().setItem(0, card);
        com.atomcraft.equipment.EquipmentEvents.recharge(player);
        check(failures, com.atomcraft.equipment.Battery.energy(chest) > 1_000 && com.atomcraft.equipment.Battery.energy(legs) > 0,
                "exoesqueleto recarrega pelo transmissor");

        // Jetpack: reabastece hidrogênio da rede
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        var jetpack = new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.JETPACK.get());
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, jetpack);
        check(failures, com.atomcraft.equipment.Worn.jetpack(player) == jetpack, "jetpack vestido no peito");
        long hBefore = transmitter.network().totals().get(com.atomcraft.chemistry.ChemicalElement.HYDROGEN);
        com.atomcraft.equipment.EquipmentEvents.recharge(player);
        long hAfter = transmitter.network().totals().get(com.atomcraft.chemistry.ChemicalElement.HYDROGEN);
        int fuel = com.atomcraft.equipment.JetpackItem.fuel(jetpack);
        check(failures, fuel > 0 && hBefore - hAfter == fuel, "jetpack tira hidrogênio da rede (" + fuel + " H)");

        // Anel Magnético puxa itens próximos
        player.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.MAGNET.get()));
        var dropped = new net.minecraft.world.entity.item.ItemEntity(level, player.getX() + 5, player.getY(), player.getZ(),
                new net.minecraft.world.item.ItemStack(Items.COBBLESTONE));
        level.addFreshEntity(dropped);
        int pulled = com.atomcraft.equipment.MagnetItem.pull(player);
        check(failures, pulled >= 1 && dropped.getDeltaMovement().x < 0, "anel magnético puxa o item em direção ao jogador");
        dropped.discard();

        // Desintegrador com Eficiência é mais rápido
        var plain = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.DISINTEGRATOR.get());
        var fast = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.DISINTEGRATOR.get());
        com.atomcraft.tools.AtomicDisintegratorItem.setEnergy(plain, 50_000);
        com.atomcraft.tools.AtomicDisintegratorItem.setEnergy(fast, 50_000);
        com.atomcraft.equipment.ModularItem.setModules(fast, List.of(new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.EFFICIENCY).get(), 3),
                net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.item.ItemStack.EMPTY));
        check(failures, fast.getDestroySpeed(Blocks.STONE.defaultBlockState()) > plain.getDestroySpeed(Blocks.STONE.defaultBlockState()),
                "módulo de Eficiência acelera o desintegrador");

        // Curios: slots de anel/costas/amuleto e acessório equipado conta
        if (com.atomcraft.compat.Curios.LOADED) {
            check(failures, com.atomcraft.compat.Curios.hasPlayerSlot(level, "ring") && com.atomcraft.compat.Curios.hasPlayerSlot(level, "back")
                    && com.atomcraft.compat.Curios.hasPlayerSlot(level, "charm"), "Curios: jogador tem slots de anel, costas e amuleto");
            player.getInventory().clearContent();
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
            var ring = new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.ANALYZER_RING.get());
            boolean equipped = com.atomcraft.compat.Curios.equip(player, "ring", ring);
            check(failures, equipped, "Curios: anel equipado no slot de anel");
            check(failures, com.atomcraft.equipment.Worn.hasAnalyzer(player), "Curios: Anel Analisador no slot de anel funciona ("
                    + com.atomcraft.compat.Curios.equipped(player).size() + " itens no Curios)");
            boolean back = com.atomcraft.compat.Curios.equip(player, "back", jetpack);
            check(failures, back && com.atomcraft.equipment.Worn.jetpack(player) == jetpack, "Curios: jetpack nas costas funciona");
            com.atomcraft.compat.Curios.equip(player, "ring", net.minecraft.world.item.ItemStack.EMPTY);
            com.atomcraft.compat.Curios.equip(player, "back", net.minecraft.world.item.ItemStack.EMPTY);
        } else {
            AtomCraft.LOGGER.info("AtomCraft SELFTEST Curios ausente: integração não testada");
        }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.item.ItemStack.EMPTY);
        player.getInventory().clearContent();
    }

    /** Células de Energia (lados, rede, item) e Matriz de Energia (estrutura 3×5×3 e portas). */
    private static void energyStorage(ServerLevel level, List<String> failures) {
        BlockPos a = new BlockPos(0, 120, 96);
        level.getChunk(a);
        place(level, a, ModBlocks.BASIC_ENERGY_CELL.get());
        place(level, a.north(), ModBlocks.BASIC_ENERGY_CELL.get());
        var cellA = (com.atomcraft.block.entity.EnergyCellBlockEntity) level.getBlockEntity(a);
        var cellB = (com.atomcraft.block.entity.EnergyCellBlockEntity) level.getBlockEntity(a.north());
        int rate = com.atomcraft.energy.CellTier.BASIC.rate();
        check(failures, cellA.energy().getCapacityAsInt() == com.atomcraft.energy.CellTier.BASIC.capacity(), "célula básica com a capacidade da config");
        cellA.energy().set(100_000);
        cellA.tick();
        check(failures, cellB.energy().getAmountAsInt() == rate && cellA.energy().getAmountAsInt() == 100_000 - rate,
                "frente da célula envia " + rate + " FE/t para a vizinha (" + cellB.energy().getAmountAsInt() + ")");
        cellA.setSide(com.atomcraft.energy.CellSide.FRONT, com.atomcraft.energy.SideMode.NONE);
        check(failures, cellA.handlerFor(net.minecraft.core.Direction.NORTH) == null, "lado desligado não expõe energia");
        cellA.tick();
        check(failures, cellB.energy().getAmountAsInt() == rate, "lado desligado não envia");
        var inputSide = cellA.handlerFor(net.minecraft.core.Direction.SOUTH);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            check(failures, inputSide != null && inputSide.extract(100, transaction) == 0 && inputSide.insert(100, transaction) == 100,
                    "lado de entrada só recebe");
        }
        check(failures, Integer.valueOf(cellA.energy().getAmountAsInt()).equals(cellA.collectComponents().get(ModComponents.STORED_ENERGY.get())),
                "energia vai para o item ao quebrar");

        // Pela rede: alimenta as máquinas (não outras células)
        BlockPos drivePos = a.east(4);
        place(level, drivePos, ModBlocks.DRIVE.get());
        place(level, drivePos.east(), ModBlocks.ATOMIZER.get());
        place(level, drivePos.west(), ModBlocks.BASIC_ENERGY_CELL.get());
        var netCell = (com.atomcraft.block.entity.EnergyCellBlockEntity) level.getBlockEntity(drivePos.west());
        var atomizer = (AtomizerBlockEntity) level.getBlockEntity(drivePos.east());
        netCell.setSide(com.atomcraft.energy.CellSide.FRONT, com.atomcraft.energy.SideMode.INPUT);
        netCell.energy().set(50_000);
        int before = atomizer.energy().getAmountAsInt();
        netCell.tick();
        check(failures, atomizer.energy().getAmountAsInt() > before, "célula alimenta máquina da rede pelo cabo/drive");

        // Matriz de Energia: estrutura 3×5×3 com uma porta de entrada e uma de saída
        BlockPos controller = new BlockPos(20, 122, 96);
        level.getChunk(controller);
        var facing = net.minecraft.core.Direction.NORTH;
        level.setBlock(controller, ModBlocks.ENERGY_MATRIX.get().defaultBlockState().setValue(com.atomcraft.block.MachineBlock.FACING, facing), Block.UPDATE_ALL);
        var matrix = (com.atomcraft.block.entity.EnergyCellBlockEntity) level.getBlockEntity(controller);
        matrix.tick();
        check(failures, !matrix.isFormed() && matrix.status() == MachineStatus.INCOMPLETE, "matriz sem estrutura fica incompleta");
        BlockPos inPort = com.atomcraft.energy.MatrixStructure.toWorld(controller, facing, 0, 0, 1);
        BlockPos outPort = com.atomcraft.energy.MatrixStructure.toWorld(controller, facing, 2, 0, 1);
        for (var slot : com.atomcraft.energy.MatrixStructure.blueprint(controller, facing)) {
            var block = switch (slot.part()) {
                case CASING -> ModBlocks.MATRIX_CASING.get();
                case GLASS -> ModBlocks.MATRIX_GLASS.get();
                case CORE -> ModBlocks.MATRIX_CORE.get();
                default -> null;
            };
            if (slot.pos().equals(inPort) || slot.pos().equals(outPort)) block = ModBlocks.MATRIX_PORT.get();
            if (block != null) level.setBlock(slot.pos(), block.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(outPort, level.getBlockState(outPort).setValue(com.atomcraft.block.MatrixPortBlock.MODE, com.atomcraft.energy.PortMode.OUTPUT), Block.UPDATE_ALL);
        // Interfaces nas laterais da base: o lado de fora da da esquerda é o leste e o da direita, o oeste (olhando para o norte)
        place(level, outPort.west(), ModBlocks.BASIC_ENERGY_CELL.get());
        var receiver = (com.atomcraft.block.entity.EnergyCellBlockEntity) level.getBlockEntity(outPort.west());
        check(failures, matrix.checkStructure() && matrix.ports().size() == 2, "matriz forma com 2 portas (" + matrix.ports().size() + ")");
        check(failures, matrix.energy().getCapacityAsInt() == com.atomcraft.energy.CellTier.MATRIX.capacity(), "matriz com 3 núcleos de capacidade");
        var inHandler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, inPort, net.minecraft.core.Direction.EAST);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            check(failures, inHandler != null && inHandler.insert(10_000, transaction) == 10_000, "porta de entrada recebe energia");
            transaction.commit();
        }
        level.setBlock(inPort.east(), ModBlocks.DRIVE.get().defaultBlockState(), Block.UPDATE_ALL);
        check(failures, com.atomcraft.storage.ElementNetwork.scan(level, inPort.east()).receivers().contains(matrix),
                "Cabo/Drive ligado à Porta da Matriz alcança o controlador");
        matrix.tick();
        check(failures, receiver.energy().getAmountAsInt() > 0, "porta de saída alimenta a célula vizinha (" + receiver.energy().getAmountAsInt() + ")");
        matrix.energy().set(1_000_000);
        matrix.checkStructure();
        check(failures, level.getBlockState(controller.south().above()).getValue(com.atomcraft.block.StructurePartBlock.FORMED),
                "matriz formada: peças escondidas");
        check(failures, level.getBlockState(inPort).getRenderShape() == net.minecraft.world.level.block.RenderShape.MODEL, "matriz formada: interfaces visíveis");
        check(failures, level.getBlockState(controller).getValue(com.atomcraft.block.EnergyMatrixBlock.CHARGED), "matriz com energia: bobina acesa");
        matrix.energy().set(0);
        matrix.checkStructure();
        check(failures, !level.getBlockState(controller).getValue(com.atomcraft.block.EnergyMatrixBlock.CHARGED), "matriz vazia: bobina apagada");
        level.setBlock(controller.south().above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        check(failures, !matrix.checkStructure(), "matriz desforma sem um núcleo");
        check(failures, !level.getBlockState(controller.south()).getValue(com.atomcraft.block.StructurePartBlock.FORMED), "peças reaparecem ao desformar");
        // Interface fora do lugar (num canto da base) não forma
        level.setBlock(controller.south().above(), ModBlocks.MATRIX_CORE.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos corner = com.atomcraft.energy.MatrixStructure.toWorld(controller, facing, 0, 0, 0);
        level.setBlock(corner, ModBlocks.MATRIX_PORT.get().defaultBlockState(), Block.UPDATE_ALL);
        check(failures, !matrix.checkStructure(), "interface no canto da base não forma a matriz");
        level.setBlock(corner, ModBlocks.MATRIX_CASING.get().defaultBlockState(), Block.UPDATE_ALL);
        check(failures, matrix.checkStructure(), "matriz forma de novo");
    }

    /** Complexidade: cada nível de replicador só aceita itens até certo número de elementos (e exóticos). */
    private static void replicatorTiers(ServerLevel level, List<String> failures) {
        var basic = com.atomcraft.replication.ReplicatorTier.BASIC;
        var advanced = com.atomcraft.replication.ReplicatorTier.ADVANCED;
        var quantum = com.atomcraft.replication.ReplicatorTier.QUANTUM;
        var iron = com.atomcraft.chemistry.AtomicValues.get(Items.IRON_INGOT, false).composition();
        var gunpowder = com.atomcraft.chemistry.AtomicValues.get(Items.GUNPOWDER, false).composition();
        var redstone = com.atomcraft.chemistry.AtomicValues.get(Items.REDSTONE, false).composition();
        var milk = com.atomcraft.chemistry.AtomicValues.get(Items.MILK_BUCKET, false).composition();
        check(failures, basic.maxElements() == 4 && advanced.maxElements() == 6 && quantum.maxElements() == -1, "limites padrão 4 / 6 / sem limite");
        check(failures, basic.refusal(iron) == null, "básico replica barra de ferro");
        check(failures, basic.refusal(gunpowder) != null, "básico recusa pólvora (" + gunpowder.elements().size() + " elementos)");
        check(failures, basic.refusal(redstone) != null, "básico recusa redstone (exótico)");
        check(failures, advanced.refusal(gunpowder) == null && advanced.refusal(redstone) == null, "avançado aceita pólvora e redstone");
        check(failures, advanced.refusal(milk) != null, "avançado recusa balde de leite (" + milk.elements().size() + " elementos)");
        check(failures, quantum.refusal(milk) == null, "quântico aceita tudo");

        // Pedido complexo demais no básico: espera com o aviso, sem consumir nada
        BlockPos pos = new BlockPos(0, 120, 84);
        level.getChunk(pos);
        place(level, pos, ModBlocks.REPLICATOR.get());
        var replicator = (ReplicatorBlockEntity) level.getBlockEntity(pos);
        replicator.energy().set(replicator.energy().getCapacityAsInt());
        replicator.startJob(TESTER, Items.GUNPOWDER, 1);
        replicator.tick();
        check(failures, replicator.status() == MachineStatus.TOO_COMPLEX, "básico fica em 'complexo demais' (" + replicator.status() + ")");
        replicator.cancelJob();

        place(level, pos.east(3), ModBlocks.ADVANCED_REPLICATOR.get());
        var adv = (ReplicatorBlockEntity) level.getBlockEntity(pos.east(3));
        check(failures, adv != null && adv.tier() == advanced && adv.getType() == com.atomcraft.registry.ModBlockEntities.ADVANCED_REPLICATOR.get(),
                "Replicador Avançado tem o nível certo");

        // Quântico: sem estrutura fica incompleto; montada, forma
        BlockPos controller = new BlockPos(10, 120, 84);
        level.getChunk(controller);
        level.setBlock(controller, ModBlocks.QUANTUM_REPLICATOR.get().defaultBlockState()
                .setValue(com.atomcraft.block.MachineBlock.FACING, net.minecraft.core.Direction.NORTH), Block.UPDATE_ALL);
        var quantumBe = (ReplicatorBlockEntity) level.getBlockEntity(controller);
        quantumBe.tick();
        check(failures, quantumBe.status() == MachineStatus.INCOMPLETE && !quantumBe.isFormed(), "quântico sem estrutura fica incompleto");
        for (var slot : com.atomcraft.replication.QuantumStructure.blueprint(controller, net.minecraft.core.Direction.NORTH)) {
            var block = switch (slot.part()) {
                case CASING -> ModBlocks.QUANTUM_CASING.get();
                case GLASS -> ModBlocks.QUANTUM_GLASS.get();
                case CORE -> ModBlocks.QUANTUM_CORE.get();
                case CONTROLLER, PORT -> null;
            };
            if (block != null) level.setBlock(slot.pos(), block.defaultBlockState(), Block.UPDATE_ALL);
        }
        // Porta Quântica no lugar de uma carcaça, com um Drive encostado: a rede chega ao controlador
        BlockPos qPort = com.atomcraft.replication.QuantumStructure.toWorld(controller, net.minecraft.core.Direction.NORTH, 0, 0, 1);
        level.setBlock(qPort, ModBlocks.QUANTUM_PORT.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(qPort.east(), ModBlocks.DRIVE.get().defaultBlockState(), Block.UPDATE_ALL);
        ((DriveBlockEntity) level.getBlockEntity(qPort.east())).disks().set(0, ItemResource.of(ModItems.DISK_64K.get()), 1);
        check(failures, quantumBe.checkStructure() && quantumBe.ports().size() == 1, "quântico forma com uma Porta Quântica");
        check(failures, com.atomcraft.storage.ElementNetwork.scan(level, qPort.east()).receivers().contains(quantumBe),
                "rede do Drive chega ao Replicador Quântico pela porta");
        check(failures, quantumBe.network().hasStorage(), "Replicador Quântico vê o Drive (elementos) pela porta");
        check(failures, level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, qPort, net.minecraft.core.Direction.EAST) != null
                && level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, qPort, net.minecraft.core.Direction.EAST) != null,
                "Porta Quântica expõe energia e itens");
        check(failures, quantumBe.checkStructure() && quantumBe.isFormed()
                && level.getBlockState(controller).getValue(com.atomcraft.block.QuantumReplicatorBlock.FORMED), "quântico forma com a estrutura 3×3×3");
        quantumBe.tick();
        check(failures, quantumBe.status() == MachineStatus.IDLE, "quântico formado fica pronto (" + quantumBe.status() + ")");
        level.setBlock(controller.south().above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        check(failures, !quantumBe.checkStructure(), "quântico desforma sem o núcleo");
    }

    /** Materiais: elementos novos, receitas da forja sem criar/destruir matéria, Forja de Ligas e minérios no mundo. */
    private static void materials(MinecraftServer server, ServerLevel level, List<String> failures) {
        for (String symbol : List.of("Li", "Ti", "V", "Cr", "Co", "W")) {
            check(failures, com.atomcraft.chemistry.ChemicalElement.bySymbol(symbol) != null, "elemento " + symbol + " existe");
        }
        var recipes = com.atomcraft.alloy.AlloyRecipes.all();
        check(failures, recipes.size() >= 17, "receitas da forja carregadas (" + recipes.size() + ")");
        for (var recipe : recipes) {
            var value = com.atomcraft.chemistry.AtomicValues.get(recipe.result(), false);
            check(failures, value != null && value.composition().equals(recipe.elements()),
                    "forja conserva a matéria: " + recipe.result() + " " + (value == null ? "sem valor" : value.composition()) + " vs " + recipe.elements());
        }
        for (var plate : com.atomcraft.material.ModMaterials.PLATES.values()) {
            check(failures, com.atomcraft.chemistry.AtomicValues.get(plate.get(), false) != null, plate.getId() + " tem valor (derivado)");
        }
        check(failures, com.atomcraft.chemistry.AtomicValues.get(
                com.atomcraft.material.ModMaterials.SLABS.get(com.atomcraft.material.Material.STEEL).get().asItem(), false) != null,
                "laje de aço tem valor (derivado)");

        // Forja: aço a partir de Fe e C da rede
        BlockPos drivePos = new BlockPos(0, 120, 72);
        level.getChunk(drivePos);
        place(level, drivePos, ModBlocks.DRIVE.get());
        place(level, drivePos.east(), ModBlocks.ALLOY_FORGE.get());
        DriveBlockEntity drive = (DriveBlockEntity) level.getBlockEntity(drivePos);
        var forge = (com.atomcraft.block.entity.AlloyForgeBlockEntity) level.getBlockEntity(drivePos.east());
        drive.disks().set(0, ItemResource.of(ModItems.DISK_64K.get()), 1);
        var network = forge.network();
        var fe = com.atomcraft.chemistry.ChemicalElement.IRON;
        var c = com.atomcraft.chemistry.ChemicalElement.CARBON;
        network.insert(new com.atomcraft.chemistry.Composition(java.util.Map.of(fe, 16L, c, 2L)), 1);
        forge.energy().set(forge.energy().getCapacityAsInt());
        var steel = com.atomcraft.material.ModMaterials.INGOTS.get(com.atomcraft.material.Material.STEEL).get();
        forge.select(steel);
        for (int i = 0; i < 400; i++) forge.tick();
        check(failures, forge.output().getAmountAsInt(0) == 2 && forge.output().getResource(0).getItem() == steel,
                "forja fez 2 lingotes de aço (" + forge.output().getAmountAsInt(0) + ")");
        check(failures, network.totals().get(fe) == 0 && network.totals().get(c) == 0, "forja consumiu Fe e C da rede");
        forge.tick();
        check(failures, forge.status() == MachineStatus.NO_ELEMENTS, "forja para sem elementos (" + forge.status() + ")");

        // Minérios: features registradas e geradas
        var placed = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
        for (var ore : com.atomcraft.material.OreType.values()) {
            check(failures, placed.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
                    AtomCraft.id("ore_" + ore.id()))).isPresent(), "feature do minério " + ore.id() + " registrada");
        }
        // O mundo do teste é plano (sem minérios): coloca o veio à mão num cubo de pedra.
        var configured = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                .get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, AtomCraft.id("ore_tin")));
        var modifiers = server.registryAccess().lookupOrThrow(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        check(failures, modifiers.get(net.minecraft.resources.ResourceKey.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                AtomCraft.id("ore_tin"))).isPresent(), "biome modifier do estanho carregado");
        BlockPos center = new BlockPos(200, 100, 200);
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-6, -6, -6), center.offset(6, 6, 6))) {
            level.getChunk(p);
            level.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        var random = net.minecraft.util.RandomSource.create(42);
        if (configured.isPresent()) {
            for (int i = 0; i < 8; i++) configured.get().value().place(level, level.getChunkSource().getGenerator(), random, center);
        }
        var tin = com.atomcraft.material.ModMaterials.ORES.get(com.atomcraft.material.OreType.TIN).get();
        int found = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-8, -8, -8), center.offset(8, 8, 8))) {
            if (level.getBlockState(p).is(tin)) found++;
        }
        check(failures, found > 0, "veio de estanho gerado na pedra (" + found + " blocos)");
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
        combustion.tick();
        check(failures, level.getBlockState(combustionPos).getValue(com.atomcraft.block.GeneratorBlock.LIT)
                == (combustion.status() == com.atomcraft.block.entity.MachineStatus.WORKING), "gerador aceso enquanto gera");
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
                case CONTROLLER, PORT -> null;
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
        if (block instanceof com.atomcraft.block.TallMachineBlock) {
            com.atomcraft.block.TallMachineBlock.place(level, pos, block.defaultBlockState());
        } else {
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Máquinas de 2 blocos: a metade de cima existe, não tem block entity, não conecta e some junto com a de baixo. */
    private static void tallAnalyzer(ServerLevel level, List<String> failures) {
        BlockPos pos = new BlockPos(0, 120, 60);
        level.getChunk(pos);
        place(level, pos, ModBlocks.ANALYZER.get());
        BlockState upper = level.getBlockState(pos.above());
        check(failures, upper.is(ModBlocks.ANALYZER.get()) && com.atomcraft.block.AnalyzerBlock.isUpper(upper), "analisador tem metade de cima");
        check(failures, level.getBlockEntity(pos) instanceof AnalyzerBlockEntity, "metade de baixo tem a máquina");
        check(failures, level.getBlockEntity(pos.above()) == null, "metade de cima não tem block entity");
        check(failures, com.atomcraft.storage.NetworkNode.connects(level.getBlockState(pos))
                && !com.atomcraft.storage.NetworkNode.connects(upper), "só a metade de baixo conecta na rede");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        check(failures, level.getBlockState(pos.above()).isAir(), "metade de cima some com a de baixo");
        for (Block block : List.of(ModBlocks.ATOMIZER.get(), ModBlocks.REPLICATOR.get())) {
            place(level, pos, block);
            check(failures, com.atomcraft.block.TallMachineBlock.isUpper(level.getBlockState(pos.above()))
                    && level.getBlockEntity(pos.above()) == null, block.getDescriptionId() + " tem 2 blocos");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Partição de discos: só os elementos permitidos entram, respeitando o limite. */
    private static void partitions(ServerLevel level, List<String> failures) {
        BlockPos drivePos = new BlockPos(0, 120, 48);
        level.getChunk(drivePos);
        place(level, drivePos, ModBlocks.DRIVE.get());
        DriveBlockEntity drive = (DriveBlockEntity) level.getBlockEntity(drivePos);
        net.minecraft.world.item.ItemStack disk = new net.minecraft.world.item.ItemStack(ModItems.DISK_16K.get());
        disk.set(com.atomcraft.registry.ModComponents.DISK_PARTITION.get(),
                com.atomcraft.storage.DiskPartition.of(java.util.Map.of(ChemicalElement.IRON, 0L, ChemicalElement.CARBON, 10L)));
        drive.disks().set(0, ItemResource.of(disk), 1);
        ElementNetwork network = ElementNetwork.scan(level, drivePos);
        check(failures, !network.insert(new Composition(java.util.Map.of(ChemicalElement.OXYGEN, 1L)), 1), "disco particionado recusa O");
        check(failures, network.insert(new Composition(java.util.Map.of(ChemicalElement.IRON, 500L)), 1), "disco particionado aceita Fe");
        check(failures, network.insert(new Composition(java.util.Map.of(ChemicalElement.CARBON, 8L)), 1)
                && !network.insert(new Composition(java.util.Map.of(ChemicalElement.CARBON, 8L)), 1), "limite de 10 C respeitado");
        check(failures, !network.insert(new Composition(java.util.Map.of(ChemicalElement.IRON, 1L, ChemicalElement.OXYGEN, 1L)), 1)
                && network.totals().get(ChemicalElement.IRON) == 500, "inserção é tudo ou nada (FeO não entra, nada muda)");
        // Um segundo disco sem partição recebe o resto
        drive.disks().set(1, ItemResource.of(ModItems.DISK_16K.get()), 1);
        network = ElementNetwork.scan(level, drivePos);
        check(failures, network.insert(new Composition(java.util.Map.of(ChemicalElement.OXYGEN, 5L, ChemicalElement.IRON, 5L)), 1)
                && ElementDiskItem.contents(drive.getDisk(0)).get(ChemicalElement.IRON) == 505
                && ElementDiskItem.contents(drive.getDisk(1)).get(ChemicalElement.OXYGEN) == 5,
                "Fe vai para o disco particionado, O para o disco livre");
        level.setBlock(drivePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }
}
