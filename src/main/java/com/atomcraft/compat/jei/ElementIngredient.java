package com.atomcraft.compat.jei;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.client.ClientState;
import com.atomcraft.client.screen.TechGui;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.TooltipFlag;

import java.util.Arrays;
import java.util.List;

/**
 * Elementos químicos como ingredientes do JEI. Aparecem na lista com o ícone redondo do Terminal de
 * Elementos (só os já descobertos); "R" num elemento mostra os itens que o dão no Atomizador e "U" mostra
 * as ligas e sínteses que o usam.
 */
public final class ElementIngredient {

    public static final IIngredientType<ChemicalElement> TYPE = new IIngredientType<>() {
        @Override
        public Class<? extends ChemicalElement> getIngredientClass() {
            return ChemicalElement.class;
        }

        @Override
        public String getUid() {
            return "atomcraft:element";
        }
    };

    private ElementIngredient() {}

    public static List<ChemicalElement> all() {
        return Arrays.asList(ChemicalElement.values());
    }

    public static List<ChemicalElement> discovered() {
        return Arrays.stream(ChemicalElement.values()).filter(ClientState::isDiscovered).toList();
    }

    static final class Helper implements IIngredientHelper<ChemicalElement> {
        @Override
        public IIngredientType<ChemicalElement> getIngredientType() {
            return TYPE;
        }

        @Override
        public String getDisplayName(ChemicalElement element) {
            return Component.translatable(element.translationKey()).getString() + " (" + element.symbol() + ")";
        }

        @Override
        public Object getUid(ChemicalElement element, UidContext context) {
            return element.symbol();
        }

        @Override
        public Identifier getIdentifier(ChemicalElement element) {
            return AtomCraft.id("element/" + element.symbol().toLowerCase());
        }

        @Override
        public String getDisplayModId(ChemicalElement element) {
            return AtomCraft.MOD_ID;
        }

        @Override
        public ChemicalElement copyIngredient(ChemicalElement element) {
            return element;
        }

        @Override
        public String getErrorInfo(ChemicalElement element) {
            return element == null ? "null" : element.symbol();
        }
    }

    static final class Renderer implements IIngredientRenderer<ChemicalElement> {
        @Override
        public void render(GuiGraphicsExtractor graphics, ChemicalElement element) {
            TechGui.badge(graphics, Minecraft.getInstance().font, 0, 0, element);
        }

        @Override
        public List<Component> getTooltip(ChemicalElement element, TooltipFlag flag) {
            if (!ClientState.isDiscovered(element)) return List.of(TechGui.elementName(element));
            return List.of(TechGui.elementName(element),
                    Component.translatable("jei.atomcraft.element_number", element.atomicNumber()).withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.atomcraft.element_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
