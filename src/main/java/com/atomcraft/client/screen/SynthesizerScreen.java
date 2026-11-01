package com.atomcraft.client.screen;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.SynthesizerMenu;
import com.atomcraft.network.SynthesizePayload;
import com.atomcraft.synthesis.SynthesisRecipe;
import com.atomcraft.synthesis.SynthesisRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sintetizador: biblioteca de schematics à esquerda, itens fabricáveis à
 * direita (clique para escolher) e o custo em elementos embaixo.
 */
public class SynthesizerScreen extends MachineScreen<SynthesizerMenu> {

    private static final int RECIPES_X = 68, RECIPES_Y = 25, CELL = 20, COLUMNS = 4, COST_Y = 63;

    public SynthesizerScreen(SynthesizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SynthesizerMenu.WIDTH, SynthesizerMenu.HEIGHT);
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 22, topPos + 21, 133, 60);
        graphics.fill(leftPos + 64, topPos + 25, leftPos + 65, topPos + 77, TechGui.GLASS_EDGE_DIM);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float energy = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + 9, topPos + 21, 11, 60, energy, TechGui.ENERGY);
        TechGui.verticalBar(graphics, leftPos + 158, topPos + 21, 11, 60, menu.progress(), TechGui.PROGRESS);

        List<Item> available = menu.available();
        Item selected = menu.selected();
        if (available.isEmpty()) {
            graphics.text(font, Component.translatable("gui.atomcraft.insert_schematics_1"), leftPos + RECIPES_X, topPos + 30, TechGui.TEXT_DIM, false);
            graphics.text(font, Component.translatable("gui.atomcraft.insert_schematics_2"), leftPos + RECIPES_X, topPos + 41, TechGui.TEXT_DIM, false);
        }
        for (int i = 0; i < available.size() && i < COLUMNS * 2; i++) {
            int x = leftPos + RECIPES_X + (i % COLUMNS) * CELL, y = topPos + RECIPES_Y + (i / COLUMNS) * CELL;
            Item item = available.get(i);
            boolean hover = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
            graphics.fill(x, y, x + 18, y + 18, item == selected ? TechGui.SELECTED : hover ? TechGui.SLOT_LIGHT : TechGui.SLOT_BG);
            if (item == selected) {
                graphics.fill(x, y, x + 18, y + 1, TechGui.GLASS_EDGE);
                graphics.fill(x, y + 17, x + 18, y + 18, TechGui.GLASS_EDGE);
            }
            graphics.item(new ItemStack(item), x + 1, y + 1);
        }

        SynthesisRecipe recipe = selected == null ? null : SynthesisRecipes.get(selected);
        if (recipe != null) {
            int i = 0;
            for (var entry : recipe.cost().elements().entrySet()) {
                if (i >= 4) break;
                int bx = leftPos + RECIPES_X + i * 21, by = topPos + COST_Y;
                TechGui.badge(graphics, font, bx, by, entry.getKey(), ClientState.networkTotals().get(entry.getKey()) < entry.getValue());
                i++;
            }
        } else if (!available.isEmpty()) {
            graphics.text(font, Component.translatable("gui.atomcraft.select_item"), leftPos + RECIPES_X, topPos + COST_Y + 4, TechGui.TEXT_DIM, false);
        }

        graphics.text(font, Component.translatable("gui.atomcraft.output"), leftPos + 26, topPos + SynthesizerMenu.OUTPUT_Y + 5, TechGui.TEXT_LIGHT, false);
        storageRow(graphics, 9, 108, 169);
        graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 100), leftPos + 9, topPos + 119,
                statusColor(menu.status()), false);
    }

    private @Nullable Item recipeAt(double mouseX, double mouseY) {
        List<Item> available = menu.available();
        for (int i = 0; i < available.size() && i < COLUMNS * 2; i++) {
            int x = leftPos + RECIPES_X + (i % COLUMNS) * CELL, y = topPos + RECIPES_Y + (i / COLUMNS) * CELL;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) return available.get(i);
        }
        return null;
    }

    private @Nullable Map.Entry<ChemicalElement, Long> costAt(double mouseX, double mouseY) {
        Item selected = menu.selected();
        SynthesisRecipe recipe = selected == null ? null : SynthesisRecipes.get(selected);
        if (recipe == null) return null;
        int i = 0;
        for (var entry : recipe.cost().elements().entrySet()) {
            int bx = leftPos + RECIPES_X + i * 21, by = topPos + COST_Y;
            if (mouseX >= bx && mouseX < bx + 16 && mouseY >= by && mouseY < by + 16) return entry;
            if (++i >= 4) break;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Item clicked = event.button() == 0 ? recipeAt(event.x(), event.y()) : null;
        if (clicked != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            ClientPacketDistributor.sendToServer(new SynthesizePayload(menu.pos(), clicked == menu.selected() ? Items.AIR : clicked));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Item hoveredRecipe = recipeAt(mouseX, mouseY);
        var cost = costAt(mouseX, mouseY);
        if (hoveredRecipe != null) {
            SynthesisRecipe recipe = SynthesisRecipes.get(hoveredRecipe);
            List<Component> lines = new ArrayList<>();
            lines.add(new ItemStack(hoveredRecipe).getHoverName());
            if (recipe != null) {
                lines.add(Component.literal(ClientState.formula(recipe.cost())));
                lines.add(Component.translatable("gui.atomcraft.energy_cost", String.format("%,d", recipe.energy())));
                lines.add(Component.translatable("gui.atomcraft.time", String.format("%.1f", recipe.ticks() / 20.0)));
            }
            lines.add(Component.translatable(hoveredRecipe == menu.selected() ? "gui.atomcraft.click_to_stop" : "gui.atomcraft.click_to_select"));
            tooltip(graphics, mouseX, mouseY, lines.toArray(Component[]::new));
        } else if (cost != null) {
            tooltip(graphics, mouseX, mouseY, TechGui.elementName(cost.getKey()),
                    Component.translatable("gui.atomcraft.cost_amount", cost.getValue(),
                            GuiUtil.formatAmount(ClientState.networkTotals().get(cost.getKey()))));
        } else if (hovering(mouseX, mouseY, 9, 21, 11, 60)) {
            tooltip(graphics, mouseX, mouseY, energyTooltip(menu.energy(), menu.capacity()));
        } else if (hovering(mouseX, mouseY, 158, 21, 11, 60)) {
            tooltip(graphics, mouseX, mouseY, menu.status().label(), Component.literal(Math.round(menu.progress() * 100) + "%"));
        } else if (hovering(mouseX, mouseY, 9, 108, 160, 20)) {
            tooltip(graphics, mouseX, mouseY, storageTooltip());
        }
    }
}
