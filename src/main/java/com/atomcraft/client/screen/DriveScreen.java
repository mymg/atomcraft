package com.atomcraft.client.screen;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.DriveMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Drive: discos e ocupação à esquerda, elementos guardados na rede (badges) à direita. */
public class DriveScreen extends MachineScreen<DriveMenu> {

    private static final int LIST_X = 89, LIST_Y = 21, LIST_W = 80, LIST_H = 78;
    private static final int COLS = 2, ROWS = 4, CELL_W = 37, CELL_H = 18;

    private int scrollRow;

    public DriveScreen(DriveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, DriveMenu.WIDTH, DriveMenu.HEIGHT);
    }

    private List<Map.Entry<ChemicalElement, Long>> entries() {
        return new ArrayList<>(ClientState.networkTotals().elements().entrySet());
    }

    private int maxScroll() {
        int rows = (entries().size() + COLS - 1) / COLS;
        return Math.max(0, rows - ROWS);
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 8, topPos + 21, 78, 78);
        TechGui.glass(graphics, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        long capacity = ClientState.networkCapacity();
        float fraction = capacity <= 0 ? 0 : (float) ClientState.networkUsed() / capacity;
        TechGui.horizontalBar(graphics, leftPos + 12, topPos + 68, 70, 8, fraction, TechGui.STORAGE);
        String amount = storageText();
        graphics.text(font, amount, leftPos + 47 - font.width(amount) / 2, topPos + 81, TechGui.TEXT_TEAL, false);

        List<Map.Entry<ChemicalElement, Long>> entries = entries();
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll()));
        if (entries.isEmpty()) {
            Component empty = Component.translatable("gui.atomcraft.empty");
            graphics.text(font, empty, leftPos + LIST_X + (LIST_W - font.width(empty)) / 2, topPos + LIST_Y + 35, TechGui.TEXT_DIM, false);
        }
        for (int i = 0; i < COLS * ROWS; i++) {
            int index = scrollRow * COLS + i;
            if (index >= entries.size()) break;
            var entry = entries.get(index);
            int x = leftPos + LIST_X + 4 + (i % COLS) * CELL_W;
            int y = topPos + LIST_Y + 4 + (i / COLS) * CELL_H;
            TechGui.badge(graphics, font, x, y, entry.getKey());
            graphics.text(font, TechGui.shortAmount(entry.getValue()), x + 18, y + 4, TechGui.TEXT_TEAL, false);
        }
        if (maxScroll() > 0) {
            int barY = topPos + LIST_Y + 3 + Math.round((float) scrollRow / maxScroll() * (LIST_H - 16));
            graphics.fill(leftPos + LIST_X + LIST_W - 4, barY, leftPos + LIST_X + LIST_W - 3, barY + 10, TechGui.GLASS_EDGE);
        }
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Map.Entry<ChemicalElement, Long>> entries = entries();
        for (int i = 0; i < COLS * ROWS; i++) {
            int index = scrollRow * COLS + i;
            if (index >= entries.size()) break;
            if (hovering(mouseX, mouseY, LIST_X + 4 + (i % COLS) * CELL_W, LIST_Y + 4 + (i / COLS) * CELL_H, CELL_W - 2, 16)) {
                var entry = entries.get(index);
                tooltip(graphics, mouseX, mouseY,
                        TechGui.elementName(entry.getKey()),
                        Component.translatable("gui.atomcraft.atoms", String.format("%,d", entry.getValue())));
                return;
            }
        }
        if (hovering(mouseX, mouseY, 12, 68, 70, 20)) tooltip(graphics, mouseX, mouseY, storageTooltip());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hovering((int) mouseX, (int) mouseY, LIST_X, LIST_Y, LIST_W, LIST_H)) {
            scrollRow = Math.max(0, Math.min(maxScroll(), scrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
