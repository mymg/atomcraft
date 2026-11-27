package com.atomcraft.equipment;

import net.minecraft.world.item.ItemStack;

/** Item com bateria (componente {@code stored_energy}): Exoesqueleto, Armadura Nano, Katana. */
public interface Powered {

    int capacity();

    /** Capacidade da bateria do item (0 se não tiver bateria). */
    static int capacity(ItemStack stack) {
        return stack.getItem() instanceof Powered powered ? powered.capacity() : 0;
    }
}
