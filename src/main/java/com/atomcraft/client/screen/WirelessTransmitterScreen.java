package com.atomcraft.client.screen;

import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.menu.WirelessTransmitterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Transmissor Sem Fio: energia guardada para os equipamentos, alcance e a rede (armazenamento). */
public class WirelessTransmitterScreen extends MachineScreen<WirelessTransmitterMenu> {

    private static final int BAR_X = 9, BAR_Y = 21, BAR_W = 14, BAR_H = 64;

    public WirelessTransmitterScreen(WirelessTransmitterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WirelessTransmitterMenu.WIDTH, WirelessTransmitterMenu.HEIGHT);
    }

    @Override
    protected boolean showsInventory() {
        return false;
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 28, topPos + 21, 140, 46);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float fraction = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H, fraction, TechGui.ENERGY);
        int x = leftPos + 32, y = topPos + 25;
        graphics.text(font, GuiUtil.formatAmount(menu.energy()) + " / " + GuiUtil.formatAmount(menu.capacity()) + " FE", x, y,
                TechGui.TEXT_LIGHT, false);
        Component range = menu.range() < 0 ? Component.translatable("gui.atomcraft.range_unlimited")
                : Component.translatable("gui.atomcraft.range", menu.range());
        graphics.text(font, range, x, y + 12, TechGui.TEXT_TEAL, false);
        graphics.text(font, Component.translatable("gui.atomcraft.card_hint_1"), x, y + 23, TechGui.TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.atomcraft.card_hint_2"), x, y + 32, TechGui.TEXT_DIM, false);
        storageRow(graphics, 28, 72, 168);
        MachineStatus status = menu.isEnabled() ? MachineStatus.WORKING : MachineStatus.DISABLED;
        graphics.text(font, status.label(), leftPos + 9, topPos + 95, statusColor(status), false);
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hovering(mouseX, mouseY, BAR_X, BAR_Y, BAR_W, BAR_H)) tooltip(graphics, mouseX, mouseY, energyTooltip(menu.energy(), menu.capacity()));
        else if (hovering(mouseX, mouseY, 28, 72, 140, 20)) tooltip(graphics, mouseX, mouseY, storageTooltip());
    }
}
