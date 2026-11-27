package com.atomcraft;

import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.atomcraft.block.entity.AtomizerBlockEntity;
import com.atomcraft.block.entity.CombustionGeneratorBlockEntity;
import com.atomcraft.block.entity.DriveBlockEntity;
import com.atomcraft.block.entity.ReplicatorBlockEntity;
import com.atomcraft.block.entity.SynthesizerBlockEntity;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.network.ModNetwork;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModItems;
import com.atomcraft.research.ResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Modo vitrine (só no CI, com ATOMCRAFT_SHOWCASE definido): monta as máquinas
 * com dados de exemplo ao lado do jogador e abre cada interface em sequência,
 * para o cliente tirar prints. Não roda em jogos normais.
 */
final class Showcase {

    private static @Nullable ServerPlayer player;
    private static BlockPos base = BlockPos.ZERO;
    private static int ticks;

    private Showcase() {}

    static void register() {
        if (System.getenv("ATOMCRAFT_SHOWCASE") == null) return;
        NeoForge.EVENT_BUS.addListener(Showcase::onLogin);
        NeoForge.EVENT_BUS.addListener(Showcase::onTick);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        player = serverPlayer;
        ticks = 0;
        setup(serverPlayer);
    }

    /** Onde o jogador começa: ele volta para cá antes de cada interface (slimes do mundo plano empurram). */
    private static net.minecraft.world.phys.Vec3 start = net.minecraft.world.phys.Vec3.ZERO;

    private static void setup(ServerPlayer player) {
        ServerLevel level = player.level();
        base = player.blockPosition().offset(3, 0, 0);
        start = player.position();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
        level.setBlock(base, ModBlocks.DRIVE.get().defaultBlockState().setValue(com.atomcraft.block.MachineBlock.FACING,
                net.minecraft.core.Direction.WEST), Block.UPDATE_ALL);
        com.atomcraft.block.TallMachineBlock.place(level, base.east(), ModBlocks.ATOMIZER.get().defaultBlockState());
        com.atomcraft.block.TallMachineBlock.place(level, base.west(), ModBlocks.REPLICATOR.get().defaultBlockState());
        level.setBlock(base.north(), ModBlocks.CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        com.atomcraft.block.TallMachineBlock.place(level, base.north(2), ModBlocks.ANALYZER.get().defaultBlockState().setValue(com.atomcraft.block.MachineBlock.FACING,
                net.minecraft.core.Direction.WEST));

        // Itens no inventário, para os prints mostrarem os ícones (Analisador, geradores, discos).
        player.getInventory().clearContent();
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModBlocks.ANALYZER.get(), 3));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModBlocks.ATOMIZER.get()));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModBlocks.REPLICATOR.get()));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModBlocks.COMBUSTION_GENERATOR.get()));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModBlocks.CARBON_GENERATOR.get()));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.DISK_64K.get()));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModBlocks.ALLOY_FORGE.get()));
        for (var material : List.of(com.atomcraft.material.Material.STEEL, com.atomcraft.material.Material.BRONZE,
                com.atomcraft.material.Material.STAINLESS_STEEL, com.atomcraft.material.Material.TITANIUM_ALLOY,
                com.atomcraft.material.Material.SUPERALLOY)) {
            player.getInventory().add(new net.minecraft.world.item.ItemStack(com.atomcraft.material.ModMaterials.INGOTS.get(material).get(), 16));
            player.getInventory().add(new net.minecraft.world.item.ItemStack(com.atomcraft.material.ModMaterials.BLOCKS.get(material).get(), 8));
        }
        for (var ore : com.atomcraft.material.OreType.values()) {
            player.getInventory().add(new net.minecraft.world.item.ItemStack(com.atomcraft.material.ModMaterials.ORES.get(ore).get(), 4));
        }
        player.getInventory().add(new net.minecraft.world.item.ItemStack(com.atomcraft.material.ModMaterials.CIRCUIT_BOARD.get(), 4));

        DriveBlockEntity drive = (DriveBlockEntity) level.getBlockEntity(base);
        drive.disks().set(0, ItemResource.of(ModItems.DISK_1M.get()), 1);
        drive.disks().set(1, ItemResource.of(ModItems.DISK_256K.get()), 1);
        drive.disks().set(2, ItemResource.of(ModItems.DISK_64K.get()), 1);
        drive.disks().set(3, ItemResource.of(ModItems.DISK_16K.get()), 1);
        Map<ChemicalElement, Long> contents = new EnumMap<>(ChemicalElement.class);
        contents.put(ChemicalElement.IRON, 16_203L);
        contents.put(ChemicalElement.CARBON, 4_608L);
        contents.put(ChemicalElement.OXYGEN, 882L);
        contents.put(ChemicalElement.SILICON, 310L);
        contents.put(ChemicalElement.HYDROGEN, 60_000L);
        contents.put(ChemicalElement.GOLD, 81L);
        contents.put(ChemicalElement.REDSTONIUM, 160L);
        contents.put(ChemicalElement.CALCIUM, 12L);
        contents.put(ChemicalElement.NITROGEN, 40L);
        contents.put(ChemicalElement.ENDERIUM, 8L);
        contents.put(ChemicalElement.COPPER, 200L);
        contents.put(ChemicalElement.LUMINIUM, 64L);
        contents.put(ChemicalElement.SULFUR, 30L);
        contents.put(ChemicalElement.TIN, 450L);
        contents.put(ChemicalElement.SILVER, 90L);
        contents.put(ChemicalElement.URANIUM, 27L);
        contents.put(ChemicalElement.ALLTHEMODIUM, 9L);
        drive.setDiskContents(0, new Composition(contents));

        AtomizerBlockEntity atomizer = (AtomizerBlockEntity) level.getBlockEntity(base.east());
        atomizer.setOwner(player);
        atomizer.energy().set(140_000);
        atomizer.buffer().set(0, ItemResource.of(Items.IRON_INGOT), 32);
        atomizer.buffer().set(1, ItemResource.of(Items.COBBLESTONE), 64);
        atomizer.buffer().set(2, ItemResource.of(Items.OAK_LOG), 16);
        atomizer.buffer().set(3, ItemResource.of(Items.DIAMOND), 2);
        atomizer.buffer().set(7, ItemResource.of(Items.APPLE), 12);
        atomizer.fluidInput().setStack(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 6_000));

        AnalyzerBlockEntity analyzer = (AnalyzerBlockEntity) level.getBlockEntity(base.north(2));
        analyzer.setOwner(player);
        ResearchData research = ResearchData.get(level.getServer());
        research.addProgress(player.getUUID(), Items.STONE, 27);
        analyzer.buffer().set(0, ItemResource.of(Items.STONE), 23);
        analyzer.buffer().set(1, ItemResource.of(Items.GOLD_INGOT), 6);
        analyzer.buffer().set(2, ItemResource.of(Items.EMERALD), 50); // continua analisando no print do mundo

        for (Item item : List.of(Items.IRON_INGOT, Items.IRON_BLOCK, Items.IRON_NUGGET, Items.DIAMOND, Items.GOLD_INGOT,
                Items.APPLE, Items.STICK, Items.TORCH, Items.GLASS, Items.BUCKET, Items.CAKE, Items.OAK_PLANKS,
                Items.REDSTONE, Items.WATER_BUCKET, Items.SUGAR, Items.IRON_PICKAXE, Items.ENDER_PEARL, Items.BREAD)) {
            if (AtomicValues.get(item, false) != null) research.unlock(player.getUUID(), item);
        }
        ModNetwork.syncResearch(player);

        ReplicatorBlockEntity replicator = (ReplicatorBlockEntity) level.getBlockEntity(base.west());
        replicator.energy().set(1_600_000);
        replicator.startJob(player.getUUID(), Items.IRON_INGOT, 14);
        // Geradores e terminal, todos ligados à rede.
        level.setBlock(base.south(), ModBlocks.CARBON_GENERATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.south(2), ModBlocks.COMBUSTION_GENERATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        // Reator de Fusão multibloco a leste, ligado à rede por cabos até uma Porta do Reator
        reactor = base.offset(3, 2, 0);
        net.minecraft.core.Direction facing = net.minecraft.core.Direction.WEST;
        BlockPos port = com.atomcraft.fusion.FusionStructure.toWorld(reactor, facing, 3, 0, 0);
        SelfTest.buildReactor(level, reactor, facing, java.util.Set.of(port), null);
        for (int dx = 2; dx <= 2; dx++) {
            level.setBlock(base.offset(dx, 0, 0), ModBlocks.CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(reactor) instanceof com.atomcraft.block.entity.FusionReactorBlockEntity fusion) {
            fusion.setOwner(player);
            var modules = fusion.upgrades();
            modules.set(0, ItemResource.of(ModItems.MODULES.get(com.atomcraft.fusion.ModuleType.CONTAINMENT).get(2).get()), 1);
            modules.set(1, ItemResource.of(ModItems.MODULES.get(com.atomcraft.fusion.ModuleType.IGNITION).get(2).get()), 1);
            modules.set(2, ItemResource.of(ModItems.MODULES.get(com.atomcraft.fusion.ModuleType.INJECTOR).get(0).get()), 1);
        }
        level.setBlock(base.south(3), ModBlocks.TERMINAL.get().defaultBlockState(), Block.UPDATE_ALL);
        CombustionGeneratorBlockEntity combustion = (CombustionGeneratorBlockEntity) level.getBlockEntity(base.south(2));
        combustion.fuel().set(0, ItemResource.of(Items.COAL), 12);
        combustion.upgrades().set(0, ItemResource.of(ModItems.EFFICIENCY_UPGRADE.get()), 2);

        // Chips no atomizador, Sintetizador com schematics fabricando um chip
        atomizer.upgrades().set(0, ItemResource.of(ModItems.SPEED_UPGRADE.get()), 3);
        atomizer.upgrades().set(1, ItemResource.of(ModItems.EFFICIENCY_UPGRADE.get()), 1);
        level.setBlock(base.south(2).west(), ModBlocks.SYNTHESIZER.get().defaultBlockState(), Block.UPDATE_ALL);
        SynthesizerBlockEntity synthesizer = (SynthesizerBlockEntity) level.getBlockEntity(base.south(2).west());
        synthesizer.setOwner(player);
        synthesizer.energy().set(150_000);
        synthesizer.library().set(0, ItemResource.of(ModItems.SPEED_UPGRADE_SCHEMATIC.get()), 1);
        synthesizer.library().set(1, ItemResource.of(ModItems.EFFICIENCY_UPGRADE_SCHEMATIC.get()), 1);
        synthesizer.output().set(0, ItemResource.of(ModItems.SPEED_UPGRADE.get()), 2);
        synthesizer.select(ModItems.SPEED_UPGRADE.get());

        // Forja de Ligas fazendo aço, com alguns materiais de exemplo no inventário
        // Replicador Quântico montado (3×3×3), a oeste
        BlockPos quantum = base.offset(-6, 0, 3);
        level.setBlock(quantum, ModBlocks.QUANTUM_REPLICATOR.get().defaultBlockState()
                .setValue(com.atomcraft.block.MachineBlock.FACING, net.minecraft.core.Direction.WEST), Block.UPDATE_ALL);
        for (var slot : com.atomcraft.replication.QuantumStructure.blueprint(quantum, net.minecraft.core.Direction.WEST)) {
            var part = switch (slot.part()) {
                case CASING -> ModBlocks.QUANTUM_CASING.get();
                case GLASS -> ModBlocks.QUANTUM_GLASS.get();
                case CORE -> ModBlocks.QUANTUM_CORE.get();
                case CONTROLLER, PORT -> null;
            };
            if (part != null) level.setBlock(slot.pos(), part.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(quantum) instanceof ReplicatorBlockEntity quantumBe) {
            quantumBe.setOwner(player);
            quantumBe.energy().set(1_500_000);
            quantumBe.checkStructure();
        }

        // Célula de Energia Avançada ligada à rede (os geradores a enchem)
        level.setBlock(base.south(5), ModBlocks.ADVANCED_ENERGY_CELL.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(base.south(5)) instanceof com.atomcraft.block.entity.EnergyCellBlockEntity cell) {
            cell.setOwner(player);
            cell.energy().set(5_200_000);
            cell.setSide(com.atomcraft.energy.CellSide.TOP, com.atomcraft.energy.SideMode.BOTH);
            cell.setSide(com.atomcraft.energy.CellSide.BOTTOM, com.atomcraft.energy.SideMode.NONE);
        }
        // Matriz de Energia montada (3×5×3), a noroeste
        BlockPos matrixPos = base.offset(-6, 1, -3);
        level.setBlock(matrixPos, ModBlocks.ENERGY_MATRIX.get().defaultBlockState()
                .setValue(com.atomcraft.block.MachineBlock.FACING, net.minecraft.core.Direction.WEST), Block.UPDATE_ALL);
        BlockPos portIn = com.atomcraft.energy.MatrixStructure.toWorld(matrixPos, net.minecraft.core.Direction.WEST, 0, 0, 1);
        BlockPos portOut = com.atomcraft.energy.MatrixStructure.toWorld(matrixPos, net.minecraft.core.Direction.WEST, 2, 0, 1);
        for (var slot : com.atomcraft.energy.MatrixStructure.blueprint(matrixPos, net.minecraft.core.Direction.WEST)) {
            var part = switch (slot.part()) {
                case CASING -> ModBlocks.MATRIX_CASING.get();
                case GLASS -> ModBlocks.MATRIX_GLASS.get();
                case CORE -> ModBlocks.MATRIX_CORE.get();
                default -> null;
            };
            if (slot.pos().equals(portIn) || slot.pos().equals(portOut)) part = ModBlocks.MATRIX_PORT.get();
            if (part != null) level.setBlock(slot.pos(), part.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(portOut, level.getBlockState(portOut).setValue(com.atomcraft.block.MatrixPortBlock.MODE, com.atomcraft.energy.PortMode.OUTPUT), Block.UPDATE_ALL);
        level.setBlock(portIn, level.getBlockState(portIn).setValue(com.atomcraft.block.MatrixPortBlock.MODE, com.atomcraft.energy.PortMode.INPUT), Block.UPDATE_ALL);
        if (level.getBlockEntity(matrixPos) instanceof com.atomcraft.block.entity.EnergyCellBlockEntity matrix) {
            matrix.setOwner(player);
            matrix.energy().set(312_000_000);
            matrix.checkStructure();
        }
        // Segundo Replicador Quântico, só começado: a prévia mostra os blocos fantasma do que falta
        BlockPos quantum2 = base.offset(-6, 0, 0);
        level.setBlock(quantum2, ModBlocks.QUANTUM_REPLICATOR.get().defaultBlockState()
                .setValue(com.atomcraft.block.MachineBlock.FACING, net.minecraft.core.Direction.WEST), Block.UPDATE_ALL);
        int placed = 0;
        for (var slot : com.atomcraft.replication.QuantumStructure.blueprint(quantum2, net.minecraft.core.Direction.WEST)) {
            if (slot.part() != com.atomcraft.replication.QuantumPart.CASING || slot.pos().getY() != quantum2.getY() || placed++ >= 5) continue;
            level.setBlock(slot.pos(), ModBlocks.QUANTUM_CASING.get().defaultBlockState(), Block.UPDATE_ALL);
        }

        // Transmissor Sem Fio na rede, com um cartão vinculado e o Desintegrador no inventário
        level.setBlock(base.south(6), com.atomcraft.tools.ModTools.WIRELESS_TRANSMITTER.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(base.south(6)) instanceof com.atomcraft.block.entity.WirelessTransmitterBlockEntity transmitter) {
            transmitter.setOwner(player);
            transmitter.energy().set(640_000);
            // Cartões no transmissor: o do jogador (vinculado) e um avançado ainda sem dono
            var card = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.NETWORK_CARD.get());
            card.set(com.atomcraft.registry.ModComponents.CARD_OWNER.get(), new com.atomcraft.wireless.CardOwner(player.getUUID(), player.getName().getString()));
            transmitter.cards().set(0, ItemResource.of(card), 1);
            transmitter.cards().set(1, ItemResource.of(new net.minecraft.world.item.ItemStack(
                    com.atomcraft.tools.ModTools.CARDS.get(com.atomcraft.wireless.CardTier.ADVANCED).get())), 1);
        }
        var disintegrator = new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.DISINTEGRATOR.get());
        com.atomcraft.tools.AtomicDisintegratorItem.setEnergy(disintegrator, 60_000);
        player.getInventory().add(disintegrator);
        for (var tier : com.atomcraft.tools.ModTools.Tier.values()) {
            player.getInventory().add(new net.minecraft.world.item.ItemStack(com.atomcraft.tools.ModTools.PICKAXES.get(tier).get()));
        }
        for (var armor : com.atomcraft.tools.ModTools.ARMOR) player.getInventory().add(new net.minecraft.world.item.ItemStack(armor.get()));
        // Bancada de Módulos; o Anel Analisador no inventário mostra a composição do bloco mirado nos prints do mundo
        level.setBlock(base.south(7), com.atomcraft.equipment.ModEquipment.MODULE_BENCH.get().defaultBlockState(), Block.UPDATE_ALL);
        // Carregador de Chunks com algumas chunks escolhidas
        level.setBlock(base.south(8), ModBlocks.CHUNK_LOADER.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(base.south(8)) instanceof com.atomcraft.block.entity.ChunkLoaderBlockEntity loader) {
            loader.setOwner(player);
            loader.energy().set(400_000);
            for (int cell : new int[]{7, 11, 13, 17, 6}) loader.toggle(cell);
        }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.ANALYZER_GLASSES.get()));
        // Bomba (interface) e uma fileira de tanques ligados por canos (print do mundo)
        level.setBlock(base.south(9), com.atomcraft.fluid.ModFluids.PUMP.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(base.south(9)) instanceof com.atomcraft.fluid.PumpBlockEntity pump) {
            pump.setOwner(player);
            pump.energy().set(64_000);
            pump.tank().setStack(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 9_000));
        }
        fluidDisplay(level, base.offset(0, 0, 14));
        armorDisplay(level, base.offset(0, 0, 24));

        level.setBlock(base.south(4), ModBlocks.ALLOY_FORGE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(base.south(4)) instanceof com.atomcraft.block.entity.AlloyForgeBlockEntity forge) {
            forge.setOwner(player);
            forge.energy().set(150_000);
            forge.network().insert(new com.atomcraft.chemistry.Composition(java.util.Map.of(
                    com.atomcraft.chemistry.ChemicalElement.IRON, 4000L, com.atomcraft.chemistry.ChemicalElement.CARBON, 500L,
                    com.atomcraft.chemistry.ChemicalElement.COPPER, 800L, com.atomcraft.chemistry.ChemicalElement.TIN, 200L)), 1);
            forge.select(com.atomcraft.material.ModMaterials.INGOTS.get(com.atomcraft.material.Material.STEEL).get());
        }

        // Gerador de Carbono desligado, para mostrar o botão vermelho
        if (level.getBlockEntity(base.south()) instanceof com.atomcraft.block.entity.MachineBlockEntity carbon) {
            carbon.setOwner(player);
            carbon.setEnabled(false);
        }
        AtomCraft.LOGGER.info("AtomCraft SHOWCASE pronto em {}", base);
    }

    private static BlockPos reactor = BlockPos.ZERO;

    /** Suportes de armadura: Armadura Nano Desgastada completa e o Jetpack (modelos do GeckoLib). */
    private static void armorDisplay(ServerLevel level, BlockPos origin) {
        Object[][] stands = {
                {origin.west(), new net.minecraft.world.item.Item[]{com.atomcraft.equipment.ModEquipment.NANO_HELMET.get(),
                        com.atomcraft.equipment.ModEquipment.NANO_CHESTPLATE.get(), com.atomcraft.equipment.ModEquipment.NANO_LEGGINGS.get(),
                        com.atomcraft.equipment.ModEquipment.NANO_BOOTS.get()}},
                {origin.east(), new net.minecraft.world.item.Item[]{com.atomcraft.equipment.ModEquipment.ANALYZER_GLASSES.get(),
                        com.atomcraft.equipment.ModEquipment.JETPACK.get(), com.atomcraft.equipment.ModEquipment.EXO_LEGGINGS.get(),
                        com.atomcraft.equipment.ModEquipment.EXO_BOOTS.get()}}};
        var order = new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        for (Object[] entry : stands) {
            BlockPos pos = (BlockPos) entry[0];
            var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            stand.setYRot(180f);
            stand.setYBodyRot(180f);
            stand.setYHeadRot(180f);
            var items = (net.minecraft.world.item.Item[]) entry[1];
            for (int i = 0; i < order.length; i++) stand.setItemSlot(order[i], new net.minecraft.world.item.ItemStack(items[i]));
            level.addFreshEntity(stand);
        }
        // Minério na mira: com os Óculos Analisadores, o Jade mostra a fórmula
        level.setBlock(origin.offset(0, 0, -2), net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Quatro tanques (um de cada nível) com água e lava, ligados por canos; um lado em modo "puxar". */
    private static void fluidDisplay(ServerLevel level, BlockPos origin) {
        var tiers = com.atomcraft.fluid.FluidTier.values();
        float[] fill = {0.6f, 0.4f, 0.9f, 0.75f};
        for (int i = 0; i < tiers.length; i++) {
            BlockPos tankPos = origin.offset(-3 + i * 2, 0, 0);
            level.setBlock(tankPos, com.atomcraft.fluid.ModFluids.TANKS.get(tiers[i]).get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(tankPos) instanceof com.atomcraft.fluid.TankBlockEntity tank) {
                var fluid = i % 2 == 0 ? net.minecraft.world.level.material.Fluids.WATER : net.minecraft.world.level.material.Fluids.LAVA;
                tank.tank().setStack(new net.neoforged.neoforge.fluids.FluidStack(fluid, Math.round(tiers[i].tankCapacity() * fill[i])));
            }
            if (i < tiers.length - 1) {
                BlockPos pipePos = tankPos.east();
                level.setBlock(pipePos, com.atomcraft.fluid.ModFluids.PIPES.get(tiers[i]).get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        // Mostra o aro do modo "puxar" no primeiro cano (do lado do tanque de água para o de lava: não mistura)
        BlockPos firstPipe = origin.offset(-2, 0, 0);
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(firstPipe);
        if (state.getBlock() instanceof com.atomcraft.fluid.PipeBlock) {
            level.setBlock(firstPipe, state.setValue(com.atomcraft.fluid.PipeBlock.PROPERTIES.get(net.minecraft.core.Direction.WEST),
                    com.atomcraft.fluid.PipeConnection.PULL), Block.UPDATE_ALL);
        }
    }

    /** Câmera a sudoeste e acima, apontada (yaw/pitch calculados) para o meio das máquinas de 2 blocos. */
    private static void machinesView(ServerPlayer player) {
        double x = base.getX() - 3.0, y = base.getY() + 2.0, z = base.getZ() + 4.5;
        double tx = base.getX() + 0.5, ty = base.getY() + 1.0, tz = base.getZ() - 0.5;
        double dx = tx - x, dy = ty - (y + player.getEyeHeight()), dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        player.connection.teleport(x, y, z, yaw, pitch);
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (player == null || player.hasDisconnected()) return;
        ticks++;
        ServerLevel level = player.level();
        BlockPos target = switch (ticks) {
            case 120 -> base.east();
            case 320 -> base.north(2);
            case 520 -> base;
            case 720 -> base.west();
            case 920 -> base.south(2);
            case 1120 -> base.south();
            case 1320 -> reactor;
            case 1520 -> base.south(3);
            case 1720 -> base.south(2).west();
            case 1920 -> base.south(4);
            case 2120 -> base.offset(-6, 0, 3);
            case 2320 -> base.south(5);
            case 2520 -> base.offset(-6, 1, -3);
            case 2720 -> base.south(6);
            case 3120 -> base.south(8);
            case 3240 -> base.south(9);
            default -> null;
        };
        if (target != null) {
            if (ticks == 3120 || ticks == 3240) {
                // O carregador fica longe do ponto inicial: o menu fecharia por distância.
                player.connection.teleport(target.getX() - 1.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
            } else {
                player.connection.teleport(start.x, start.y, start.z, player.getYRot(), player.getXRot());
            }
            if (level.getBlockEntity(target) instanceof MenuProvider provider) player.openMenu(provider);
            AtomCraft.LOGGER.info("AtomCraft SHOWCASE tick {}: abrindo {} ({})", ticks, target, level.getBlockState(target).getBlock());
        }
        if (ticks == 2920) {
            player.connection.teleport(start.x, start.y, start.z, player.getYRot(), player.getXRot());
            // Bancada com o peitoral do exoesqueleto (escudo + blindagem II)
            // Armadura vestida (aparece na coluna da esquerda da bancada)
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.EXO_HELMET.get()));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.EXO_LEGGINGS.get()));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.EXO_BOOTS.get()));
            if (level.getBlockEntity(base.south(7)) instanceof com.atomcraft.block.entity.ModuleBenchBlockEntity bench) {
                bench.energy().set(320_000);
                player.openMenu(bench);
            }
            if (player.containerMenu instanceof com.atomcraft.menu.ModuleBenchMenu bench) {
                var chest = new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.EXO_CHESTPLATE.get());
                com.atomcraft.equipment.Battery.set(chest, 180_000, com.atomcraft.equipment.ExoArmorItem.CAPACITY);
                var modules = com.atomcraft.equipment.ModEquipment.MODULES;
                com.atomcraft.equipment.ModularItem.setModules(chest, List.of(
                        new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.SHIELD).get()),
                        new net.minecraft.world.item.ItemStack(modules.get(com.atomcraft.equipment.EquipmentModule.PLATING).get(), 2),
                        net.minecraft.world.item.ItemStack.EMPTY));
                bench.insertEquipment(chest);
            }
        }
        if (ticks == 3360) {
            // Visão das máquinas de 2 blocos (Replicador, Drive, Atomizador e Analisador animados)
            player.closeContainer();
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            machinesView(player);
            // Itens novos, para o Analisador estar trabalhando (item girando na câmara) no print.
            if (player.level().getBlockEntity(base.north(2)) instanceof AnalyzerBlockEntity analyzer) {
                int slot = 3;
                for (Item item : List.of(Items.COBBLESTONE, Items.DIRT, Items.SAND, Items.GRAVEL)) {
                    analyzer.buffer().set(slot++, ItemResource.of(item), 64);
                }
            }
        }
        if (ticks > 3360 && ticks < 3700 && ticks % 20 == 0) {
            // Mantém a câmera no lugar (mobs/slimes podem empurrar o jogador)
            machinesView(player);
        }
        if (ticks == 3700) {
            // Visão do reator formado no mundo
            player.connection.teleport(base.getX() - 3.5, base.getY() + 4, base.getZ() + 0.5, -90f, 18f);
        }
        if (ticks == 3900) player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, net.minecraft.world.item.ItemStack.EMPTY);
        if (ticks >= 3900 && ticks < 4300 && ticks % 20 == 0) {
            // Visão das estruturas: Matriz formada (modelo), Replicador Quântico e um segundo quântico com a prévia
            player.connection.teleport(base.getX() - 13.5, base.getY() + 3.5, base.getZ() + 0.5, -90f, 12f);
        }
        if (ticks >= 4300 && ticks < 4520 && ticks % 20 == 0) {
            // Tanques (um de cada nível) com água e lava, ligados por canos
            player.connection.teleport(base.getX() + 0.5, base.getY() + 2.2, base.getZ() + 9.5, 0f, 22f);
        }
        if (ticks >= 4520 && ticks < 4740 && ticks % 20 == 0) {
            // Armadura Nano Desgastada e Jetpack em suportes de armadura
            player.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 20.6, 0f, 24f);
            if (ticks == 4520) {
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                        new net.minecraft.world.item.ItemStack(com.atomcraft.equipment.ModEquipment.ANALYZER_GLASSES.get()));
            }
        }
        if (ticks == 5600) AtomCraft.LOGGER.info("AtomCraft SHOWCASE DONE");
    }
}
