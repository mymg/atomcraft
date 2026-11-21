package com.atomcraft.client;

import net.minecraft.core.BlockPos;

import com.atomcraft.AtomCraft;
import com.atomcraft.client.screen.AnalyzerScreen;
import com.atomcraft.client.screen.AtomizerScreen;
import com.atomcraft.client.screen.GeneratorScreen;
import com.atomcraft.client.screen.MachineScreen;
import com.atomcraft.client.screen.SynthesizerScreen;
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
    private static int screensSeen;
    private static int idleTicks, jeiTicks;
    private static boolean worldCaptured, machinesCaptured;

    private ClientShowcase() {}

    static void register() {
        if (System.getenv("ATOMCRAFT_SHOWCASE") == null) return;
        NeoForge.EVENT_BUS.addListener(ClientShowcase::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (machinesCaptured && jeiTicks < 320) {
            // Por último, as abas do AtomCraft no JEI (se o JEI estiver instalado)
            jeiTicks++;
            String[] categories = {"alloy_forge", "atomizer", "combustion_fuel", "synthesizer"};
            for (int i = 0; i < categories.length; i++) {
                if (jeiTicks == 20 + i * 70) com.atomcraft.compat.JeiBridge.showCategory.accept(categories[i]);
                if (jeiTicks == 70 + i * 70 && screen != null) AtomCraft.LOGGER.info("AtomCraft SHOWCASE capture jei_{}", categories[i]);
            }
            if (jeiTicks == 310 && screen != null) Minecraft.getInstance().gui.setScreen(null);
            return;
        }
        if (screen == null && screensSeen >= 15 && !machinesCaptured) {
            // Depois das interfaces, o servidor fecha o menu e posiciona a câmera nas máquinas e depois no reator.
            idleTicks++;
            if (idleTicks == 100 && !worldCaptured) {
                worldCaptured = true;
                AtomCraft.LOGGER.info("AtomCraft SHOWCASE capture world_drive_analyzer");
            }
            if (idleTicks == 480) AtomCraft.LOGGER.info("AtomCraft SHOWCASE capture world_fusion_reactor");
            if (idleTicks == 620) {
                // Liga a prévia (blocos fantasma) dos controladores incompletos perto da câmera
                var mc = Minecraft.getInstance();
                BlockPos center = mc.player.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-20, -6, -20), center.offset(20, 6, 20))) {
                    var state = mc.level.getBlockState(pos);
                    if (state.getBlock() instanceof com.atomcraft.block.QuantumReplicatorBlock
                            && !state.getValue(com.atomcraft.block.QuantumReplicatorBlock.FORMED) && !FusionPreview.isShowing(pos)) {
                        FusionPreview.toggle(pos.immutable());
                    }
                }
            }
            if (idleTicks == 720) {
                machinesCaptured = true;
                AtomCraft.LOGGER.info("AtomCraft SHOWCASE capture world_structures");
            }
            return;
        }
        if (!(screen instanceof MachineScreen<?>)) return;
        idleTicks = 0;
        if (screen != current) {
            current = screen;
            ticks = 0;
            screensSeen++;
            AtomCraft.LOGGER.info("AtomCraft SHOWCASE tela {}: {}", screensSeen, screen.getClass().getSimpleName());
            if (screen instanceof ReplicatorScreen replicator) replicator.showcaseSelect(Items.IRON_INGOT);
            // Mostra os painéis laterais em algumas máquinas.
            MachineScreen<?> machine = (MachineScreen<?>) screen;
            boolean fusion = screen instanceof GeneratorScreen && machine.getMenu().getType() == com.atomcraft.registry.ModMenus.FUSION_REACTOR.get();
            machine.showcasePanel(screen instanceof AtomizerScreen || screen instanceof SynthesizerScreen || fusion
                    ? MachineScreen.SidePanel.UPGRADES
                    : screen instanceof AnalyzerScreen ? MachineScreen.SidePanel.INFO
                    : screen instanceof ReplicatorScreen ? MachineScreen.SidePanel.EXTRA : MachineScreen.SidePanel.NONE);
            return;
        }
        if (++ticks == 25) {
            var key = BuiltInRegistries.MENU.getKey(((MachineScreen<?>) screen).getMenu().getType());
            String name = key == null ? "unknown" : key.getPath();
            if (screen instanceof com.atomcraft.client.screen.EnergyCellScreen cellScreen
                    && cellScreen.getMenu().tier() == com.atomcraft.energy.CellTier.MATRIX) {
                name = "energy_matrix";
            }
            if (screen instanceof ReplicatorScreen replicatorScreen
                    && replicatorScreen.getMenu().tier() != com.atomcraft.replication.ReplicatorTier.BASIC) {
                name = replicatorScreen.getMenu().tier().name().toLowerCase() + "_replicator";
            }
            AtomCraft.LOGGER.info("AtomCraft SHOWCASE capture {}", name);
        }
    }
}
