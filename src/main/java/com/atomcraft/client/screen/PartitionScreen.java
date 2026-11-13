package com.atomcraft.client.screen;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.client.ClientState;
import com.atomcraft.network.PartitionPayload;
import com.atomcraft.storage.DiskPartition;
import com.atomcraft.storage.ElementDiskItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Partição de um disco (clique com o botão direito segurando o disco): escolhe quais
 * elementos ele aceita e, para cada um, um limite opcional. Sem nenhum elemento
 * marcado o disco aceita tudo. Só elementos já descobertos podem ser marcados.
 */
public class PartitionScreen extends Screen {

    private static final int WIDTH = 304, HEIGHT = 222, TAB = 10;
    private static final int CLEAR_W = 50, CLEAR_H = 13;

    private final InteractionHand hand;
    private final Map<ChemicalElement, Long> limits = new EnumMap<>(ChemicalElement.class);
    private @Nullable ChemicalElement selected;
    private @Nullable EditBox limitBox;
    private int left, top;

    public PartitionScreen(InteractionHand hand) {
        super(Component.translatable("gui.atomcraft.partition"));
        this.hand = hand;
        ItemStack stack = disk();
        if (stack != null) limits.putAll(ElementDiskItem.partition(stack).limits());
    }

    private @Nullable ItemStack disk() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        ItemStack stack = player.getItemInHand(hand);
        return stack.getItem() instanceof ElementDiskItem ? stack : null;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        limitBox = new EditBox(font, left + 150, top + TerminalScreen.STORAGE_Y + 3, 70, 14, Component.translatable("gui.atomcraft.partition_limit"));
        limitBox.setMaxLength(12);
        limitBox.setResponder(text -> {
            if (selected == null || !limits.containsKey(selected)) return;
            String digits = text.replaceAll("[^0-9]", "");
            long value;
            try {
                value = digits.isEmpty() ? 0 : Long.parseLong(digits);
            } catch (NumberFormatException e) {
                value = 0;
            }
            limits.put(selected, value);
        });
        addRenderableWidget(limitBox);
        updateBox();
    }

    private void updateBox() {
        if (limitBox == null) return;
        boolean editable = selected != null && limits.containsKey(selected);
        limitBox.visible = editable;
        if (editable) {
            long limit = limits.get(selected);
            limitBox.setValue(limit == 0 ? "" : Long.toString(limit));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int clearX() {
        return left + WIDTH - 9 - CLEAR_W;
    }

    private int clearY() {
        return top + TerminalScreen.STORAGE_Y + 4;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        TechGui.frame(graphics, left, top + TAB, WIDTH, HEIGHT - TAB);
        ItemStack stack = disk();
        String title = stack == null ? getTitle().getString() : getTitle().getString() + ": " + stack.getHoverName().getString();
        TechGui.tab(graphics, font, left + WIDTH / 2, top, title);
        TechGui.glass(graphics, left + 8, top + 18, WIDTH - 16, TerminalScreen.TABLE_Y + 7 * TerminalScreen.CELL_H - 16);

        for (ChemicalElement element : ChemicalElement.values()) {
            int[] pos = TerminalScreen.cell(element);
            int x = left + pos[0], y = top + pos[1];
            boolean in = limits.containsKey(element);
            if (element == selected) {
                graphics.fill(x, y - 1, x + TerminalScreen.CELL_W, y + TerminalScreen.CELL_H - 2, TechGui.SELECTED);
            } else if (hovered(mouseX, mouseY) == element && ClientState.isDiscovered(element)) {
                graphics.fill(x, y - 1, x + TerminalScreen.CELL_W, y + TerminalScreen.CELL_H - 2, 0x402E8F7E);
            }
            TechGui.badge(graphics, font, x + (TerminalScreen.CELL_W - 16) / 2, y, element, !in);
            if (in) {
                String text = limits.get(element) == 0 ? "∞" : TechGui.shortAmount(limits.get(element));
                TechGui.smallText(graphics, font, text, x + (TerminalScreen.CELL_W - TechGui.smallWidth(font, text)) / 2, y + 18,
                        TechGui.TEXT_GOOD, false);
            }
        }

        int y = top + TerminalScreen.STORAGE_Y + 7;
        if (selected != null && limits.containsKey(selected)) {
            Component label = Component.translatable("gui.atomcraft.partition_limit_of", ClientState.symbol(selected));
            graphics.text(font, label, left + 146 - font.width(label), y, TechGui.TEXT_LIGHT, false);
        } else {
            Component info = Component.translatable(limits.isEmpty() ? "gui.atomcraft.partition_none" : "gui.atomcraft.partition_count", limits.size());
            graphics.text(font, info, left + 12, y, limits.isEmpty() ? TechGui.TEXT_DIM : TechGui.TEXT_TEAL, false);
        }
        boolean hoverClear = mouseX >= clearX() && mouseX < clearX() + CLEAR_W && mouseY >= clearY() && mouseY < clearY() + CLEAR_H;
        TechGui.button(graphics, font, clearX(), clearY(), CLEAR_W, CLEAR_H, Component.translatable("gui.atomcraft.partition_clear").getString(),
                hoverClear, !limits.isEmpty());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        ChemicalElement element = hovered(mouseX, mouseY);
        if (element != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(TechGui.elementName(element));
            if (!ClientState.isDiscovered(element)) {
                lines.add(Component.translatable("gui.atomcraft.unknown_hint"));
            } else {
                lines.add(Component.translatable(limits.containsKey(element)
                        ? (element == selected ? "gui.atomcraft.partition_click_remove" : "gui.atomcraft.partition_click_edit")
                        : "gui.atomcraft.partition_click_add"));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        } else if (limitBox != null && limitBox.visible && limitBox.isMouseOver(mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.atomcraft.partition_limit_hint")), mouseX, mouseY);
        }
    }

    private @Nullable ChemicalElement hovered(double mouseX, double mouseY) {
        for (ChemicalElement element : ChemicalElement.values()) {
            int[] pos = TerminalScreen.cell(element);
            int x = left + pos[0], y = top + pos[1];
            if (mouseX >= x && mouseX < x + TerminalScreen.CELL_W && mouseY >= y && mouseY < y + TerminalScreen.CELL_H - 1) return element;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            ChemicalElement element = hovered(event.x(), event.y());
            if (element != null && ClientState.isDiscovered(element)) {
                click();
                if (!limits.containsKey(element)) {
                    limits.put(element, 0L);
                    selected = element;
                } else if (element == selected) {
                    limits.remove(element);
                    selected = null;
                } else {
                    selected = element;
                }
                updateBox();
                return true;
            }
            if (!limits.isEmpty() && event.x() >= clearX() && event.x() < clearX() + CLEAR_W
                    && event.y() >= clearY() && event.y() < clearY() + CLEAR_H) {
                click();
                limits.clear();
                selected = null;
                updateBox();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    /** Ao fechar, a partição vai para o servidor. */
    @Override
    public void removed() {
        super.removed();
        ClientPacketDistributor.sendToServer(new PartitionPayload(hand == InteractionHand.OFF_HAND, DiskPartition.of(limits)));
    }
}
