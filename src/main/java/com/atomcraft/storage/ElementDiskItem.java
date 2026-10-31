package com.atomcraft.storage;

import com.atomcraft.chemistry.Composition;
import com.atomcraft.registry.ModComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Disco que guarda elementos químicos, como as células de armazenamento do
 * Applied Energistics. A capacidade é medida em átomos (soma de todos os elementos).
 * O conteúdo fica no componente {@link ModComponents#DISK_CONTENTS}, então o
 * disco mantém os elementos ao ser retirado do Drive.
 */
public class ElementDiskItem extends Item {

    private final long capacity;

    public ElementDiskItem(Properties properties, long capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public long capacity() {
        return capacity;
    }

    public static Composition contents(ItemStack stack) {
        return stack.getOrDefault(ModComponents.DISK_CONTENTS.get(), Composition.EMPTY);
    }
}
