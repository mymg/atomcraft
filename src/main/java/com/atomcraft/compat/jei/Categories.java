package com.atomcraft.compat.jei;

import com.atomcraft.alloy.AlloyRecipe;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.client.screen.GuiUtil;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.synthesis.SynthesisRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/** Categorias do AtomCraft no JEI. */
public final class Categories {

    private static final int TEXT = 0xFF404850, TEXT_DIM = 0xFF707880;

    private Categories() {}

    /** Base: elementos desenhados como fichas, com o nome no tooltip. */
    abstract static class ElementCategory<T> extends AbstractRecipeCategory<T> {

        ElementCategory(IRecipeType<T> type, String key, IGuiHelper gui, ItemLike icon, int width, int height) {
            super(type, Component.translatable("jei.atomcraft." + key), gui.createDrawableItemLike(icon), width, height);
        }

        /** Elementos da receita e onde ficam (x, y, largura, linhas). */
        abstract Composition elements(T recipe);

        abstract int[] area();

        List<ElementTiles.Tile> tiles(T recipe) {
            int[] a = area();
            return ElementTiles.layout(elements(recipe), a[0], a[1], a[2], a[3]);
        }

        /**
         * Cada elemento vira um slot do JEI (dá para usar R/U nele). O ícone de um elemento ainda não
         * descoberto é desenhado como "?" e o tooltip não diz o nome.
         */
        void addElementSlots(IRecipeLayoutBuilder builder, T recipe, boolean output) {
            for (ElementTiles.Tile tile : tiles(recipe)) {
                (output ? builder.addOutputSlot(tile.x(), tile.y()) : builder.addInputSlot(tile.x(), tile.y()))
                        .add(ElementIngredient.TYPE, tile.element());
            }
        }

        @Override
        public void draw(T recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            List<ElementTiles.Tile> tiles = tiles(recipe);
            ElementTiles.draw(graphics, tiles, elements(recipe).elements().size() - tiles.size());
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            ElementTiles.Tile tile = ElementTiles.at(tiles(recipe), mouseX, mouseY);
            if (tile != null && mouseX >= tile.x() + 16) {
                tooltip.add(ElementTiles.name(tile));
            }
        }

        static void text(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
            graphics.text(Minecraft.getInstance().font, text, x, y, color, false);
        }

        static Component energyAndTime(long energy, int ticks) {
            return Component.translatable("jei.atomcraft.energy_time", GuiUtil.formatAmount(energy), String.format("%.1f", ticks / 20.0));
        }
    }

    /** Forja de Ligas: elementos + energia → liga. */
    static final class Alloy extends ElementCategory<AlloyRecipe> {
        Alloy(IGuiHelper gui) {
            super(JeiRecipes.ALLOY, "alloy_forge", gui, ModBlocks.ALLOY_FORGE.get(), 160, 56);
        }

        @Override
        Composition elements(AlloyRecipe recipe) {
            return recipe.elements();
        }

        @Override
        int[] area() {
            return new int[]{0, 2, 100, 2};
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, AlloyRecipe recipe, IFocusGroup focuses) {
            builder.addOutputSlot(138, 14).setOutputSlotBackground().add(new net.minecraft.world.item.ItemStack(recipe.result(), recipe.count()));
            addElementSlots(builder, recipe, false);
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, AlloyRecipe recipe, IFocusGroup focuses) {
            builder.addAnimatedRecipeArrow(recipe.ticks()).setPosition(106, 14);
        }

        @Override
        public void draw(AlloyRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            super.draw(recipe, slots, graphics, mouseX, mouseY);
            text(graphics, energyAndTime(recipe.energy(), recipe.ticks()), 0, 46, TEXT_DIM);
        }
    }

    /** Sintetizador: elementos + energia → item exclusivo (precisa da schematic na biblioteca). */
    static final class Synthesis extends ElementCategory<SynthesisRecipe> {
        Synthesis(IGuiHelper gui) {
            super(JeiRecipes.SYNTHESIS, "synthesizer", gui, ModBlocks.SYNTHESIZER.get(), 160, 66);
        }

        @Override
        Composition elements(SynthesisRecipe recipe) {
            return recipe.cost();
        }

        @Override
        int[] area() {
            return new int[]{0, 2, 100, 2};
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SynthesisRecipe recipe, IFocusGroup focuses) {
            builder.addOutputSlot(138, 14).setOutputSlotBackground().add(recipe.result());
            addElementSlots(builder, recipe, false);
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, SynthesisRecipe recipe, IFocusGroup focuses) {
            builder.addAnimatedRecipeArrow(recipe.ticks()).setPosition(106, 14);
        }

        @Override
        public void draw(SynthesisRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            super.draw(recipe, slots, graphics, mouseX, mouseY);
            text(graphics, energyAndTime(recipe.energy(), recipe.ticks()), 0, 46, TEXT_DIM);
            text(graphics, Component.translatable("jei.atomcraft.needs_schematic"), 0, 56, TEXT_DIM);
        }
    }

    /** Gerador de Combustão: quanto FE cada item rende. */
    static final class Fuel extends AbstractRecipeCategory<JeiRecipes.Fuel> {
        Fuel(IGuiHelper gui) {
            super(JeiRecipes.FUEL, Component.translatable("jei.atomcraft.combustion_fuel"),
                    gui.createDrawableItemLike(ModBlocks.COMBUSTION_GENERATOR.get()), 120, 24);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipes.Fuel recipe, IFocusGroup focuses) {
            builder.addInputSlot(4, 4).setStandardSlotBackground().add(recipe.item());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, JeiRecipes.Fuel recipe, IFocusGroup focuses) {
            builder.addAnimatedRecipeFlame(200).setPosition(26, 5);
        }

        @Override
        public void draw(JeiRecipes.Fuel recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            ElementCategory.text(graphics, Component.literal(GuiUtil.formatAmount(recipe.energy()) + " FE"), 46, 8, TEXT);
        }
    }

    /** Atomizador: item → elementos guardados na rede. */
    static final class Atomizer extends ElementCategory<JeiRecipes.Atomize> {
        Atomizer(IGuiHelper gui) {
            super(JeiRecipes.ATOMIZE, "atomizer", gui, ModBlocks.ATOMIZER.get(), 160, 53);
        }

        @Override
        Composition elements(JeiRecipes.Atomize recipe) {
            return recipe.elements();
        }

        @Override
        int[] area() {
            return new int[]{50, 1, 110, 3};
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipes.Atomize recipe, IFocusGroup focuses) {
            var input = builder.addInputSlot(2, 18).setStandardSlotBackground();
            if (recipe.fluid() != null) {
                input.setFluidRenderer(1000, false, 16, 16).add(recipe.fluid(), 1000);
            } else {
                input.add(recipe.item());
            }
            addElementSlots(builder, recipe, true);
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, JeiRecipes.Atomize recipe, IFocusGroup focuses) {
            builder.addRecipeArrow().setPosition(23, 18);
        }
    }
}
