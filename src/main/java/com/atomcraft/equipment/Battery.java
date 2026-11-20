package com.atomcraft.equipment;

import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.atomcraft.registry.ModComponents;
import net.minecraft.world.item.ItemStack;

/** Energia guardada nos equipamentos (componente {@code stored_energy}) e recarga pelo Transmissor Sem Fio. */
public final class Battery {

    private Battery() {}

    public static int energy(ItemStack stack) {
        Integer value = stack.get(ModComponents.STORED_ENERGY.get());
        return value == null ? 0 : value;
    }

    public static void set(ItemStack stack, int energy, int capacity) {
        stack.set(ModComponents.STORED_ENERGY.get(), Math.max(0, Math.min(capacity, energy)));
    }

    /** Tira {@code amount} FE; falso (sem tirar nada) se não houver o bastante. */
    public static boolean use(ItemStack stack, int amount, int capacity) {
        if (amount <= 0) return true;
        int energy = energy(stack);
        if (energy < amount) return false;
        set(stack, energy - amount, capacity);
        return true;
    }

    /** Passa até {@code max} FE do transmissor para o item. Devolve quanto passou. */
    public static int charge(ItemStack stack, int capacity, WirelessTransmitterBlockEntity transmitter, int max) {
        int amount = Math.min(Math.min(capacity - energy(stack), max), transmitter.energy().getAmountAsInt());
        if (amount <= 0) return 0;
        transmitter.energy().set(transmitter.energy().getAmountAsInt() - amount);
        set(stack, energy(stack) + amount, capacity);
        return amount;
    }

    public static int barWidth(ItemStack stack, int capacity) {
        return Math.round(13f * energy(stack) / capacity);
    }
}
