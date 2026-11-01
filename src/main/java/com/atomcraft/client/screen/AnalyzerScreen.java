package com.atomcraft.client.screen;

import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.AnalyzerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Analisador: progresso da análise, buffer 3×3 e painel com o item em pesquisa. */
public class AnalyzerScreen extends MachineScreen<AnalyzerMenu> {

    public AnalyzerScreen(AnalyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AnalyzerMenu.WIDTH, AnalyzerMenu.HEIGHT);
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 22, topPos + 21, 61, 60);
        TechGui.glass(graphics, leftPos + 87, topPos + 21, 82, 60);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.verticalBar(graphics, leftPos + 9, topPos + 21, 11, 60, menu.progress(), TechGui.PROGRESS);

        int x = leftPos + 91, y = topPos + 25;
        TechGui.slot(graphics, x + 1, y + 1);
        Item item = menu.currentItem();
        if (item != null) {
            ItemStack stack = new ItemStack(item);
            graphics.item(stack, x + 1, y + 1);
            graphics.text(font, font.plainSubstrByWidth(stack.getHoverName().getString(), 54), x + 21, y + 2, TechGui.TEXT_LIGHT, false);
            AtomicValue value = AtomicValues.get(item, true);
            if (value != null) {
                graphics.text(font, Component.translatable(value.rarity().translationKey()), x + 21, y + 11,
                        TechGui.scale(0xFF000000 | value.rarity().color().getColor(), 1.0), false);
            }
            int required = Math.max(1, menu.required());
            String count = menu.researched() + "/" + menu.required();
            TechGui.horizontalBar(graphics, x, y + 22, 74, 8, (float) menu.researched() / required, TechGui.STORAGE);
            graphics.text(font, count, x + 74 - font.width(count), y + 32, TechGui.TEXT_TEAL, false);
        } else {
            graphics.text(font, Component.translatable("gui.atomcraft.insert_items"), x + 21, y + 6, TechGui.TEXT_DIM, false);
        }
        graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 74), x, y + 44,
                statusColor(menu.status()), false);

        long researched = ClientState.research().keySet().stream().filter(ClientState::isResearched).count();
        graphics.text(font, Component.translatable("gui.atomcraft.researched_total", researched),
                leftPos + 9, topPos + 87, TechGui.TEXT_LIGHT, false);
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hovering(mouseX, mouseY, 9, 21, 11, 60)) {
            tooltip(graphics, mouseX, mouseY, menu.status().label(), Component.literal(Math.round(menu.progress() * 100) + "%"));
        } else if (menu.currentItem() != null && hovering(mouseX, mouseY, 92, 26, 16, 16)) {
            graphics.setTooltipForNextFrame(font, new ItemStack(menu.currentItem()), mouseX, mouseY);
        }
    }
}
