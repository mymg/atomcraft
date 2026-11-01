package com.atomcraft.client;

import com.atomcraft.AtomCraft;
import com.atomcraft.client.screen.MachineScreen;
import com.atomcraft.client.screen.ReplicatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * Lado cliente do modo vitrine do CI: quando uma interface do mod está aberta e
 * estável, avisa no log para o CI capturar a tela virtual.
 */
final class ClientShowcase {

    private static @Nullable Screen current;
    private static int ticks;

    private ClientShowcase() {}

    static void register() {
        if (System.getenv("ATOMCRAFT_SHOWCASE") == null) return;
        NeoForge.EVENT_BUS.addListener(ClientShowcase::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (!(screen instanceof MachineScreen<?>)) return;
        if (screen != current) {
            current = screen;
            ticks = 0;
            if (screen instanceof ReplicatorScreen replicator) replicator.showcaseSelect(Items.IRON_INGOT);
            return;
        }
        if (++ticks == 40) {
            var key = BuiltInRegistries.MENU.getKey(((MachineScreen<?>) screen).getMenu().getType());
            String name = key == null ? "unknown" : key.getPath();
            AtomCraft.LOGGER.info("AtomCraft SHOWCASE capture {}", name);
        }
    }
}
