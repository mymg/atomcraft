package com.atomcraft.client.screen;

import com.atomcraft.client.ClientState;
import com.atomcraft.menu.AnalyzerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AnalyzerScreen extends MachineScreen<AnalyzerMenu> {

    public AnalyzerScreen(AnalyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AnalyzerMenu.WIDTH, AnalyzerMenu.HEIGHT);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiUtil.horizontalBar(graphics, leftPos + 8, topPos + 42, 160, 5, menu.progress(), 0xFF9C27B0);
        Item item = menu.currentItem();
        GuiUtil.slot(graphics, leftPos + 8, topPos + 52);
        if (item != null) {
            graphics.item(new ItemStack(item), leftPos + 8, topPos + 52);
            int required = Math.max(1, menu.required());
            GuiUtil.horizontalBar(graphics, leftPos + 30, topPos + 63, 138, 5, (float) menu.researched() / required, 0xFF4CAF50);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Item item = menu.currentItem();
        if (item != null) {
            Component name = new ItemStack(item).getHoverName();
            graphics.text(font, font.plainSubstrByWidth(name.getString(), 100), 30, 53, GuiUtil.TEXT, false);
            String count = menu.researched() + "/" + menu.required();
            graphics.text(font, count, 168 - font.width(count), 53, GuiUtil.TEXT, false);
        }
        graphics.text(font, menu.status().label(), 8, 72, menu.status().color(), false);
        long researched = ClientState.research().keySet().stream().filter(ClientState::isResearched).count();
        Component total = Component.translatable("gui.atomcraft.researched_total", researched);
        graphics.text(font, total, 168 - font.width(total), 72, GuiUtil.TEXT_LIGHT, false);
    }
}
