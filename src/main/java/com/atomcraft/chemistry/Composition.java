package com.atomcraft.chemistry;

import com.mojang.serialization.Codec;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Composição química de um item: quantos "átomos" (unidades) de cada elemento
 * são obtidos ao atomizá-lo, e quantos são necessários para recriá-lo.
 */
public record Composition(Map<ChemicalElement, Integer> elements) {

    public static final Codec<Composition> CODEC =
            Codec.unboundedMap(ChemicalElement.CODEC, Codec.intRange(1, Integer.MAX_VALUE))
                    .xmap(Composition::new, Composition::elements);

    public Composition {
        EnumMap<ChemicalElement, Integer> copy = new EnumMap<>(ChemicalElement.class);
        copy.putAll(elements);
        elements = Collections.unmodifiableMap(copy);
    }

    /** Fórmula legível, ex.: H2O ou C12H22O11. */
    public String formula() {
        StringBuilder sb = new StringBuilder();
        elements.forEach((element, amount) -> {
            sb.append(element.symbol());
            if (amount > 1) sb.append(amount);
        });
        return sb.toString();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
