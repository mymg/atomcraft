package com.atomcraft;

import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.CompositionManager;
import com.atomcraft.command.AtomCraftCommand;
import com.atomcraft.network.ModNetwork;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModCapabilities;
import com.atomcraft.registry.ModComponents;
import com.atomcraft.registry.ModCreativeTab;
import com.atomcraft.registry.ModItems;
import com.atomcraft.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

@Mod(AtomCraft.MOD_ID)
public class AtomCraft {

    public static final String MOD_ID = "atomcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public AtomCraft(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModNetwork::register);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.addListener(AtomCraft::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(AtomCraft::onServerStarted);
        NeoForge.EVENT_BUS.addListener(AtomCraft::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(AtomCraft::registerCommands);
    }

    private static void addReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(id("compositions"), new CompositionManager());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        AtomicValues.recompute(event.getServer());
        SelfTest.runIfRequested(event.getServer());
    }

    /** Disparado quando um jogador entra (player != null) e após /reload (player == null). */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) AtomicValues.recompute(event.getPlayerList().getServer());
        event.getRelevantPlayers().forEach(ModNetwork::syncAll);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        AtomCraftCommand.register(event.getDispatcher());
    }
}
