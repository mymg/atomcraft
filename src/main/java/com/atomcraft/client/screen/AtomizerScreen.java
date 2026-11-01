package com.atomcraft.client.screen;

import com.atomcraft.menu.AtomizerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Atomizador: energia à esquerda, buffer 7×3 no centro, progresso à direita e armazenamento da rede. */
public class AtomizerScreen extends MachineScreen<AtomizerMenu> {

    public AtomizerScreen(AtomizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AtomizerMenu.WIDTH, AtomizerMenu.HEIGHT);
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 22, topPos + 21, 133, 60);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float energy = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + 9, topPos + 21, 11, 60, energy, TechGui.ENERGY);
        TechGui.verticalBar(graphics, leftPos + 158, topPos + 21, 11, 60, menu.progress(), TechGui.PROGRESS);
        storageRow(graphics, 9, 85, 169);
        graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 100), leftPos + 9, topPos + 96,
                statusColor(menu.status()), false);
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hovering(mouseX, mouseY, 9, 21, 11, 60)) {
            tooltip(graphics, mouseX, mouseY, energyTooltip(menu.energy(), menu.capacity()));
        } else if (hovering(mouseX, mouseY, 158, 21, 11, 60)) {
            tooltip(graphics, mouseX, mouseY, menu.status().label(),
                    Component.literal(Math.round(menu.progress() * 100) + "%"));
        } else if (hovering(mouseX, mouseY, 9, 85, 160, 20)) {
            tooltip(graphics, mouseX, mouseY, storageTooltip());
        }
    }
}
