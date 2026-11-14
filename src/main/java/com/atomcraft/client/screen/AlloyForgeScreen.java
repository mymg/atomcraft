package com.atomcraft.client.screen;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.AlloyForgeMenu;
import com.atomcraft.network.SynthesizePayload;
import com.atomcraft.alloy.AlloyRecipe;
import com.atomcraft.alloy.AlloyRecipes;
import com.atomcraft.upgrade.UpgradeType;
import com.atomcraft.upgrade.Upgrades;
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
 * Forja de Ligas: receitas em grade (clique para escolher; role a roda do mouse se
 * houver mais), elementos da receita escolhida embaixo e a saída.
 */
public class AlloyForgeScreen extends MachineScreen<AlloyForgeMenu> {

    private static final int RECIPES_X = 27, RECIPES_Y = 24, CELL = 20, COLUMNS = 6, ROWS = 2, COST_Y = 64;

    private int scrollRow;

    public AlloyForgeScreen(AlloyForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AlloyForgeMenu.WIDTH, AlloyForgeMenu.HEIGHT);
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 22, topPos + 21, 133, 60);
        graphics.fill(leftPos + 26, topPos + COST_Y - 2, leftPos + 151, topPos + COST_Y - 1, TechGui.GLASS_EDGE_DIM);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float energy = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + 9, topPos + 21, 11, 60, energy, TechGui.ENERGY);
        TechGui.verticalBar(graphics, leftPos + 158, topPos + 21, 11, 60, menu.progress(), TechGui.PROGRESS);

        List<Item> available = menu.available();
        Item selected = menu.selected();
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll()));
        if (available.isEmpty()) {
            graphics.text(font, Component.translatable("gui.atomcraft.no_alloy_recipes"), leftPos + RECIPES_X, topPos + 30, TechGui.TEXT_DIM, false);
        }
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            int index = scrollRow * COLUMNS + i;
            if (index >= available.size()) break;
            int x = leftPos + RECIPES_X + (i % COLUMNS) * CELL, y = topPos + RECIPES_Y + (i / COLUMNS) * CELL;
            Item item = available.get(index);
            boolean hover = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
            graphics.fill(x, y, x + 18, y + 18, item == selected ? TechGui.SELECTED : hover ? TechGui.SLOT_LIGHT : TechGui.SLOT_BG);
            if (item == selected) {
                graphics.fill(x, y, x + 18, y + 1, TechGui.GLASS_EDGE);
                graphics.fill(x, y + 17, x + 18, y + 18, TechGui.GLASS_EDGE);
            }
            graphics.item(new ItemStack(item), x + 1, y + 1);
        }
        if (maxScroll() > 0) {
            int barY = topPos + RECIPES_Y + Math.round((float) scrollRow / maxScroll() * (ROWS * CELL - 10));
            graphics.fill(leftPos + 150, barY, leftPos + 151, barY + 10, TechGui.GLASS_EDGE);
        }

        AlloyRecipe recipe = selected == null ? null : AlloyRecipes.get(selected);
        if (recipe != null) {
            int i = 0;
            for (var entry : recipe.elements().elements().entrySet()) {
                if (i >= 6) break;
                int bx = leftPos + RECIPES_X + i * 20, by = topPos + COST_Y;
                TechGui.badge(graphics, font, bx, by, entry.getKey(), ClientState.networkTotals().get(entry.getKey()) < entry.getValue());
                String amount = TechGui.shortAmount(entry.getValue());
                TechGui.smallText(graphics, font, amount, bx + 17 - TechGui.smallWidth(font, amount), by + 13, 0xFFFFFFFF, true);
                i++;
            }
        } else if (!available.isEmpty()) {
            graphics.text(font, Component.translatable("gui.atomcraft.select_alloy"), leftPos + RECIPES_X, topPos + COST_Y + 4, TechGui.TEXT_DIM, false);
        }

        graphics.text(font, Component.translatable("gui.atomcraft.output"), leftPos + 26, topPos + AlloyForgeMenu.OUTPUT_Y + 5, TechGui.TEXT_LIGHT, false);
        storageRow(graphics, 9, 108, 169);
        graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 100), leftPos + 9, topPos + 119,
                statusColor(menu.status()), false);
    }

    private int maxScroll() {
        int rows = (menu.available().size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - ROWS);
    }

    private @Nullable Item recipeAt(double mouseX, double mouseY) {
        List<Item> available = menu.available();
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            int index = scrollRow * COLUMNS + i;
            if (index >= available.size()) break;
            int x = leftPos + RECIPES_X + (i % COLUMNS) * CELL, y = topPos + RECIPES_Y + (i / COLUMNS) * CELL;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) return available.get(index);
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hovering((int) mouseX, (int) mouseY, 22, 21, 133, COST_Y - 21)) {
            scrollRow = Math.max(0, Math.min(maxScroll(), scrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Map.@Nullable Entry<ChemicalElement, Long> costAt(double mouseX, double mouseY) {
        Item selected = menu.selected();
        AlloyRecipe recipe = selected == null ? null : AlloyRecipes.get(selected);
        if (recipe == null) return null;
        int i = 0;
        for (var entry : recipe.elements().elements().entrySet()) {
            int bx = leftPos + RECIPES_X + i * 20, by = topPos + COST_Y;
            if (mouseX >= bx && mouseX < bx + 16 && mouseY >= by && mouseY < by + 16) return entry;
            if (++i >= 6) break;
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
            AlloyRecipe recipe = AlloyRecipes.get(hoveredRecipe);
            List<Component> lines = new ArrayList<>();
            lines.add(new ItemStack(hoveredRecipe).getHoverName());
            if (recipe != null) {
                lines.add(Component.literal(ClientState.formula(recipe.elements())));
                int speed = installedUpgrades(UpgradeType.SPEED);
                lines.add(Component.translatable("gui.atomcraft.energy_cost", String.format("%,d",
                        Upgrades.energy(recipe.energy(), speed, installedUpgrades(UpgradeType.EFFICIENCY)))));
                lines.add(Component.translatable("gui.atomcraft.time", GuiUtil.formatDuration(Upgrades.ticks(recipe.ticks(), speed))));
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
