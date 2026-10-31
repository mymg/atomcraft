package com.atomcraft.client.screen;

import com.atomcraft.client.ClientState;
import com.atomcraft.menu.BaseMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Base das interfaces: painel, fundo dos slots e barra de armazenamento da rede. */
public abstract class MachineScreen<M extends BaseMenu> extends AbstractContainerScreen<M> {

    protected MachineScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title);
        this.imageWidth = width;
        this.imageHeight = height;
        this.inventoryLabelY = height - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiUtil.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) GuiUtil.slot(graphics, leftPos + slot.x, topPos + slot.y);
        extractMachine(graphics, mouseX, mouseY);
    }

    /** Desenho específico de cada máquina (coordenadas absolutas da tela). */
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    /** Barra de ocupação da rede de elementos. */
    protected void networkBar(GuiGraphicsExtractor graphics, int x, int y, int width) {
        long capacity = ClientState.networkCapacity();
        float fraction = capacity <= 0 ? 0 : (float) ClientState.networkUsed() / capacity;
        GuiUtil.horizontalBar(graphics, x, y, width, 6, fraction, 0xFF3A7BD5);
    }

    protected Component networkLabel() {
        long capacity = ClientState.networkCapacity();
        if (capacity <= 0) return Component.translatable("gui.atomcraft.no_storage");
        return Component.translatable("gui.atomcraft.storage",
                GuiUtil.formatAmount(ClientState.networkUsed()), GuiUtil.formatAmount(capacity));
    }
}
