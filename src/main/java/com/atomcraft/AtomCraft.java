package com.atomcraft;

import com.atomcraft.chemistry.CompositionManager;
import com.atomcraft.command.AtomCraftCommand;
import com.atomcraft.registry.ModBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(AtomCraft.MOD_ID)
public class AtomCraft {

    public static final String MOD_ID = "atomcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AtomCraft(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        modBus.addListener(this::addCreativeTabItems);

        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.ATOMIZER_ITEM);
            event.accept(ModBlocks.ASSEMBLER_ITEM);
        }
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CompositionManager());
    }

    private void registerCommands(RegisterCommandsEvent event) {
        AtomCraftCommand.register(event.getDispatcher());
    }
}
