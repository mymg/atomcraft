package com.atomcraft.client.screen;

import com.atomcraft.menu.AtomizerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AtomizerScreen extends MachineScreen<AtomizerMenu> {

    public AtomizerScreen(AtomizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AtomizerMenu.WIDTH, AtomizerMenu.HEIGHT);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiUtil.horizontalBar(graphics, leftPos + 8, topPos + 60, 160, 6, menu.progress(), 0xFF4CAF50);
        networkBar(graphics, leftPos + 8, topPos + 82, 160);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, menu.status().label(), 8, 69, menu.status().color(), false);
        Component storage = networkLabel();
        graphics.text(font, storage, 168 - font.width(storage), 69, GuiUtil.TEXT_LIGHT, false);
    }
}
