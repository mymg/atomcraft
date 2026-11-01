package com.atomcraft.client.screen;

import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.BaseMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Base das interfaces no tema tecnológico: moldura, aba de título, slots e
 * utilitários (barra de armazenamento, tooltips, cores de status).
 */
public abstract class MachineScreen<M extends BaseMenu> extends AbstractContainerScreen<M> {

    /** Altura da aba de título acima da moldura. */
    protected static final int TAB = 10;

    /** No modo vitrine do CI os tooltips ficam desligados, para não cobrir os prints. */
    private static final boolean SHOWCASE = System.getenv("ATOMCRAFT_SHOWCASE") != null;

    protected MachineScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
        this.inventoryLabelX = 9;
        this.inventoryLabelY = height - 93;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TechGui.frame(graphics, leftPos, topPos + TAB, imageWidth, imageHeight - TAB);
        TechGui.tab(graphics, font, leftPos + imageWidth / 2, topPos, title.getString());
        extractPanels(graphics, mouseX, mouseY);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (i < menu.machineSlotCount()) TechGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
            else TechGui.metalSlot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        extractMachine(graphics, mouseX, mouseY);
    }

    /** Painéis desenhados antes dos slots (coordenadas absolutas). */
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    /** Barras, ícones e textos desenhados depois dos slots (coordenadas absolutas). */
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    /** Tooltips das áreas da máquina. */
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TechGui.TEXT_LIGHT, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!SHOWCASE) extractTooltips(graphics, mouseX, mouseY);
    }

    protected boolean hovering(int mouseX, int mouseY, int x, int y, int w, int h) {
        int ax = leftPos + x, ay = topPos + y;
        return mouseX >= ax && mouseX < ax + w && mouseY >= ay && mouseY < ay + h;
    }

    protected void tooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Component... lines) {
        if (SHOWCASE) return;
        graphics.setComponentTooltipForNextFrame(font, List.of(lines), mouseX, mouseY);
    }

    /** Linha "Armazenamento [barra]" com o valor embaixo, como no mockup do Atomizador. Coordenadas relativas. */
    protected void storageRow(GuiGraphicsExtractor graphics, int x, int y, int right) {
        Component label = Component.translatable("gui.atomcraft.storage_label");
        graphics.text(font, label, leftPos + x, topPos + y + 1, TechGui.TEXT_LIGHT, false);
        int barX = x + font.width(label) + 4;
        long capacity = ClientState.networkCapacity();
        float fraction = capacity <= 0 ? 0 : (float) ClientState.networkUsed() / capacity;
        TechGui.horizontalBar(graphics, leftPos + barX, topPos + y, right - barX, 9, fraction, TechGui.STORAGE);
        String amount = storageText();
        graphics.text(font, amount, leftPos + right - font.width(amount), topPos + y + 11, TechGui.TEXT_DIM, false);
    }

    protected String storageText() {
        long capacity = ClientState.networkCapacity();
        if (capacity <= 0) return Component.translatable("gui.atomcraft.no_storage").getString();
        return GuiUtil.formatAmount(ClientState.networkUsed()) + "/" + GuiUtil.formatAmount(capacity);
    }

    protected Component storageTooltip() {
        return Component.translatable("gui.atomcraft.storage",
                String.format("%,d", ClientState.networkUsed()), String.format("%,d", ClientState.networkCapacity()));
    }

    /** Cores de status legíveis sobre o fundo escuro. */
    protected static int statusColor(MachineStatus status) {
        return switch (status) {
            case WORKING -> TechGui.TEXT_GOOD;
            case IDLE, NOTHING_TO_RESEARCH -> TechGui.TEXT_DIM;
            case NO_ENERGY, NO_ELEMENTS, OUTPUT_FULL -> 0xFFFFB347;
            default -> TechGui.TEXT_BAD;
        };
    }

    protected Component energyTooltip(int energy, int capacity) {
        return Component.translatable("gui.atomcraft.energy", String.format("%,d", energy), String.format("%,d", capacity));
    }
}
