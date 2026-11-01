package com.atomcraft;

import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.atomcraft.block.entity.AtomizerBlockEntity;
import com.atomcraft.block.entity.CombustionGeneratorBlockEntity;
import com.atomcraft.block.entity.DriveBlockEntity;
import com.atomcraft.block.entity.ReplicatorBlockEntity;
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
        level.setBlock(base, ModBlocks.DRIVE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.east(), ModBlocks.ATOMIZER.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.west(), ModBlocks.REPLICATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.north(), ModBlocks.CABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.north(2), ModBlocks.ANALYZER.get().defaultBlockState(), Block.UPDATE_ALL);

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
        contents.put(ChemicalElement.HYDROGEN, 640L);
        contents.put(ChemicalElement.GOLD, 81L);
        contents.put(ChemicalElement.REDSTONIUM, 16L);
        contents.put(ChemicalElement.CALCIUM, 12L);
        contents.put(ChemicalElement.NITROGEN, 40L);
        contents.put(ChemicalElement.ENDERIUM, 8L);
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
        level.setBlock(base.above(), ModBlocks.FUSION_REACTOR.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.west().above(), ModBlocks.TERMINAL.get().defaultBlockState(), Block.UPDATE_ALL);
        CombustionGeneratorBlockEntity combustion = (CombustionGeneratorBlockEntity) level.getBlockEntity(base.south(2));
        combustion.fuel().set(0, ItemResource.of(Items.COAL), 12);
        AtomCraft.LOGGER.info("AtomCraft SHOWCASE pronto em {}", base);
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (player == null || player.hasDisconnected()) return;
        ticks++;
        ServerLevel level = player.level();
        BlockPos target = switch (ticks) {
            case 60 -> base.east();
            case 160 -> base.north(2);
            case 260 -> base;
            case 360 -> base.west();
            case 460 -> base.south(2);
            case 560 -> base.south();
            case 760 -> base.above();
            case 860 -> base.west().above();
            default -> null;
        };
        if (target != null && level.getBlockEntity(target) instanceof MenuProvider provider) {
            player.openMenu(provider, target);
        }
        if (ticks == 980) AtomCraft.LOGGER.info("AtomCraft SHOWCASE DONE");
    }
}
