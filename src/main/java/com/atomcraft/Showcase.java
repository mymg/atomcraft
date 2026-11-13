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

    private static void setup(ServerPlayer player) {
        ServerLevel level = player.level();
        base = player.blockPosition().offset(3, 0, 0);
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

        // Gerador de Carbono desligado, para mostrar o botão vermelho
        if (level.getBlockEntity(base.south()) instanceof com.atomcraft.block.entity.MachineBlockEntity carbon) {
            carbon.setOwner(player);
            carbon.setEnabled(false);
        }
        AtomCraft.LOGGER.info("AtomCraft SHOWCASE pronto em {}", base);
    }

    private static BlockPos reactor = BlockPos.ZERO;

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
            default -> null;
        };
        if (target != null && level.getBlockEntity(target) instanceof MenuProvider provider) {
            player.openMenu(provider);
        }
        if (ticks == 1960) {
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
        if (ticks > 1960 && ticks < 2300 && ticks % 20 == 0) {
            // Mantém a câmera no lugar (mobs/slimes podem empurrar o jogador)
            machinesView(player);
        }
        if (ticks == 2300) {
            // Visão do reator formado no mundo
            player.connection.teleport(base.getX() - 3.5, base.getY() + 4, base.getZ() + 0.5, -90f, 18f);
        }
        if (ticks == 2900) AtomCraft.LOGGER.info("AtomCraft SHOWCASE DONE");
    }
}
