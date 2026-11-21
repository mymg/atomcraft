package com.atomcraft.compat.jei;

import com.atomcraft.AtomCraft;
import com.atomcraft.alloy.AlloyRecipe;
import com.atomcraft.alloy.AlloyRecipes;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.Fuel;
import com.atomcraft.compat.JeiBridge;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.synthesis.SynthesisRecipe;
import com.atomcraft.synthesis.SynthesisRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Integração com o JEI (opcional): abas da Forja de Ligas, do Sintetizador, do combustível do Gerador de
 * Combustão e do Atomizador. "R" num item mostra como fazê-lo (Forja/Sintetizador); "U" mostra se ele
 * queima no gerador e quais elementos dá no Atomizador.
 * <p>
 * Os valores atômicos e as receitas da Forja chegam do servidor depois que o JEI monta as listas; quando
 * chegam ({@link JeiBridge#dataChanged()}), as receitas antigas são escondidas e as novas adicionadas.
 */
@JeiPlugin
public class AtomCraftJeiPlugin implements IModPlugin {

    private @Nullable IJeiRuntime runtime;
    private List<AlloyRecipe> shownAlloys = List.of();
    private List<JeiRecipes.Fuel> shownFuels = List.of();
    private List<JeiRecipes.Atomize> shownAtomize = List.of();

    @Override
    public Identifier getPluginUid() {
        return AtomCraft.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new Categories.Alloy(gui), new Categories.Synthesis(gui),
                new Categories.Fuel(gui), new Categories.Atomizer(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(JeiRecipes.SYNTHESIS, synthesis());
        shownAlloys = AlloyRecipes.all();
        shownFuels = fuels();
        shownAtomize = atomize();
        registration.addRecipes(JeiRecipes.ALLOY, shownAlloys);
        registration.addRecipes(JeiRecipes.FUEL, shownFuels);
        registration.addRecipes(JeiRecipes.ATOMIZE, shownAtomize);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(JeiRecipes.ALLOY, ModBlocks.ALLOY_FORGE.get());
        registration.addCraftingStation(JeiRecipes.SYNTHESIS, ModBlocks.SYNTHESIZER.get());
        registration.addCraftingStation(JeiRecipes.FUEL, ModBlocks.COMBUSTION_GENERATOR.get());
        registration.addCraftingStation(JeiRecipes.ATOMIZE, ModBlocks.ATOMIZER.get());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        JeiBridge.onDataChanged = this::refresh;
        JeiBridge.showCategory = this::show;
        refresh();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        JeiBridge.onDataChanged = () -> {};
        JeiBridge.showCategory = id -> {};
    }

    /** Troca as receitas que dependem de dados do servidor (valores atômicos e Forja). */
    private void refresh() {
        if (runtime == null) return;
        IRecipeManager manager = runtime.getRecipeManager();
        shownAlloys = replace(manager, JeiRecipes.ALLOY, shownAlloys, AlloyRecipes.all());
        shownFuels = replace(manager, JeiRecipes.FUEL, shownFuels, fuels());
        shownAtomize = replace(manager, JeiRecipes.ATOMIZE, shownAtomize, atomize());
    }

    private static <T> List<T> replace(IRecipeManager manager, IRecipeType<T> type, List<T> old, List<T> fresh) {
        if (old.equals(fresh)) return old;
        if (!old.isEmpty()) manager.hideRecipes(type, old);
        if (!fresh.isEmpty()) manager.addRecipes(type, fresh);
        return fresh;
    }

    private void show(String id) {
        if (runtime == null) return;
        IRecipeType<?> type = switch (id) {
            case "alloy_forge" -> JeiRecipes.ALLOY;
            case "synthesizer" -> JeiRecipes.SYNTHESIS;
            case "combustion_fuel" -> JeiRecipes.FUEL;
            default -> JeiRecipes.ATOMIZE;
        };
        runtime.getRecipesGui().showTypes(List.of(type));
    }

    private static List<SynthesisRecipe> synthesis() {
        List<SynthesisRecipe> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            SynthesisRecipe recipe = SynthesisRecipes.get(item);
            if (recipe != null) list.add(recipe);
        }
        return list;
    }

    private static List<Map.Entry<Item, AtomicValue>> values() {
        return AtomicValues.clientValues().entrySet().stream()
                .sorted(Comparator.comparing(e -> BuiltInRegistries.ITEM.getKey(e.getKey()).toString()))
                .toList();
    }

    private static List<JeiRecipes.Fuel> fuels() {
        List<JeiRecipes.Fuel> list = new ArrayList<>();
        for (var entry : values()) {
            long energy = Fuel.energy(entry.getKey(), true);
            if (energy > 0) list.add(new JeiRecipes.Fuel(entry.getKey(), energy));
        }
        list.sort(Comparator.comparingLong(JeiRecipes.Fuel::energy).reversed());
        return list;
    }

    private static List<JeiRecipes.Atomize> atomize() {
        List<JeiRecipes.Atomize> list = new ArrayList<>();
        for (var entry : values()) {
            if (!entry.getValue().composition().isEmpty()) list.add(new JeiRecipes.Atomize(entry.getKey(), entry.getValue().composition()));
        }
        return list;
    }
}
