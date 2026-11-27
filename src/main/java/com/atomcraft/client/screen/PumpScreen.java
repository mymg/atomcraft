package com.atomcraft.client.screen;

import com.atomcraft.fluid.PumpBlockEntity;
import com.atomcraft.menu.PumpMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Bomba: energia à esquerda, o fluido guardado no meio e o progresso do próximo balde à direita. */
public class PumpScreen extends MachineScreen<PumpMenu> {

    private static final int TANK_X = 30, TANK_Y = 21, TANK_W = 40, TANK_H = 46;

    public PumpScreen(PumpMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PumpMenu.WIDTH, PumpMenu.HEIGHT);
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + TANK_X - 3, topPos + TANK_Y - 3, TANK_W + 6, TANK_H + 6);
        TechGui.glass(graphics, leftPos + 78, topPos + 18, 76, 52);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float energy = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + 9, topPos + 21, 11, 46, energy, TechGui.ENERGY);
        FluidGauge.vertical(graphics, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H, menu.fluid(), menu.amount(), PumpBlockEntity.CAPACITY);
        TechGui.verticalBar(graphics, leftPos + 158, topPos + 21, 11, 46, menu.progress(), TechGui.PROGRESS);

        int x = leftPos + 82, y = topPos + 21;
        TechGui.smallText(graphics, font, Component.translatable("gui.atomcraft.fluid_label").getString(), x, y, TechGui.TEXT_DIM, false);
        Component name = menu.fluid() == Fluids.EMPTY || menu.amount() <= 0 ? Component.translatable("gui.atomcraft.fluid_empty")
                : new FluidStack(menu.fluid(), menu.amount()).getHoverName();
        graphics.text(font, font.plainSubstrByWidth(name.getString(), 70), x, y + 8, TechGui.TEXT_LIGHT, false);
        TechGui.smallText(graphics, font, Component.translatable("gui.atomcraft.stored").getString(), x, y + 20, TechGui.TEXT_DIM, false);
        graphics.text(font, GuiUtil.formatAmount(menu.amount()) + " mB", x, y + 28, TechGui.TEXT_LIGHT, false);
        graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 70), x, y + 39, statusColor(menu.status()), false);
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hovering(mouseX, mouseY, 9, 21, 11, 46)) {
            tooltip(graphics, mouseX, mouseY, energyTooltip(menu.energy(), menu.capacity()));
        } else if (hovering(mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H)) {
            tooltip(graphics, mouseX, mouseY, FluidGauge.tooltip(menu.fluid(), menu.amount(), PumpBlockEntity.CAPACITY).toArray(Component[]::new));
        } else if (hovering(mouseX, mouseY, 158, 21, 11, 46)) {
            tooltip(graphics, mouseX, mouseY, menu.status().label(), Component.literal(Math.round(menu.progress() * 100) + "%"));
        }
    }
}
