package com.atomcraft.chemistry;

import com.atomcraft.Config;
import net.minecraft.world.item.Item;

/**
 * Valor combustível de um item, com base na química: carbono e hidrogênio
 * queimam; o oxigênio já presente na molécula reduz a energia liberada
 * (a madeira, parcialmente oxidada, rende menos que o carvão).
 * <p>
 * unidades = C + H/4 − O
 */
public final class Fuel {

    private Fuel() {}

    public static double units(Composition composition) {
        double units = composition.get(ChemicalElement.CARBON)
                + composition.get(ChemicalElement.HYDROGEN) / 4.0
                - composition.get(ChemicalElement.OXYGEN);
        return Math.max(0, units);
    }

    /** FE liberado ao queimar uma unidade do item no Gerador de Combustão (0 se não queima). */
    public static long energy(Item item, boolean clientSide) {
        AtomicValue value = AtomicValues.get(item, clientSide);
        if (value == null || value.composition().get(ChemicalElement.CARBON) <= 0) return 0;
        return (long) Math.floor(units(value.composition()) * Config.COMBUSTION_FE_PER_UNIT.get());
    }
}
