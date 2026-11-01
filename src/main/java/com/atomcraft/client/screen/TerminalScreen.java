package com.atomcraft.client.screen;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.TerminalMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Terminal de Elementos: o estoque da rede organizado como uma tabela
 * periódica. Elementos sem estoque aparecem apagados.
 */
public class TerminalScreen extends MachineScreen<TerminalMenu> {

    /** Grupos da tabela que o mod usa, na ordem das colunas. */
    private static final int[] GROUPS = {1, 2, 8, 11, 13, 14, 15, 16, 17, 18};
    /** Períodos que o mod usa, na ordem das linhas. */
    private static final int[] PERIODS = {1, 2, 3, 4, 6};
    private static final int CELL_W = 22, CELL_H = 26, TABLE_X = 12, TABLE_Y = 22, EXOTIC_Y = 164;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TerminalMenu.WIDTH, TerminalMenu.HEIGHT);
    }

    @Override
    protected boolean showsInventory() {
        return false;
    }

    /** Posição relativa da célula do elemento. */
    private static int[] cell(ChemicalElement element) {
        if (element.exotic()) {
            int index = element.ordinal() - ChemicalElement.REDSTONIUM.ordinal();
            return new int[]{TABLE_X + (2 + index) * CELL_W, EXOTIC_Y};
        }
        int column = indexOf(GROUPS, group(element));
        int row = indexOf(PERIODS, period(element));
        return new int[]{TABLE_X + column * CELL_W, TABLE_Y + row * CELL_H};
    }

    private static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++) if (array[i] == value) return i;
        return 0;
    }

    private static int period(ChemicalElement element) {
        int z = element.atomicNumber();
        if (z <= 2) return 1;
        if (z <= 10) return 2;
        if (z <= 18) return 3;
        if (z <= 36) return 4;
        if (z <= 54) return 5;
        return 6;
    }

    private static int group(ChemicalElement element) {
        int z = element.atomicNumber();
        if (z == 1) return 1;
        if (z == 2) return 18;
        if (z <= 18) {
            int position = (z - 2) % 8 == 0 ? 8 : (z - 2) % 8;
            return position <= 2 ? position : position + 10;
        }
        if (z <= 36) return z - 18;
        if (z >= 72 && z <= 86) return z - 68; // 6º período, depois dos lantanídeos
        return 3;
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 8, topPos + 18, imageWidth - 16, 134);
        TechGui.glass(graphics, leftPos + 8, topPos + 154, imageWidth - 16, 38);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Composition totals = ClientState.networkTotals();
        graphics.text(font, Component.translatable("gui.atomcraft.exotic"), leftPos + 13, topPos + EXOTIC_Y + 4,
                TechGui.TEXT_DIM, false);
        for (ChemicalElement element : ChemicalElement.values()) {
            int[] pos = cell(element);
            long amount = totals.get(element);
            int x = leftPos + pos[0], y = topPos + pos[1];
            if (hovering(mouseX, mouseY, pos[0], pos[1], CELL_W, CELL_H - 1)) {
                graphics.fill(x, y - 1, x + CELL_W, y + CELL_H - 2, 0x402E8F7E);
            }
            TechGui.badge(graphics, font, x + 3, y, element, amount <= 0);
            String text = amount <= 0 ? "-" : TechGui.shortAmount(amount);
            graphics.text(font, text, x + (CELL_W - font.width(text) + 1) / 2, y + 17,
                    amount <= 0 ? 0xFF4A5560 : TechGui.TEXT_TEAL, false);
        }
        storageRow(graphics, 9, 196, imageWidth - 9);
        long kinds = totals.elements().values().stream().filter(v -> v > 0).count();
        graphics.text(font, Component.translatable("gui.atomcraft.element_kinds", kinds), leftPos + 9, topPos + 207,
                TechGui.TEXT_DIM, false);
    }

    private @Nullable ChemicalElement hovered(int mouseX, int mouseY) {
        for (ChemicalElement element : ChemicalElement.values()) {
            int[] pos = cell(element);
            if (hovering(mouseX, mouseY, pos[0], pos[1], CELL_W, CELL_H - 1)) return element;
        }
        return null;
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ChemicalElement element = hovered(mouseX, mouseY);
        if (element != null) {
            long amount = ClientState.networkTotals().get(element);
            long used = ClientState.networkUsed();
            String share = used <= 0 ? "0" : String.format("%.1f", amount * 100.0 / used);
            Component amountLine = Component.translatable("gui.atomcraft.element_amount", String.format("%,d", amount), share);
            List<Component> lines = ClientState.isDiscovered(element)
                    ? List.of(TechGui.elementName(element),
                            Component.translatable("gui.atomcraft.atomic_number", element.atomicNumber(),
                                    String.format("%.3f", element.atomicMass())),
                            amountLine)
                    : List.of(TechGui.elementName(element),
                            Component.translatable("gui.atomcraft.unknown_hint"),
                            amountLine);
            tooltip(graphics, mouseX, mouseY, lines.toArray(Component[]::new));
        } else if (hovering(mouseX, mouseY, 9, 196, imageWidth - 18, 20)) {
            tooltip(graphics, mouseX, mouseY, storageTooltip());
        }
    }
}
