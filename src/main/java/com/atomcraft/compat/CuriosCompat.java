package com.atomcraft.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/** Chamadas à API do Curios. Só é carregada quando o Curios está instalado (ver {@link Curios}). */
final class CuriosCompat {

    private CuriosCompat() {}

    static List<ItemStack> equipped(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).map(inventory -> {
            var handler = inventory.getEquippedCurios();
            List<ItemStack> stacks = new ArrayList<>(handler.getSlots());
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) stacks.add(stack);
            }
            return stacks;
        }).orElse(List.of());
    }

    static boolean equip(LivingEntity entity, String slot, ItemStack stack) {
        return CuriosApi.getCuriosInventory(entity).map(inventory -> {
            // Jogadores falsos (autoteste) não passam pelo login, que é quando o Curios monta os slots.
            if (inventory.getStacksHandler(slot).isEmpty()) inventory.loadDatapacks();
            if (inventory.getStacksHandler(slot).isEmpty()) return false;
            inventory.setEquippedCurio(slot, 0, stack);
            return true;
        }).orElse(false);
    }

    static boolean hasPlayerSlot(Level level, String slot) {
        return CuriosApi.getPlayerSlots(level).containsKey(slot);
    }
}
