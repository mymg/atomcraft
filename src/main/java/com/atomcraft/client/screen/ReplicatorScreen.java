package com.atomcraft.client.screen;

import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.ReplicatorMenu;
import com.atomcraft.network.ReplicatePayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Replicador Atômico: biblioteca dos itens pesquisados (com busca), detalhes do
 * item escolhido, quantidade e botões para iniciar ou cancelar a replicação.
 */
public class ReplicatorScreen extends MachineScreen<ReplicatorMenu> {

    private static final int GRID_X = 8, GRID_Y = 34, COLS = 6, ROWS = 5, CELL = 18;
    private static final int PANEL_X = 124;
    private static final int ENERGY_X = 224, ENERGY_Y = 18, ENERGY_W = 8, ENERGY_H = 118;

    private EditBox search;
    private EditBox quantity;
    private Button replicateButton;
    private Button cancelButton;

    private List<Item> library = List.of();
    private @Nullable Map<Item, Integer> libraryResearch;
    private @Nullable Map<Item, AtomicValue> libraryValues;
    private String libraryQuery = "";
    private int scrollRow;
    private @Nullable Item selected;

    public ReplicatorScreen(ReplicatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ReplicatorMenu.WIDTH, ReplicatorMenu.HEIGHT);
        this.inventoryLabelX = ReplicatorMenu.OUTPUT_X;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + GRID_X, topPos + 18, COLS * CELL, 12, Component.translatable("gui.atomcraft.search"));
        search.setHint(Component.translatable("gui.atomcraft.search"));
        search.setMaxLength(50);
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("-"), button -> changeQuantity(-1))
                .bounds(leftPos + PANEL_X, topPos + 82, 14, 14).build());
        quantity = new EditBox(font, leftPos + PANEL_X + 17, topPos + 83, 40, 12, Component.translatable("gui.atomcraft.quantity"));
        quantity.setMaxLength(4);
        quantity.setValue("1");
        quantity.setResponder(text -> {
            String digits = text.replaceAll("\\D", "");
            if (!digits.equals(text)) quantity.setValue(digits);
        });
        addRenderableWidget(quantity);
        addRenderableWidget(Button.builder(Component.literal("+"), button -> changeQuantity(1))
                .bounds(leftPos + PANEL_X + 60, topPos + 82, 14, 14).build());

        replicateButton = addRenderableWidget(Button.builder(Component.translatable("gui.atomcraft.replicate"), button -> replicate())
                .bounds(leftPos + PANEL_X, topPos + 100, 47, 16).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gui.atomcraft.cancel"), button -> cancel())
                .bounds(leftPos + PANEL_X + 50, topPos + 100, 47, 16).build());
    }

    private int quantityValue() {
        try {
            return Math.max(1, Integer.parseInt(quantity.getValue()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void changeQuantity(int delta) {
        quantity.setValue(Integer.toString(Math.max(1, Math.min(9999, quantityValue() + delta))));
    }

    private void replicate() {
        if (selected != null) ClientPacketDistributor.sendToServer(new ReplicatePayload(menu.pos(), selected, quantityValue()));
    }

    private void cancel() {
        ClientPacketDistributor.sendToServer(new ReplicatePayload(menu.pos(), Items.AIR, 0));
    }

    /** Refaz a lista quando a pesquisa, os valores ou o texto de busca mudam. */
    private void refreshLibrary() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        Map<Item, Integer> research = ClientState.research();
        Map<Item, AtomicValue> values = AtomicValues.clientValues();
        if (research == libraryResearch && values == libraryValues && query.equals(libraryQuery)) return;
        libraryResearch = research;
        libraryValues = values;
        libraryQuery = query;

        List<Item> items = new ArrayList<>();
        for (Item item : research.keySet()) {
            if (!ClientState.isResearched(item)) continue;
            String name = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query)) items.add(item);
        }
        items.sort(Comparator.comparing(item -> new ItemStack(item).getHoverName().getString()));
        library = items;
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll()));
    }

    private int maxScroll() {
        int rows = (library.size() + COLS - 1) / COLS;
        return Math.max(0, rows - ROWS);
    }

    private @Nullable Item itemAt(double mouseX, double mouseY) {
        if (!GuiUtil.inside(mouseX, mouseY, leftPos + GRID_X, topPos + GRID_Y, COLS * CELL, ROWS * CELL)) return null;
        int col = (int) (mouseX - leftPos - GRID_X) / CELL;
        int row = (int) (mouseY - topPos - GRID_Y) / CELL;
        int index = (row + scrollRow) * COLS + col;
        return index >= 0 && index < library.size() ? library.get(index) : null;
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        refreshLibrary();
        replicateButton.active = selected != null;
        cancelButton.active = menu.target() != null;

        // Biblioteca
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = leftPos + GRID_X + col * CELL + 1;
                int y = topPos + GRID_Y + row * CELL + 1;
                GuiUtil.slot(graphics, x, y);
                int index = (row + scrollRow) * COLS + col;
                if (index < library.size()) {
                    Item item = library.get(index);
                    if (item == selected) graphics.fill(x, y, x + 16, y + 16, 0xFF5FAF5F);
                    graphics.item(new ItemStack(item), x, y);
                }
            }
        }
        if (maxScroll() > 0) {
            float fraction = (float) scrollRow / maxScroll();
            int barY = topPos + GRID_Y + Math.round(fraction * (ROWS * CELL - 10));
            graphics.fill(leftPos + GRID_X + COLS * CELL + 1, barY, leftPos + GRID_X + COLS * CELL + 3, barY + 10, 0xFF555555);
        }

        // Item escolhido
        GuiUtil.slot(graphics, leftPos + PANEL_X, topPos + 18);
        if (selected != null) graphics.item(new ItemStack(selected), leftPos + PANEL_X, topPos + 18);

        // Produção atual
        Item target = menu.target();
        GuiUtil.slot(graphics, leftPos + PANEL_X, topPos + 121);
        if (target != null) graphics.item(new ItemStack(target), leftPos + PANEL_X, topPos + 121);
        GuiUtil.horizontalBar(graphics, leftPos + PANEL_X + 20, topPos + 132, 76, 5, menu.progress(), 0xFF4CAF50);

        // Energia
        int capacity = Math.max(1, menu.capacity());
        GuiUtil.verticalBar(graphics, leftPos + ENERGY_X, topPos + ENERGY_Y, ENERGY_W, ENERGY_H,
                (float) menu.energy() / capacity, 0xFFD03030);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int x = PANEL_X;
        if (selected != null) {
            ItemStack stack = new ItemStack(selected);
            graphics.text(font, font.plainSubstrByWidth(stack.getHoverName().getString(), 76), x + 20, 18, GuiUtil.TEXT, false);
            AtomicValue value = AtomicValues.get(selected, true);
            if (value != null) {
                graphics.text(font, Component.translatable(value.rarity().translationKey()).withStyle(value.rarity().color()), x + 20, 27, 0xFFFFFFFF, true);
                graphics.text(font, font.plainSubstrByWidth(value.composition().formula(), 96), x, 39, 0xFF1E5A8C, false);
                graphics.text(font, Component.translatable("gui.atomcraft.energy_cost", GuiUtil.formatAmount(value.energyCost())), x, 49, GuiUtil.TEXT, false);
                graphics.text(font, Component.translatable("gui.atomcraft.time", String.format("%.1f", value.replicateTicks() / 20.0)), x, 59, GuiUtil.TEXT, false);
                long available = ClientState.maxFromNetwork(value.composition());
                graphics.text(font, Component.translatable("gui.atomcraft.available", GuiUtil.formatAmount(available)), x, 69,
                        available > 0 ? 0xFF2E7D32 : 0xFFB03030, false);
            }
        } else {
            graphics.text(font, Component.translatable("gui.atomcraft.select_item"), x, 22, GuiUtil.TEXT_LIGHT, false);
        }

        Item target = menu.target();
        if (target != null) {
            graphics.text(font, "×" + menu.remaining(), x + 20, 121, GuiUtil.TEXT, false);
        }
        graphics.text(font, menu.status().label(), x, 140, menu.status().color(), false);

        if (library.isEmpty()) {
            graphics.text(font, Component.translatable("gui.atomcraft.library_empty"), GRID_X + 4, GRID_Y + 4, 0xFFE0E0E0, true);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Item hovered = itemAt(mouseX, mouseY);
        if (hovered != null) {
            graphics.setTooltipForNextFrame(font, new ItemStack(hovered), mouseX, mouseY);
        } else if (GuiUtil.inside(mouseX, mouseY, leftPos + ENERGY_X, topPos + ENERGY_Y, ENERGY_W, ENERGY_H)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.atomcraft.energy",
                    GuiUtil.formatAmount(menu.energy()), GuiUtil.formatAmount(menu.capacity()))), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Item clicked = itemAt(event.x(), event.y());
        if (clicked != null) {
            selected = clicked;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (GuiUtil.inside(mouseX, mouseY, leftPos + GRID_X, topPos + GRID_Y, COLS * CELL + 4, ROWS * CELL)) {
            scrollRow = Math.max(0, Math.min(maxScroll(), scrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Enquanto digita na busca ou na quantidade, as teclas não fecham a interface.
        EditBox focused = search.isFocused() ? search : quantity.isFocused() ? quantity : null;
        if (focused != null && event.key() != GLFW.GLFW_KEY_ESCAPE) {
            focused.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }
}
