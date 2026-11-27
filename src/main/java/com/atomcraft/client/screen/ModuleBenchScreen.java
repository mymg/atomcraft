package com.atomcraft.client.screen;

import com.atomcraft.equipment.Battery;
import com.atomcraft.equipment.EquipmentKind;
import com.atomcraft.equipment.EquipmentModule;
import com.atomcraft.equipment.EquipmentModuleItem;
import com.atomcraft.equipment.JetpackItem;
import com.atomcraft.equipment.ModularItem;
import com.atomcraft.menu.ModuleBenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Bancada de Módulos: equipamento à esquerda, slots de módulo à direita e a lista do que está instalado. */
public class ModuleBenchScreen extends MachineScreen<ModuleBenchMenu> {

    private static final int BENCH_BAR_X = 60, BENCH_BAR_Y = 25, BENCH_BAR_W = 6, BENCH_BAR_H = 74;

    public ModuleBenchScreen(ModuleBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ModuleBenchMenu.WIDTH, ModuleBenchMenu.HEIGHT);
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 8, topPos + 21, 62, 82);
        // Coluna da armadura vestida, à esquerda
        TechGui.frame(graphics, leftPos + ModuleBenchMenu.ARMOR_X - 6, topPos + ModuleBenchMenu.ARMOR_Y - 6, 27, 4 * 18 + 10);
        TechGui.glass(graphics, leftPos + 74, topPos + 21, 94, 82);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack equipment = menu.equipment();
        EquipmentKind kind = menu.kind();
        int left = leftPos + 12, right = leftPos + 78;
        // Energia da bancada (barra fina à direita do painel do equipamento)
        float benchFill = menu.capacity() <= 0 ? 0 : (float) menu.energy() / menu.capacity();
        TechGui.verticalBar(graphics, leftPos + BENCH_BAR_X, topPos + BENCH_BAR_Y, BENCH_BAR_W, BENCH_BAR_H, benchFill, TechGui.ENERGY);
        TechGui.smallText(graphics, font, Component.translatable("gui.atomcraft.equipment").getString(), left, topPos + 26, TechGui.TEXT_TEAL, false);
        TechGui.smallText(graphics, font, (kind == null ? Component.translatable("gui.atomcraft.augments") : kind.label()).getString(),
                right, topPos + 26, TechGui.TEXT_TEAL, false);

        // Slots de módulo que o equipamento não tem ficam riscados
        for (int i = 0; i < EquipmentKind.MAX_SLOTS; i++) {
            int x = leftPos + ModuleBenchMenu.MODULE_X + i * ModuleBenchMenu.MODULE_GAP, y = topPos + ModuleBenchMenu.MODULE_Y;
            if (kind != null && i < kind.slots()) continue;
            TechGui.slot(graphics, x, y);
            graphics.fill(x, y, x + 16, y + 16, 0xC0101418);
            graphics.fill(x + 3, y + 7, x + 13, y + 9, 0xFF3A4048);
        }

        if (kind == null) {
            drawLines(graphics, List.of(Component.translatable("gui.atomcraft.bench_hint_1").getString(),
                    Component.translatable("gui.atomcraft.bench_hint_2").getString(),
                    Component.translatable("gui.atomcraft.bench_hint_3").getString()), right, topPos + 66, TechGui.TEXT_DIM);
            return;
        }

        // Estado do equipamento: energia ou hidrogênio
        long amount = 0, capacity = 1;
        String unit = " FE";
        int color = TechGui.ENERGY;
        if (equipment.getItem() instanceof com.atomcraft.equipment.Powered powered) {
            amount = Battery.energy(equipment);
            capacity = powered.capacity();
        } else if (equipment.getItem() instanceof JetpackItem) {
            amount = JetpackItem.fuel(equipment);
            capacity = JetpackItem.capacity();
            unit = " H";
            color = 0xFFE8F0FF;
        }
        TechGui.horizontalBar(graphics, left, topPos + 66, 44, 7, (float) amount / capacity, color);
        TechGui.smallText(graphics, font, GuiUtil.formatAmount(amount) + unit, left, topPos + 76, TechGui.TEXT_LIGHT, false);
        TechGui.smallText(graphics, font, "/ " + GuiUtil.formatAmount(capacity), left, topPos + 84, TechGui.TEXT_DIM, false);

        List<String> installed = new ArrayList<>();
        for (ItemStack module : ModularItem.modules(equipment)) {
            if (module.getItem() instanceof EquipmentModuleItem item) {
                String name = item.module().label().getString();
                installed.add(module.getCount() > 1 ? name + " " + roman(module.getCount()) : name);
            }
        }
        if (installed.isEmpty()) installed.add(Component.translatable("gui.atomcraft.no_modules").getString());
        drawLines(graphics, installed, right, topPos + 66, installed.size() == 1 && ModularItem.modules(equipment).stream().allMatch(ItemStack::isEmpty)
                ? TechGui.TEXT_DIM : TechGui.TEXT_LIGHT);
    }

    private void drawLines(GuiGraphicsExtractor graphics, List<String> lines, int x, int y, int color) {
        for (String line : lines) {
            TechGui.smallText(graphics, font, line, x, y, color, false);
            y += 8;
        }
    }

    private static String roman(int level) {
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            default -> "I";
        };
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        if (mouseX >= left + ModuleBenchMenu.ARMOR_X - 6 && mouseX < left && mouseY >= top + ModuleBenchMenu.ARMOR_Y - 6
                && mouseY < top + ModuleBenchMenu.ARMOR_Y + 4 * 18 + 4) return false;
        return super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    @Override
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hovering(mouseX, mouseY, BENCH_BAR_X, BENCH_BAR_Y, BENCH_BAR_W, BENCH_BAR_H)) {
            tooltip(graphics, mouseX, mouseY, Component.translatable("gui.atomcraft.bench_energy"),
                    Component.literal(GuiUtil.formatAmount(menu.energy()) + " / " + GuiUtil.formatAmount(menu.capacity()) + " FE").withColor(TechGui.TEXT_LIGHT),
                    Component.translatable("gui.atomcraft.bench_energy_hint").withColor(TechGui.TEXT_DIM));
            return;
        }
        EquipmentKind kind = menu.kind();
        if (kind == null || !menu.getCarried().isEmpty()) return;
        for (int i = 0; i < kind.slots(); i++) {
            int x = ModuleBenchMenu.MODULE_X + i * ModuleBenchMenu.MODULE_GAP;
            if (!hovering(mouseX, mouseY, x, ModuleBenchMenu.MODULE_Y, 18, 18) || menu.slots.get(1 + i).hasItem()) continue;
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.atomcraft.module_slot"));
            for (EquipmentModule module : EquipmentModule.values()) {
                if (module.fits(kind)) lines.add(Component.literal("• ").append(module.label()).withColor(TechGui.TEXT_DIM));
            }
            tooltip(graphics, mouseX, mouseY, lines.toArray(Component[]::new));
        }
    }
}
