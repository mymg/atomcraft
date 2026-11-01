package com.atomcraft.synthesis;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.registry.ModItems;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Receitas do Sintetizador. Cada uma só fica disponível com a schematic na biblioteca. */
public final class SynthesisRecipes {

    private static @Nullable Map<Item, SynthesisRecipe> recipes;

    private SynthesisRecipes() {}

    private static Map<Item, SynthesisRecipe> all() {
        if (recipes == null) {
            Map<Item, SynthesisRecipe> map = new LinkedHashMap<>();
            add(map, ModItems.SPEED_UPGRADE.get(), 30_000, 200,
                    ChemicalElement.SILICON, 32, ChemicalElement.GOLD, 8, ChemicalElement.REDSTONIUM, 24, ChemicalElement.COPPER, 8);
            add(map, ModItems.EFFICIENCY_UPGRADE.get(), 30_000, 200,
                    ChemicalElement.SILICON, 32, ChemicalElement.GOLD, 8, ChemicalElement.LUMINIUM, 16, ChemicalElement.COPPER, 8);
            recipes = map;
        }
        return recipes;
    }

    private static void add(Map<Item, SynthesisRecipe> map, Item result, long energy, int ticks, Object... cost) {
        Map<ChemicalElement, Long> elements = new EnumMap<>(ChemicalElement.class);
        for (int i = 0; i < cost.length; i += 2) elements.put((ChemicalElement) cost[i], ((Integer) cost[i + 1]).longValue());
        map.put(result, new SynthesisRecipe(result, new Composition(elements), energy, ticks));
    }

    public static @Nullable SynthesisRecipe get(Item result) {
        return all().get(result);
    }
}
