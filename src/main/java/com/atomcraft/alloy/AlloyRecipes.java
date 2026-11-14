package com.atomcraft.alloy;

import com.atomcraft.AtomCraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Receitas da Forja de Ligas, lidas de {@code data/<namespace>/atomcraft/alloys/*.json}
 * (datapacks podem adicionar, trocar ou remover receitas). O servidor envia a lista
 * ao cliente ao entrar e após /reload.
 */
public class AlloyRecipes extends SimpleJsonResourceReloadListener<AlloyRecipe> {

    private static volatile List<AlloyRecipe> recipes = List.of();

    public AlloyRecipes() {
        super(AlloyRecipe.CODEC, FileToIdConverter.json("atomcraft/alloys"));
    }

    @Override
    protected void apply(Map<Identifier, AlloyRecipe> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        set(loaded.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Identifier, AlloyRecipe> e) -> e.getValue().order())
                        .thenComparing(e -> e.getKey().toString()))
                .map(Map.Entry::getValue).toList());
        AtomCraft.LOGGER.info("AtomCraft: {} receitas da Forja de Ligas", recipes.size());
    }

    /** Todas as receitas, na ordem da máquina. */
    public static List<AlloyRecipe> all() {
        return recipes;
    }

    /** Substitui a lista (no cliente, pelo que o servidor enviou). */
    public static void set(List<AlloyRecipe> list) {
        recipes = List.copyOf(list);
    }

    public static @Nullable AlloyRecipe get(Item result) {
        for (AlloyRecipe recipe : recipes) if (recipe.result() == result) return recipe;
        return null;
    }
}
