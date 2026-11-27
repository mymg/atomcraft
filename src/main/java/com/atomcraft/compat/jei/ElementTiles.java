package com.atomcraft.compat.jei;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.client.screen.GuiUtil;
import com.atomcraft.client.screen.TechGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elementos desenhados com o mesmo ícone redondo do Terminal de Elementos, seguido da quantidade, quebrando
 * linha na largura disponível. Os elementos não são itens, então não ocupam slots do JEI: o ícone sob o
 * mouse mostra o nome no tooltip. Elementos ainda não descobertos aparecem como "?".
 */
public final class ElementTiles {

    public static final int TILE_H = 16, GAP = 4, ROW = 17;
    private static final int TEXT = 0xFF404850;

    /** Ficha posicionada. */
    public record Tile(ChemicalElement element, long amount, int x, int y, int width) {}

    private ElementTiles() {}

    public static List<Tile> layout(Composition composition, int x, int y, int width, int maxRows) {
        Font font = Minecraft.getInstance().font;
        List<Tile> tiles = new ArrayList<>();
        int cx = x, cy = y, row = 0;
        for (Map.Entry<ChemicalElement, Long> entry : composition.elements().entrySet()) {
            int w = 17 + font.width(GuiUtil.formatAmount(entry.getValue()));
            if (cx + w > x + width && cx > x) {
                cx = x;
                cy += ROW;
                if (++row >= maxRows) break;
            }
            tiles.add(new Tile(entry.getKey(), entry.getValue(), cx, cy, w));
            cx += w + GAP;
        }
        return tiles;
    }

    public static void draw(GuiGraphicsExtractor graphics, List<Tile> tiles, int hidden) {
        Font font = Minecraft.getInstance().font;
        for (Tile tile : tiles) {
            // O ícone é desenhado pelo slot do JEI (ElementIngredient.Renderer); aqui só a quantidade
            graphics.text(font, GuiUtil.formatAmount(tile.amount()), tile.x() + 17, tile.y() + 4, TEXT, false);
        }
        if (hidden > 0 && !tiles.isEmpty()) {
            Tile last = tiles.get(tiles.size() - 1);
            graphics.text(font, "+" + hidden, last.x() + last.width() + 3, last.y() + 4, 0xFF707880, false);
        }
    }

    public static @Nullable Tile at(List<Tile> tiles, double mouseX, double mouseY) {
        for (Tile tile : tiles) {
            if (mouseX >= tile.x() && mouseX < tile.x() + tile.width() && mouseY >= tile.y() && mouseY < tile.y() + TILE_H) return tile;
        }
        return null;
    }

    public static Component name(Tile tile) {
        return TechGui.elementName(tile.element()).copy().append(" ×" + tile.amount());
    }
}
