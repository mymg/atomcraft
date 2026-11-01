package com.atomcraft.client.screen;

import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.ReplicatorMenu;
import com.atomcraft.network.ReplicatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Replicador Atômico: biblioteca dos itens pesquisados (com busca), detalhes e
 * composição do item escolhido, quantidade com botões ±10/100/1000 (Shift:
 * ±16/32/64, como no Applied Energistics), produção atual e energia.
 */
public class ReplicatorScreen extends MachineScreen<ReplicatorMenu> {

    private static final int GRID_X = 12, GRID_Y = 40, COLS = 6, ROWS = 5, CELL = 18;
    private static final int INFO_X = 128, INFO_Y = 21, INFO_W = 102, INFO_H = 112;
    private static final int ENERGY_X = 234, ENERGY_Y = 21, ENERGY_W = 13, ENERGY_H = 112;
    private static final int ROW_Y = 137;
    private static final int MAX_BADGES = 5;

    private record Button(int x, int y, int w, int h, Supplier<String> label, Runnable action, BooleanSupplier active) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Button> buttons = new ArrayList<>();
    private EditBox search;
    private EditBox quantity;

    private List<Item> library = List.of();
    private @Nullable Map<Item, Integer> libraryResearch;
    private @Nullable Map<Item, AtomicValue> libraryValues;
    private String libraryQuery = "";
    private int scrollRow;
    private @Nullable Item selected;

    public ReplicatorScreen(ReplicatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ReplicatorMenu.WIDTH, ReplicatorMenu.HEIGHT);
        this.inventoryLabelX = ReplicatorMenu.INVENTORY_X;
    }

    /** Usado pelo modo vitrine do CI para mostrar um item selecionado. */
    public void showcaseSelect(Item item) {
        selected = item;
    }

    private static boolean shift() {
        return Minecraft.getInstance().hasShiftDown();
    }

    /** Passos dos botões: 10/100/1000, ou 16/32/64 com Shift. */
    private static int step(int index) {
        return shift() ? new int[]{16, 32, 64}[index] : new int[]{10, 100, 1000}[index];
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        search = new EditBox(font, leftPos + 11, topPos + 24, 110, 12, Component.translatable("gui.atomcraft.search"));
        search.setHint(Component.translatable("gui.atomcraft.search"));
        search.setMaxLength(50);
        addRenderableWidget(search);

        int x = leftPos + 8, y = topPos + ROW_Y;
        int[] widths = {font.width("-1000") + 6, font.width("-100") + 6, font.width("-10") + 6};
        for (int i = 2; i >= 0; i--) {
            int index = i;
            buttons.add(new Button(x, y, widths[i], 14, () -> "-" + step(index), () -> changeQuantity(-step(index)), () -> true));
            x += widths[i] + 2;
        }
        quantity = new EditBox(font, x, y, 40, 14, Component.translatable("gui.atomcraft.quantity"));
        quantity.setMaxLength(4);
        quantity.setValue("1");
        quantity.setResponder(text -> {
            String digits = text.replaceAll("\\D", "");
            if (!digits.equals(text)) quantity.setValue(digits);
        });
        addRenderableWidget(quantity);
        x += 42;
        for (int i = 0; i < 3; i++) {
            int index = i;
            buttons.add(new Button(x, y, widths[i], 14, () -> "+" + step(index), () -> changeQuantity(step(index)), () -> true));
            x += widths[i] + 2;
        }

        buttons.add(new Button(leftPos + 186, topPos + 152, 62, 12,
                () -> Component.translatable("gui.atomcraft.replicate").getString(), this::replicate, () -> selected != null));
        buttons.add(new Button(leftPos + 186, topPos + 166, 62, 12,
                () -> Component.translatable("gui.atomcraft.cancel").getString(), this::cancel, () -> menu.target() != null));
    }

    private int quantityValue() {
        try {
            return Math.max(1, Integer.parseInt(quantity.getValue()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void changeQuantity(int delta) {
        int base = quantity.getValue().isEmpty() ? 0 : quantityValue();
        // Ao partir de 1, "+10" resulta em 10 (e não 11), como no AE2.
        int value = base == 1 && delta > 0 ? delta : base + delta;
        quantity.setValue(Integer.toString(Math.max(1, Math.min(9999, value))));
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
        double rx = mouseX - leftPos - GRID_X + 1, ry = mouseY - topPos - GRID_Y + 1;
        if (rx < 0 || ry < 0 || rx >= COLS * CELL || ry >= ROWS * CELL) return null;
        int index = ((int) ry / CELL + scrollRow) * COLS + (int) rx / CELL;
        return index < library.size() ? library.get(index) : null;
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 8, topPos + 21, 117, 112);
        TechGui.glass(graphics, leftPos + INFO_X, topPos + INFO_Y, INFO_W, INFO_H);
        TechGui.glass(graphics, leftPos + 8, topPos + 152, 174, 26);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        refreshLibrary();

        // Biblioteca
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = leftPos + GRID_X + col * CELL, y = topPos + GRID_Y + row * CELL;
                TechGui.slot(graphics, x, y);
                int index = (row + scrollRow) * COLS + col;
                if (index < library.size()) {
                    Item item = library.get(index);
                    if (item == selected) graphics.fill(x, y, x + 16, y + 16, TechGui.SELECTED);
                    graphics.item(new ItemStack(item), x, y);
                }
            }
        }
        if (maxScroll() > 0) {
            int barY = topPos + GRID_Y + Math.round((float) scrollRow / maxScroll() * (ROWS * CELL - 12));
            graphics.fill(leftPos + 121, barY, leftPos + 122, barY + 10, TechGui.GLASS_EDGE);
        }
        if (library.isEmpty()) {
            Component empty = Component.translatable("gui.atomcraft.library_empty");
            graphics.text(font, empty, leftPos + 66 - font.width(empty) / 2, topPos + 82, TechGui.TEXT_DIM, false);
        }

        extractInfo(graphics);

        // Energia
        float energy = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + ENERGY_X, topPos + ENERGY_Y, ENERGY_W, ENERGY_H, energy, TechGui.ENERGY);

        // Botões e campo de quantidade
        TechGui.field(graphics, quantity.getX() - 1, quantity.getY() - 1, quantity.getWidth() + 2, quantity.getHeight() + 2);
        for (Button button : buttons) {
            boolean active = button.active().getAsBoolean();
            TechGui.button(graphics, font, button.x(), button.y(), button.w(), button.h(), button.label().get(),
                    button.contains(mouseX, mouseY), active);
        }
    }

    private void extractInfo(GuiGraphicsExtractor graphics) {
        int x = leftPos + INFO_X + 5, y = topPos + INFO_Y + 5;
        TechGui.slot(graphics, x, y);
        if (selected == null) {
            graphics.text(font, Component.translatable("gui.atomcraft.select_item"), x + 20, y + 4, TechGui.TEXT_DIM, false);
        } else {
            ItemStack stack = new ItemStack(selected);
            graphics.item(stack, x, y);
            graphics.text(font, font.plainSubstrByWidth(stack.getHoverName().getString(), 74), x + 20, y + 1, TechGui.TEXT_LIGHT, false);
            AtomicValue value = AtomicValues.get(selected, true);
            if (value != null) {
                graphics.text(font, Component.translatable(value.rarity().translationKey()), x + 20, y + 10,
                        value.rarity().argb(), false);

                // Composição em badges, com a quantidade de cada elemento embaixo
                List<Map.Entry<ChemicalElement, Long>> elements = new ArrayList<>(value.composition().elements().entrySet());
                int shown = Math.min(elements.size(), MAX_BADGES);
                for (int i = 0; i < shown; i++) {
                    var entry = elements.get(i);
                    int bx = x + i * 19;
                    if (i == MAX_BADGES - 1 && elements.size() > MAX_BADGES) {
                        String more = "+" + (elements.size() - MAX_BADGES + 1);
                        graphics.text(font, more, bx + 8 - font.width(more) / 2, y + 25, TechGui.TEXT_TEAL, false);
                        break;
                    }
                    TechGui.badge(graphics, font, bx, y + 20, entry.getKey());
                    String count = "×" + TechGui.shortAmount(entry.getValue());
                    graphics.text(font, count, bx + 8 - font.width(count) / 2, y + 38, TechGui.TEXT_TEAL, false);
                }

                graphics.text(font, Component.translatable("gui.atomcraft.energy_cost", GuiUtil.formatAmount(value.energyCost())),
                        x, y + 50, TechGui.TEXT_TEAL, false);
                graphics.text(font, Component.translatable("gui.atomcraft.time", String.format("%.1f", value.replicateTicks() / 20.0)),
                        x, y + 60, TechGui.TEXT_TEAL, false);
                long available = ClientState.maxFromNetwork(value.composition());
                graphics.text(font, Component.translatable("gui.atomcraft.available", GuiUtil.formatAmount(available)),
                        x, y + 70, available > 0 ? TechGui.TEXT_GOOD : TechGui.TEXT_BAD, false);
            }
        }

        // Produção atual
        int jy = topPos + INFO_Y + 84;
        TechGui.slot(graphics, x, jy);
        Item target = menu.target();
        if (target != null) {
            graphics.item(new ItemStack(target), x, jy);
            graphics.text(font, "×" + menu.remaining(), x + 20, jy, TechGui.TEXT_LIGHT, false);
        }
        TechGui.horizontalBar(graphics, x + 20, jy + 9, 72, 7, menu.progress(), TechGui.STORAGE);
        graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 92), x, jy + 19 - 1,
                statusColor(menu.status()), false);
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Item hovered = itemAt(mouseX, mouseY);
        if (hovered != null) {
            graphics.setTooltipForNextFrame(font, new ItemStack(hovered), mouseX, mouseY);
            return;
        }
        if (hovering(mouseX, mouseY, ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H)) {
            tooltip(graphics, mouseX, mouseY, energyTooltip(menu.energy(), menu.capacity()));
            return;
        }
        if (selected != null) {
            AtomicValue value = AtomicValues.get(selected, true);
            if (value != null) {
                List<Map.Entry<ChemicalElement, Long>> elements = new ArrayList<>(value.composition().elements().entrySet());
                for (int i = 0; i < Math.min(elements.size(), MAX_BADGES); i++) {
                    if (hovering(mouseX, mouseY, INFO_X + 5 + i * 19, INFO_Y + 25, 16, 26)) {
                        if (i == MAX_BADGES - 1 && elements.size() > MAX_BADGES) {
                            tooltip(graphics, mouseX, mouseY, Component.literal(value.composition().formula()));
                        } else {
                            var entry = elements.get(i);
                            long available = ClientState.networkTotals().get(entry.getKey());
                            tooltip(graphics, mouseX, mouseY,
                                    Component.translatable(entry.getKey().translationKey()).append(" (" + entry.getKey().symbol() + ")"),
                                    Component.translatable("gui.atomcraft.atoms", String.format("%,d", entry.getValue())),
                                    Component.translatable("gui.atomcraft.in_network", String.format("%,d", available)));
                        }
                        return;
                    }
                }
            }
        }
        if (menu.target() != null && hovering(mouseX, mouseY, INFO_X + 5, INFO_Y + 84, 16, 16)) {
            graphics.setTooltipForNextFrame(font, new ItemStack(menu.target()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (Button button : buttons) {
                if (button.contains(event.x(), event.y()) && button.active().getAsBoolean()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    button.action().run();
                    return true;
                }
            }
        }
        Item clicked = itemAt(event.x(), event.y());
        if (clicked != null) {
            selected = clicked;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hovering((int) mouseX, (int) mouseY, 8, 38, 117, 95)) {
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
