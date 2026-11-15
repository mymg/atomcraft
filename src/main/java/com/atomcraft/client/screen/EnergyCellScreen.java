package com.atomcraft.client.screen;

import com.atomcraft.block.MachineBlock;
import com.atomcraft.client.FusionPreview;
import com.atomcraft.energy.CellSide;
import com.atomcraft.energy.CellTier;
import com.atomcraft.energy.MatrixStructure;
import com.atomcraft.energy.SideMode;
import com.atomcraft.menu.EnergyCellMenu;
import com.atomcraft.network.SideConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Célula de Energia: barra de energia, fluxo de entrada/saída e, à direita, os 6 lados da célula
 * (planificados como um cubo) — clique num lado para alternar entrada → saída → ambos → nada.
 * Na Matriz de Energia, o lado direito mostra a estrutura e as portas.
 */
public class EnergyCellScreen extends MachineScreen<EnergyCellMenu> {

    private static final int BAR_X = 9, BAR_Y = 21, BAR_W = 14, BAR_H = 76;
    private static final int TEXT_X = 30, NET_X = 106, NET_Y = 30, CELL = 16;
    private static final int PREVIEW_X = 96, PREVIEW_Y = 82, PREVIEW_W = 72, PREVIEW_H = 13;

    public EnergyCellScreen(EnergyCellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EnergyCellMenu.WIDTH, EnergyCellMenu.HEIGHT);
    }

    @Override
    protected boolean showsInventory() {
        return false;
    }

    /** Posição (coluna, linha) de cada lado no cubo planificado. */
    private static int[] netCell(CellSide side) {
        return switch (side) {
            case TOP -> new int[]{1, 0};
            case LEFT -> new int[]{0, 1};
            case FRONT -> new int[]{1, 1};
            case RIGHT -> new int[]{2, 1};
            case BACK -> new int[]{2, 0};
            case BOTTOM -> new int[]{1, 2};
        };
    }

    private @Nullable CellSide sideAt(double mouseX, double mouseY) {
        if (menu.tier() == CellTier.MATRIX) return null;
        for (CellSide side : CellSide.values()) {
            int[] c = netCell(side);
            int x = leftPos + NET_X + c[0] * CELL, y = topPos + NET_Y + c[1] * CELL;
            if (mouseX >= x && mouseX < x + CELL - 1 && mouseY >= y && mouseY < y + CELL - 1) return side;
        }
        return null;
    }

    private boolean overPreview(double mouseX, double mouseY) {
        return menu.tier() == CellTier.MATRIX && !menu.formed()
                && mouseX >= leftPos + PREVIEW_X && mouseX < leftPos + PREVIEW_X + PREVIEW_W
                && mouseY >= topPos + PREVIEW_Y && mouseY < topPos + PREVIEW_Y + PREVIEW_H;
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + TEXT_X - 4, topPos + 21, 64, 76);
        TechGui.glass(graphics, leftPos + 92, topPos + 21, 78, 76);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float fraction = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H, fraction, TechGui.ENERGY);

        int x = leftPos + TEXT_X, y = topPos + 25;
        graphics.text(font, Component.translatable("gui.atomcraft.stored"), x, y, TechGui.TEXT_DIM, false);
        graphics.text(font, GuiUtil.formatAmount(menu.energy()) + " FE", x, y + 10, TechGui.TEXT_LIGHT, false);
        graphics.text(font, "/ " + GuiUtil.formatAmount(menu.capacity()), x, y + 20, TechGui.TEXT_DIM, false);
        graphics.text(font, Math.round(fraction * 100) + "%", x, y + 30, TechGui.TEXT_TEAL, false);
        graphics.text(font, "▲ " + GuiUtil.formatAmount(menu.input()) + "/t", x, y + 46, SideMode.INPUT.color(), false);
        graphics.text(font, "▼ " + GuiUtil.formatAmount(menu.output()) + "/t", x, y + 57, SideMode.OUTPUT.color(), false);

        if (menu.tier() == CellTier.MATRIX) {
            extractMatrix(graphics, mouseX, mouseY);
        } else {
            graphics.text(font, Component.translatable("gui.atomcraft.sides"), leftPos + 96, topPos + 24, TechGui.TEXT_TEAL, false);
            CellSide hovered = sideAt(mouseX, mouseY);
            for (CellSide side : CellSide.values()) {
                int[] c = netCell(side);
                int sx = leftPos + NET_X + c[0] * CELL, sy = topPos + NET_Y + c[1] * CELL;
                SideMode mode = menu.side(side);
                graphics.fill(sx, sy, sx + CELL - 1, sy + CELL - 1, side == hovered ? 0xFFFFFFFF : 0xFF14181E);
                graphics.fill(sx + 1, sy + 1, sx + CELL - 2, sy + CELL - 2, mode.color());
                String letter = Component.translatable("cell_side.atomcraft." + side.name().toLowerCase() + ".short").getString();
                graphics.text(font, letter, sx + (CELL - 1 - font.width(letter)) / 2 + 1, sy + 4, 0xFFFFFFFF, true);
            }
            graphics.text(font, Component.translatable("gui.atomcraft.sides_hint"), leftPos + 96, topPos + 84, TechGui.TEXT_DIM, false);
        }

        graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 160), leftPos + 9, topPos + 103,
                statusColor(menu.status()), false);
    }

    private void extractMatrix(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + 96, y = topPos + 25;
        if (menu.formed()) {
            graphics.text(font, Component.translatable("gui.atomcraft.structure_ok"), x, y, TechGui.TEXT_GOOD, false);
            graphics.text(font, Component.translatable("gui.atomcraft.ports"), x, y + 14, TechGui.TEXT_DIM, false);
            graphics.text(font, "▲ " + menu.portsIn() + "  ▼ " + menu.portsOut(), x, y + 25, TechGui.TEXT_LIGHT, false);
            graphics.text(font, Component.translatable("gui.atomcraft.port_hint_1"), x, y + 44, TechGui.TEXT_DIM, false);
            graphics.text(font, Component.translatable("gui.atomcraft.port_hint_2"), x, y + 54, TechGui.TEXT_DIM, false);
            return;
        }
        graphics.text(font, Component.translatable("gui.atomcraft.quantum_incomplete"), x, y, TechGui.TEXT_BAD, false);
        var level = Minecraft.getInstance().level;
        var state = level == null ? null : level.getBlockState(menu.pos());
        if (state != null && state.hasProperty(MachineBlock.FACING)) {
            var missing = MatrixStructure.check(level, menu.pos(), state.getValue(MachineBlock.FACING)).missingCounts();
            int line = y + 12;
            for (var entry : missing.entrySet()) {
                String text = entry.getValue() + "× " + Component.translatable(entry.getKey().translationKey()).getString();
                graphics.text(font, font.plainSubstrByWidth(text, 72), x, line, TechGui.TEXT_LIGHT, false);
                line += 10;
            }
        }
        TechGui.button(graphics, font, leftPos + PREVIEW_X, topPos + PREVIEW_Y, PREVIEW_W, PREVIEW_H,
                Component.translatable(FusionPreview.isShowing(menu.pos()) ? "gui.atomcraft.hide_structure" : "gui.atomcraft.show_structure").getString(),
                overPreview(mouseX, mouseY), true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        CellSide side = event.button() == 0 || event.button() == 1 ? sideAt(event.x(), event.y()) : null;
        if (side != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            SideMode mode = menu.side(side);
            // Botão direito volta um passo.
            SideMode next = event.button() == 0 ? mode.next() : mode.next().next().next();
            ClientPacketDistributor.sendToServer(new SideConfigPayload(menu.pos(), side.ordinal(), next.ordinal()));
            return true;
        }
        if (event.button() == 0 && overPreview(event.x(), event.y())) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            FusionPreview.toggle(menu.pos());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        CellSide side = sideAt(mouseX, mouseY);
        if (side != null) {
            tooltip(graphics, mouseX, mouseY, side.label(), menu.side(side).label().copy().withColor(menu.side(side).color()),
                    Component.translatable("gui.atomcraft.sides_click"));
        } else if (hovering(mouseX, mouseY, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            tooltip(graphics, mouseX, mouseY, energyTooltip(menu.energy(), menu.capacity()));
        }
    }
}
