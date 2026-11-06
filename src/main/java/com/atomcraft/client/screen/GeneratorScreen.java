package com.atomcraft.client.screen;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Fuel;
import com.atomcraft.block.MachineBlock;
import com.atomcraft.client.ClientState;
import com.atomcraft.client.FusionPreview;
import com.atomcraft.fusion.FusionStructure;
import com.atomcraft.fusion.ReactorPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import com.atomcraft.menu.GeneratorMenu;
import com.atomcraft.registry.ModMenus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Interface dos geradores. Mesmo layout do Atomizador: energia à esquerda,
 * painel de vidro no centro e uma barra à direita (queima ou temperatura).
 */
public class GeneratorScreen extends MachineScreen<GeneratorMenu> {

    private static final int BURN = 0xFFFF8A1F, HEAT = 0xFFFF4FA3;

    private enum Kind { COMBUSTION, CARBON, FUSION }

    private final Kind kind;

    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GeneratorMenu.WIDTH, GeneratorMenu.HEIGHT);
        this.kind = menu.fuel() != null ? Kind.COMBUSTION
                : menu.getType() == ModMenus.FUSION_REACTOR.get() ? Kind.FUSION : Kind.CARBON;
    }

    private boolean hasRightBar() {
        return kind != Kind.CARBON;
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 22, topPos + 21, hasRightBar() ? 133 : 147, 60);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        float energy = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + 9, topPos + 21, 11, 60, energy, TechGui.ENERGY);

        int x = leftPos + 28, y = topPos + 26;
        switch (kind) {
            case COMBUSTION -> {
                TechGui.verticalBar(graphics, leftPos + 158, topPos + 21, 11, 60, menu.burn(), BURN);
                int tx = leftPos + 58;
                ItemStack fuel = menu.slots.getFirst().getItem();
                graphics.text(font, Component.translatable("gui.atomcraft.fuel"), tx, y + 2, TechGui.TEXT_TEAL, false);
                String value = fuel.isEmpty() ? "-" : GuiUtil.formatAmount(Fuel.energy(fuel.getItem(), true)) + " FE";
                graphics.text(font, Component.translatable("gui.atomcraft.fuel_value", value), tx, y + 14, TechGui.TEXT_LIGHT, false);
                graphics.text(font, generatingText(), tx, y + 26, TechGui.TEXT_LIGHT, false);
                graphics.text(font, storedText(), tx, y + 38, TechGui.TEXT_DIM, false);
            }
            case CARBON -> {
                TechGui.badge(graphics, font, x, y, ChemicalElement.CARBON);
                graphics.text(font, Component.translatable("gui.atomcraft.in_network",
                        GuiUtil.formatAmount(ClientState.networkTotals().get(ChemicalElement.CARBON))), x + 20, y + 4, TechGui.TEXT_TEAL, false);
                graphics.text(font, generatingText(), x, y + 24, TechGui.TEXT_LIGHT, false);
                graphics.text(font, storedText(), x, y + 36, TechGui.TEXT_DIM, false);
            }
            case FUSION -> {
                TechGui.verticalBar(graphics, leftPos + 158, topPos + 21, 11, 60, menu.heat(), HEAT);
                if (!menu.formed()) {
                    extractIncomplete(graphics, mouseX, mouseY, x, y);
                    break;
                }
                TechGui.badge(graphics, font, x, y, ChemicalElement.HYDROGEN);
                graphics.text(font, "×4 →", x + 19, y + 4, TechGui.TEXT_LIGHT, false);
                TechGui.badge(graphics, font, x + 46, y, ChemicalElement.HELIUM);
                graphics.text(font, GuiUtil.formatAmount(ClientState.networkTotals().get(ChemicalElement.HYDROGEN)) + " H",
                        x + 68, y + 4, TechGui.TEXT_TEAL, false);
                graphics.text(font, Component.translatable("gui.atomcraft.heat", Math.round(menu.heat() * 100)),
                        x, y + 22, TechGui.TEXT_LIGHT, false);
                graphics.text(font, generatingText(), x, y + 33, TechGui.TEXT_LIGHT, false);
                graphics.text(font, Component.translatable("gui.atomcraft.helium_produced", GuiUtil.formatAmount(menu.heliumProduced())),
                        x, y + 44, TechGui.TEXT_DIM, false);
            }
        }

        if (kind == Kind.COMBUSTION) {
            graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 158), leftPos + 9, topPos + 87,
                    statusColor(menu.status()), false);
        } else {
            storageRow(graphics, 9, 85, 169);
            graphics.text(font, font.plainSubstrByWidth(menu.status().label().getString(), 100), leftPos + 9, topPos + 96,
                    statusColor(menu.status()), false);
        }
    }

    // ----- Reator de Fusão: estrutura incompleta -----

    private static final int PREVIEW_W = 120, PREVIEW_H = 13;
    private Map<ReactorPart, Integer> missing = Map.of();
    private long missingCheckedAt = -100;

    private Map<ReactorPart, Integer> missingParts() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return Map.of();
        if (level.getGameTime() - missingCheckedAt >= 10) {
            missingCheckedAt = level.getGameTime();
            BlockState state = level.getBlockState(menu.pos());
            missing = state.hasProperty(MachineBlock.FACING)
                    ? FusionStructure.check(level, menu.pos(), state.getValue(MachineBlock.FACING)).missingCounts() : Map.of();
        }
        return missing;
    }

    private int previewX() {
        return leftPos + 28;
    }

    private int previewY() {
        return topPos + 66;
    }

    private void extractIncomplete(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y) {
        Map<ReactorPart, Integer> parts = missingParts();
        int total = parts.values().stream().mapToInt(Integer::intValue).sum();
        graphics.text(font, Component.translatable("gui.atomcraft.structure_missing", total), x, y, TechGui.TEXT_BAD, false);
        int line = y + 11, shown = 0;
        for (var entry : parts.entrySet()) {
            if (shown == 2 && parts.size() > 3) {
                graphics.text(font, Component.translatable("gui.atomcraft.structure_more", parts.size() - 2), x, line, TechGui.TEXT_DIM, false);
                break;
            }
            Component name = Component.translatable(entry.getKey().translationKey());
            String text = font.plainSubstrByWidth(entry.getValue() + "× " + name.getString(), 124);
            graphics.text(font, text, x, line, TechGui.TEXT_LIGHT, false);
            line += 10;
            shown++;
        }
        boolean hover = mouseX >= previewX() && mouseX < previewX() + PREVIEW_W && mouseY >= previewY() && mouseY < previewY() + PREVIEW_H;
        TechGui.button(graphics, font, previewX(), previewY(), PREVIEW_W, PREVIEW_H,
                Component.translatable(FusionPreview.isShowing(menu.pos()) ? "gui.atomcraft.hide_structure" : "gui.atomcraft.show_structure").getString(),
                hover, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (kind == Kind.FUSION && !menu.formed() && event.button() == 0
                && event.x() >= previewX() && event.x() < previewX() + PREVIEW_W
                && event.y() >= previewY() && event.y() < previewY() + PREVIEW_H) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            FusionPreview.toggle(menu.pos());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private Component generatingText() {
        return Component.translatable("gui.atomcraft.generating", String.format("%,d", menu.generating()));
    }

    private Component storedText() {
        return Component.literal(GuiUtil.formatAmount(menu.energy()) + " / " + GuiUtil.formatAmount(menu.capacity()) + " FE");
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hovering(mouseX, mouseY, 9, 21, 11, 60)) {
            tooltip(graphics, mouseX, mouseY, energyTooltip(menu.energy(), menu.capacity()));
        } else if (hasRightBar() && hovering(mouseX, mouseY, 158, 21, 11, 60)) {
            if (kind == Kind.COMBUSTION) {
                tooltip(graphics, mouseX, mouseY, Component.translatable("gui.atomcraft.burn", Math.round(menu.burn() * 100)));
            } else {
                tooltip(graphics, mouseX, mouseY, Component.translatable("gui.atomcraft.heat", Math.round(menu.heat() * 100)),
                        Component.translatable("gui.atomcraft.heat_hint"));
            }
        } else if (kind != Kind.COMBUSTION && hovering(mouseX, mouseY, 9, 85, 160, 20)) {
            tooltip(graphics, mouseX, mouseY, storageTooltip());
        }
    }
}
