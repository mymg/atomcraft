package com.atomcraft.client.screen;

import com.atomcraft.block.entity.ChunkLoaderBlockEntity;
import com.atomcraft.menu.ChunkLoaderMenu;
import com.atomcraft.network.ChunkLoaderPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Carregador de Chunks: grade das chunks em volta (a do meio é a do carregador, sempre carregada).
 * Clique numa chunk para carregá-la ou soltá-la; à direita, quantas estão escolhidas, o custo e quantas
 * estão carregadas agora.
 */
public class ChunkLoaderScreen extends MachineScreen<ChunkLoaderMenu> {

    private static final int BAR_X = 9, BAR_Y = 21, BAR_W = 14, BAR_H = 80;
    private static final int GRID_X = 33, GRID_Y = 23, CELL = 15, INFO_X = 118;
    private static final int GRID = ChunkLoaderBlockEntity.GRID, MAX = ChunkLoaderBlockEntity.MAX_RADIUS;

    public ChunkLoaderScreen(ChunkLoaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ChunkLoaderMenu.WIDTH, ChunkLoaderMenu.HEIGHT);
    }

    @Override
    protected boolean showsInventory() {
        return false;
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 29, topPos + 21, 82, 80);
        TechGui.glass(graphics, leftPos + 114, topPos + 21, 54, 80);
    }

    private int cellAt(double mouseX, double mouseY) {
        int col = (int) Math.floor((mouseX - leftPos - GRID_X) / CELL), row = (int) Math.floor((mouseY - topPos - GRID_Y) / CELL);
        if (col < 0 || row < 0 || col >= GRID || row >= GRID) return -1;
        return row * GRID + col;
    }

    private boolean inRange(int cell) {
        int dx = cell % GRID - MAX, dz = cell / GRID - MAX;
        return Math.abs(dx) <= menu.radius() && Math.abs(dz) <= menu.radius();
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float fraction = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H, fraction, TechGui.ENERGY);

        int hovered = cellAt(mouseX, mouseY);
        for (int cell = 0; cell < GRID * GRID; cell++) {
            int x = leftPos + GRID_X + (cell % GRID) * CELL, y = topPos + GRID_Y + (cell / GRID) * CELL;
            boolean range = inRange(cell), selected = menu.selected(cell), center = cell == ChunkLoaderBlockEntity.CENTER;
            int border = center ? 0xFFFFC040 : cell == hovered && range ? 0xFFFFFFFF : 0xFF0A1418;
            int fill = !range ? 0xFF161A1E : selected ? (menu.loaded() > 0 ? 0xFF2FB8D0 : 0xFF2A6070) : 0xFF22303A;
            graphics.fill(x, y, x + CELL - 1, y + CELL - 1, border);
            graphics.fill(x + 1, y + 1, x + CELL - 2, y + CELL - 2, fill);
            if (!range) graphics.fill(x + 3, y + 6, x + CELL - 4, y + 8, 0xFF2A2F36);
        }

        int x = leftPos + INFO_X, y = topPos + 25;
        TechGui.smallText(graphics, font, Component.translatable("gui.atomcraft.chunks").getString(), x, y, TechGui.TEXT_DIM, false);
        graphics.text(font, menu.selectedCount() + " / " + menu.maxChunks(), x, y + 9, TechGui.TEXT_LIGHT, false);
        TechGui.smallText(graphics, font, Component.translatable("gui.atomcraft.cost").getString(), x, y + 24, TechGui.TEXT_DIM, false);
        graphics.text(font, GuiUtil.formatAmount((long) menu.selectedCount() * menu.costPerChunk()), x, y + 33, TechGui.TEXT_LIGHT, false);
        TechGui.smallText(graphics, font, "FE/t", x, y + 43, TechGui.TEXT_DIM, false);
        TechGui.smallText(graphics, font, Component.translatable("gui.atomcraft.loaded").getString(), x, y + 56, TechGui.TEXT_DIM, false);
        graphics.text(font, String.valueOf(menu.loaded()), x, y + 65, menu.loaded() > 0 ? TechGui.TEXT_GOOD : TechGui.TEXT_BAD, false);

        graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 160), leftPos + 9, topPos + 108,
                statusColor(menu.status()), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int cell = event.button() == 0 ? cellAt(event.x(), event.y()) : -1;
        if (cell >= 0 && cell != ChunkLoaderBlockEntity.CENTER && inRange(cell)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            ClientPacketDistributor.sendToServer(new ChunkLoaderPayload(menu.pos(), cell));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            ChunkPos center = ChunkPos.containing(menu.pos());
            int cx = center.x() + cell % GRID - MAX, cz = center.z() + cell / GRID - MAX;
            Component hint = cell == ChunkLoaderBlockEntity.CENTER ? Component.translatable("gui.atomcraft.chunk_center")
                    : !inRange(cell) ? Component.translatable("gui.atomcraft.chunk_out_of_range")
                    : Component.translatable(menu.selected(cell) ? "gui.atomcraft.chunk_unload" : "gui.atomcraft.chunk_load");
            tooltip(graphics, mouseX, mouseY, Component.translatable("gui.atomcraft.chunk_pos", cx, cz), hint.copy().withColor(TechGui.TEXT_DIM));
        } else if (hovering(mouseX, mouseY, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            tooltip(graphics, mouseX, mouseY, energyTooltip(menu.energy(), menu.capacity()));
        }
    }
}
