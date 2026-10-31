package com.atomcraft.chemistry;

import com.atomcraft.AtomCraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula o valor atômico dos itens a partir das receitas.
 * <p>
 * Exemplo: o bloco de ferro (9 barras) vale Fe×81 e a pepita (1 barra → 9 pepitas)
 * vale Fe×1. O valor de um resultado é a soma dos ingredientes dividida pela
 * quantidade produzida, arredondando para baixo. Isso garante que atomizar um item
 * nunca rende mais do que os ingredientes usados para fabricá-lo.
 * <p>
 * Itens devolvidos na receita (ex.: baldes) são descontados. Quando há várias
 * receitas para o mesmo item, vale a mais barata. A raridade é a maior entre os
 * ingredientes e a raridade vanilla do resultado.
 */
public final class RecipeDeriver {

    private static final int MAX_PASSES = 32;

    private record Candidate(Item output, int count, List<List<ItemStack>> inputs) {}

    private RecipeDeriver() {}

    public static Map<Item, AtomicValue> derive(MinecraftServer server, Map<Item, AtomicValue> base) {
        Map<Item, AtomicValue> values = new HashMap<>(base);
        ContextMap context = SlotDisplayContext.fromLevel(server.overworld());
        List<Candidate> candidates = new ArrayList<>();

        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            List<RecipeDisplay> displays;
            try {
                displays = holder.value().display();
            } catch (RuntimeException e) {
                continue;
            }
            for (RecipeDisplay display : displays) {
                List<SlotDisplay> inputs = inputsOf(display);
                if (inputs == null) continue;
                ItemStack result = display.result().resolveForFirstStack(context);
                if (result.isEmpty() || base.containsKey(result.getItem())) continue;

                List<List<ItemStack>> resolved = new ArrayList<>();
                for (SlotDisplay input : inputs) {
                    List<ItemStack> options = input.resolveForStacks(context);
                    if (!options.isEmpty()) resolved.add(options);
                }
                if (!resolved.isEmpty()) candidates.add(new Candidate(result.getItem(), result.getCount(), resolved));
            }
        }

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            boolean changed = false;
            for (Candidate candidate : candidates) {
                AtomicValue derived = evaluate(candidate, values);
                if (derived == null) continue;
                AtomicValue current = values.get(candidate.output());
                if (current == null || derived.composition().totalAtoms() < current.composition().totalAtoms()) {
                    values.put(candidate.output(), derived);
                    changed = true;
                }
            }
            if (!changed) break;
        }
        AtomCraft.LOGGER.debug("AtomCraft: {} receitas analisadas", candidates.size());
        return values;
    }

    private static @Nullable List<SlotDisplay> inputsOf(RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) return shaped.ingredients();
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) return shapeless.ingredients();
        if (display instanceof FurnaceRecipeDisplay furnace) return List.of(furnace.ingredient());
        if (display instanceof StonecutterRecipeDisplay stonecutter) return List.of(stonecutter.input());
        if (display instanceof SmithingRecipeDisplay smithing) return List.of(smithing.template(), smithing.base(), smithing.addition());
        return null;
    }

    private static @Nullable AtomicValue evaluate(Candidate candidate, Map<Item, AtomicValue> values) {
        Composition sum = Composition.EMPTY;
        Rarity rarity = AtomicValues.vanillaRarity(candidate.output());

        for (List<ItemStack> options : candidate.inputs()) {
            ItemStack chosen = null;
            AtomicValue chosenValue = null;
            for (ItemStack option : options) {
                AtomicValue value = values.get(option.getItem());
                if (value == null || option.getItem() == candidate.output()) continue;
                if (chosenValue == null || value.composition().totalAtoms() < chosenValue.composition().totalAtoms()) {
                    chosen = option;
                    chosenValue = value;
                }
            }
            if (chosenValue == null) return null;

            Composition ingredient = chosenValue.composition().times(Math.max(1, chosen.getCount()));
            ItemStackTemplate remainder = chosen.getCraftingRemainder();
            if (remainder != null) {
                AtomicValue remainderValue = values.get(remainder.create().getItem());
                if (remainderValue != null) ingredient = ingredient.minusClamped(remainderValue.composition());
            }
            sum = sum.plus(ingredient);
            rarity = Rarity.max(rarity, chosenValue.rarity());
        }

        Composition result = sum.divideFloor(Math.max(1, candidate.count()));
        return result.isEmpty() ? null : new AtomicValue(result, rarity);
    }
}
