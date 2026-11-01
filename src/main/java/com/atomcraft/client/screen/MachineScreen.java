package com.atomcraft.client.screen;

import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.client.ClientState;
import com.atomcraft.menu.BaseMenu;
import com.atomcraft.network.MachineTogglePayload;
import com.atomcraft.upgrade.UpgradeChipItem;
import com.atomcraft.upgrade.UpgradeType;
import com.atomcraft.upgrade.Upgrades;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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

    // ----- Abas laterais (ligar/desligar, informações, upgrades) -----

    public enum SidePanel { NONE, INFO, UPGRADES }

    private static final int TAB_SIZE = 20, TAB_POWER = 0, TAB_INFO = 1, TAB_UPGRADES = 2;
    private static final String[] ICON_POWER = {
            "....#....", ".#..#..#.", "#...#...#", "#...#...#", "#.......#", "#.......#", ".#.....#.", "..#####.."};
    private static final String[] ICON_INFO = {
            "...###...", "..#####..", "..#####..", "...###...", ".........", ".#######.", "#########", "#########"};
    private static final String[] ICON_UPGRADES = {
            ".#.#.#.#.", "#########", "##.....##", "##.###.##", "##.###.##", "##.....##", "#########", ".#.#.#.#."};

    /** Painel aberto, lembrado entre as máquinas. */
    private static SidePanel sidePanel = SidePanel.NONE;

    protected MachineScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
        this.inventoryLabelX = 9;
        this.inventoryLabelY = height - 93;
    }

    @Override
    protected void init() {
        super.init();
        menu.setUpgradesVisible(menu.hasMachineControls() && sidePanel == SidePanel.UPGRADES);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (menu.hasMachineControls()) extractSideTabs(graphics, mouseX, mouseY);
        TechGui.frame(graphics, leftPos, topPos + TAB, imageWidth, imageHeight - TAB);
        TechGui.tab(graphics, font, leftPos + imageWidth / 2, topPos, title.getString());
        extractPanels(graphics, mouseX, mouseY);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.isActive()) continue;
            if (i < menu.machineSlotCount()) TechGui.slot(graphics, leftPos + slot.x, topPos + slot.y);
            else TechGui.metalSlot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        if (menu.hasMachineControls()) extractSidePanelContents(graphics);
        extractMachine(graphics, mouseX, mouseY);
    }

    private SidePanel openPanel() {
        return menu.hasMachineControls() ? sidePanel : SidePanel.NONE;
    }

    private int tabX() {
        return leftPos + imageWidth - 3;
    }

    private int tabY(int index) {
        return topPos + TAB + 6 + index * (TAB_SIZE + 2);
    }

    private int panelX() {
        return leftPos + imageWidth + BaseMenu.TABS_W - 1;
    }

    private int panelY() {
        return topPos + TAB;
    }

    private int panelHeight() {
        return openPanel() == SidePanel.UPGRADES ? 74 : 56;
    }

    private int tabAt(double mouseX, double mouseY) {
        if (!menu.hasMachineControls()) return -1;
        for (int i = 0; i < 3; i++) {
            if (mouseX >= tabX() && mouseX < tabX() + TAB_SIZE + 2 && mouseY >= tabY(i) && mouseY < tabY(i) + TAB_SIZE) return i;
        }
        return -1;
    }

    private boolean insidePanel(double mouseX, double mouseY) {
        return openPanel() != SidePanel.NONE && mouseX >= panelX() && mouseX < panelX() + BaseMenu.PANEL_W
                && mouseY >= panelY() && mouseY < panelY() + panelHeight();
    }

    private void extractSideTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int hovered = tabAt(mouseX, mouseY);
        for (int i = 0; i < 3; i++) {
            int x = tabX(), y = tabY(i);
            boolean selected = (i == TAB_INFO && sidePanel == SidePanel.INFO) || (i == TAB_UPGRADES && sidePanel == SidePanel.UPGRADES);
            int w = TAB_SIZE + 2;
            graphics.fill(x, y, x + w, y + TAB_SIZE, TechGui.OUT);
            graphics.fill(x, y + 1, x + w - 1, y + TAB_SIZE - 1, selected ? TechGui.SELECTED : hovered == i ? TechGui.METAL_LIGHT : TechGui.METAL);
            graphics.fill(x, y + 1, x + w - 1, y + 2, TechGui.METAL_HI);
            graphics.fill(x + w - 2, y + 1, x + w - 1, y + TAB_SIZE - 1, TechGui.METAL_SHADOW);
            String[] icon = i == TAB_POWER ? ICON_POWER : i == TAB_INFO ? ICON_INFO : ICON_UPGRADES;
            int color = i == TAB_POWER ? (menu.isEnabled() ? TechGui.TEXT_GOOD : TechGui.ENERGY) : TechGui.TEXT_TEAL;
            pixelIcon(graphics, x + 7, y + 6, icon, color);
        }
        if (openPanel() != SidePanel.NONE) {
            TechGui.frame(graphics, panelX(), panelY(), BaseMenu.PANEL_W, panelHeight());
            TechGui.plainGlass(graphics, panelX() + 4, panelY() + 4, BaseMenu.PANEL_W - 8, panelHeight() - 8);
        }
    }

    private static void pixelIcon(GuiGraphicsExtractor graphics, int x, int y, String[] rows, int color) {
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                if (rows[row].charAt(col) == '#') graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
            }
        }
    }

    private void extractSidePanelContents(GuiGraphicsExtractor graphics) {
        int x = panelX() + 8, y = panelY() + 8;
        switch (openPanel()) {
            case INFO -> {
                graphics.text(font, Component.translatable("gui.atomcraft.owner"), x, y, TechGui.TEXT_TEAL, false);
                String owner = menu.ownerName().isEmpty() ? "-" : menu.ownerName();
                graphics.text(font, font.plainSubstrByWidth(owner, BaseMenu.PANEL_W - 16), x, y + 12, TechGui.TEXT_LIGHT, false);
                graphics.text(font, Component.translatable("gui.atomcraft.access_public"), x, y + 26, TechGui.TEXT_DIM, false);
            }
            case UPGRADES -> {
                graphics.text(font, Component.translatable("gui.atomcraft.upgrades"), x, y, TechGui.TEXT_TEAL, false);
                int speed = installedUpgrades(UpgradeType.SPEED), efficiency = installedUpgrades(UpgradeType.EFFICIENCY);
                graphics.text(font, Component.translatable("gui.atomcraft.upgrade_speed", speed), x, y + 40, TechGui.TEXT_LIGHT, false);
                graphics.text(font, Component.translatable("gui.atomcraft.upgrade_efficiency", efficiency), x, y + 51, TechGui.TEXT_LIGHT, false);
            }
            default -> {}
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int tab = event.button() == 0 ? tabAt(event.x(), event.y()) : -1;
        if (tab >= 0) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            switch (tab) {
                case TAB_POWER -> ClientPacketDistributor.sendToServer(new MachineTogglePayload(menu.pos()));
                case TAB_INFO -> sidePanel = sidePanel == SidePanel.INFO ? SidePanel.NONE : SidePanel.INFO;
                default -> sidePanel = sidePanel == SidePanel.UPGRADES ? SidePanel.NONE : SidePanel.UPGRADES;
            }
            menu.setUpgradesVisible(sidePanel == SidePanel.UPGRADES);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        if (tabAt(mouseX, mouseY) >= 0 || insidePanel(mouseX, mouseY)) return false;
        return super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    /** Chips de um tipo instalados na máquina aberta (até o limite por máquina). */
    protected int installedUpgrades(UpgradeType type) {
        int count = 0;
        for (int i = 0; i < menu.slots.size(); i++) {
            if (!menu.isUpgradeSlot(i)) continue;
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.getItem() instanceof UpgradeChipItem chip && chip.type() == type) count += stack.getCount();
        }
        return Math.min(Upgrades.MAX_PER_TYPE, count);
    }

    /** Usado pelo modo vitrine do CI para mostrar um painel nos prints. */
    public void showcasePanel(SidePanel panel) {
        sidePanel = menu.hasMachineControls() ? panel : SidePanel.NONE;
        menu.setUpgradesVisible(sidePanel == SidePanel.UPGRADES);
    }

    private void extractTabTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        switch (tabAt(mouseX, mouseY)) {
            case TAB_POWER -> tooltip(graphics, mouseX, mouseY, Component.translatable(menu.isEnabled()
                    ? "gui.atomcraft.power_on" : "gui.atomcraft.power_off"), Component.translatable("gui.atomcraft.power_hint"));
            case TAB_INFO -> tooltip(graphics, mouseX, mouseY, Component.translatable("gui.atomcraft.info"));
            case TAB_UPGRADES -> tooltip(graphics, mouseX, mouseY, Component.translatable("gui.atomcraft.upgrades"),
                    Component.translatable("gui.atomcraft.upgrades_hint"));
            default -> {}
        }
    }

    /** Painéis desenhados antes dos slots (coordenadas absolutas). */
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    /** Barras, ícones e textos desenhados depois dos slots (coordenadas absolutas). */
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    /** Tooltips das áreas da máquina. */
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    /** Falso nas interfaces sem inventário do jogador (geradores compactos, terminal). */
    protected boolean showsInventory() {
        return true;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!showsInventory()) return;
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TechGui.TEXT_LIGHT, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!SHOWCASE) {
            extractTooltips(graphics, mouseX, mouseY);
            extractTabTooltips(graphics, mouseX, mouseY);
        }
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
            case NO_ENERGY, NO_ELEMENTS, OUTPUT_FULL, NO_FUEL -> 0xFFFFB347;
            default -> TechGui.TEXT_BAD;
        };
    }

    protected Component energyTooltip(int energy, int capacity) {
        return Component.translatable("gui.atomcraft.energy", String.format("%,d", energy), String.format("%,d", capacity));
    }
}
