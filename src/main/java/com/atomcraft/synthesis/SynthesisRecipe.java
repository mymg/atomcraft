package com.atomcraft.synthesis;

import com.atomcraft.chemistry.Composition;
import net.minecraft.world.item.Item;

/** Receita do Sintetizador: elementos da rede + energia → item exclusivo. */
public record SynthesisRecipe(Item result, Composition cost, long energy, int ticks) {}
