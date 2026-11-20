package com.atomcraft.client.screen;

import com.atomcraft.equipment.Battery;
import com.atomcraft.equipment.EquipmentKind;
import com.atomcraft.equipment.EquipmentModule;
import com.atomcraft.equipment.EquipmentModuleItem;
import com.atomcraft.equipment.ExoArmorItem;
import com.atomcraft.equipment.JetpackItem;
import com.atomcraft.equipment.ModularItem;
import com.atomcraft.menu.ModuleBenchMenu;
import com.atomcraft.tools.AtomicDisintegratorItem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Bancada de Módulos: equipamento à esquerda, slots de módulo à direita e a lista do que está instalado. */
public class ModuleBenchScreen extends MachineScreen<ModuleBenchMenu> {

    public ModuleBenchScreen(ModuleBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ModuleBenchMenu.WIDTH, ModuleBenchMenu.HEIGHT);
    }

    @Override
    protected void extractPanels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TechGui.glass(graphics, leftPos + 8, topPos + 21, 62, 82);
        TechGui.glass(graphics, leftPos + 74, topPos + 21, 94, 82);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack equipment = menu.equipment();
        EquipmentKind kind = menu.kind();
        int left = leftPos + 12, right = leftPos + 78;
        TechGui.smallText(graphics, font, Component.translatable("gui.atomcraft.equipment").getString(), left, topPos + 26, TechGui.TEXT_TEAL, false);
        TechGui.smallText(graphics, font, (kind == null ? Component.translatable("gui.atomcraft.modules") : kind.label()).getString(),
                right, topPos + 26, TechGui.TEXT_TEAL, false);

        // Slots de módulo que o equipamento não tem ficam riscados
        for (int i = 0; i < EquipmentKind.MAX_SLOTS; i++) {
            int x = leftPos + ModuleBenchMenu.MODULE_X + i * ModuleBenchMenu.MODULE_GAP, y = topPos + ModuleBenchMenu.MODULE_Y;
            if (kind != null && i < kind.slots()) continue;
            TechGui.slot(graphics, x, y);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xC0101418);
            graphics.fill(x + 4, y + 8, x + 14, y + 10, 0xFF3A4048);
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
        if (equipment.getItem() instanceof ExoArmorItem) {
            amount = Battery.energy(equipment);
            capacity = ExoArmorItem.CAPACITY;
        } else if (equipment.getItem() instanceof JetpackItem) {
            amount = JetpackItem.fuel(equipment);
            capacity = JetpackItem.capacity();
            unit = " H";
            color = 0xFFE8F0FF;
        } else if (equipment.getItem() instanceof AtomicDisintegratorItem) {
            amount = AtomicDisintegratorItem.energy(equipment);
            capacity = AtomicDisintegratorItem.CAPACITY;
        }
        TechGui.horizontalBar(graphics, left, topPos + 66, 54, 7, (float) amount / capacity, color);
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
    protected void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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
