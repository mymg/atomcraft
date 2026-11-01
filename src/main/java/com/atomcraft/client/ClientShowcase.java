package com.atomcraft.client;

import com.atomcraft.AtomCraft;
import com.atomcraft.client.screen.MachineScreen;
import com.atomcraft.client.screen.ReplicatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

import java.io.File;

/** Lado cliente do modo vitrine do CI: tira um print de cada interface aberta pelo servidor. */
final class ClientShowcase {

    private static @Nullable Screen current;
    private static int ticks;
    private static @Nullable String pendingName;

    private ClientShowcase() {}

    static void register() {
        if (System.getenv("ATOMCRAFT_SHOWCASE") == null) return;
        NeoForge.EVENT_BUS.addListener(ClientShowcase::onTick);
        NeoForge.EVENT_BUS.addListener(ClientShowcase::onScreenshot);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (!(screen instanceof MachineScreen<?>)) return;
        if (screen != current) {
            current = screen;
            ticks = 0;
            if (screen instanceof ReplicatorScreen replicator) replicator.showcaseSelect(Items.IRON_INGOT);
            return;
        }
        if (++ticks == 40) {
            pendingName = screen.getClass().getSimpleName().replace("Screen", "").toLowerCase() + ".png";
            Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(), message -> {});
            AtomCraft.LOGGER.info("AtomCraft SHOWCASE print {}", pendingName);
        }
    }

    private static void onScreenshot(ScreenshotEvent event) {
        if (pendingName != null) {
            event.setScreenshotFile(new File(event.getScreenshotFile().getParentFile(), pendingName));
            pendingName = null;
        }
    }
}
