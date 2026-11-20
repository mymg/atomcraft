package com.atomcraft.equipment;

import com.atomcraft.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;
import java.util.function.Consumer;

/** Item que aceita módulos. Os módulos ficam guardados no próprio item (componente {@code installed_modules}). */
public interface ModularItem {

    EquipmentKind kind();

    /** Módulos instalados, um por slot ({@link EquipmentKind#MAX_SLOTS} posições, vazias quando não há módulo). */
    static NonNullList<ItemStack> modules(ItemStack stack) {
        NonNullList<ItemStack> list = NonNullList.withSize(EquipmentKind.MAX_SLOTS, ItemStack.EMPTY);
        ItemContainerContents contents = stack.get(ModComponents.INSTALLED_MODULES.get());
        if (contents != null) contents.copyInto(list);
        return list;
    }

    static void setModules(ItemStack stack, List<ItemStack> modules) {
        if (modules.stream().allMatch(ItemStack::isEmpty)) stack.remove(ModComponents.INSTALLED_MODULES.get());
        else stack.set(ModComponents.INSTALLED_MODULES.get(), ItemContainerContents.fromItems(modules));
    }

    /** Nível instalado do módulo (0 se não tiver). */
    static int level(ItemStack stack, EquipmentModule module) {
        if (!(stack.getItem() instanceof ModularItem)) return 0;
        int level = 0;
        for (ItemStack installed : modules(stack)) {
            if (installed.getItem() instanceof EquipmentModuleItem item && item.module() == module) level += installed.getCount();
        }
        return Math.min(level, module.maxLevel());
    }

    /** Linhas do tooltip com os módulos instalados. */
    static void appendModules(ItemStack stack, EquipmentKind kind, Consumer<Component> tooltip) {
        int used = 0;
        for (ItemStack installed : modules(stack)) {
            if (installed.getItem() instanceof EquipmentModuleItem item) {
                used++;
                Component name = item.module().label();
                tooltip.accept(Component.literal(" • ").append(installed.getCount() > 1
                        ? Component.translatable("tooltip.atomcraft.module_level", name, installed.getCount()) : name)
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        tooltip.accept(Component.translatable("tooltip.atomcraft.module_slots", used, kind.slots()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
