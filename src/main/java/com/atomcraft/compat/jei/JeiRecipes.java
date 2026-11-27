package com.atomcraft.compat.jei;

import com.atomcraft.alloy.AlloyRecipe;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.synthesis.SynthesisRecipe;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.item.Item;

/** Tipos de receita das categorias do AtomCraft no JEI e os registros que não existem no mod. */
public final class JeiRecipes {

    /** Gerador de Combustão: item → FE. */
    public record Fuel(Item item, long energy) {}

    /** Atomizador: item (ou 1 balde de fluido) → elementos. */
    public record Atomize(Item item, net.minecraft.world.level.material.@org.jspecify.annotations.Nullable Fluid fluid, Composition elements) {}

    public static final IRecipeType<AlloyRecipe> ALLOY = IRecipeType.create("atomcraft", "alloy_forge", AlloyRecipe.class);
    public static final IRecipeType<SynthesisRecipe> SYNTHESIS = IRecipeType.create("atomcraft", "synthesizer", SynthesisRecipe.class);
    public static final IRecipeType<Fuel> FUEL = IRecipeType.create("atomcraft", "combustion_fuel", Fuel.class);
    public static final IRecipeType<Atomize> ATOMIZE = IRecipeType.create("atomcraft", "atomizer", Atomize.class);

    private JeiRecipes() {}
}
