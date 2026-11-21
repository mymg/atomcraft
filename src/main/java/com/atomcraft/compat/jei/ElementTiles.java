package com.atomcraft.compat.jei;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.client.ClientState;
import com.atomcraft.client.screen.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elementos desenhados como pequenas fichas (símbolo + quantidade), que quebram linha na largura
 * disponível. Os elementos não são itens, então não ocupam slots do JEI: a ficha sob o mouse mostra o
 * nome no tooltip. Elementos ainda não descobertos aparecem como "?".
 */
public final class ElementTiles {

    public static final int TILE_H = 11, GAP = 2;
    private static final int COLOR = 0xFF1E3A40, COLOR_EXOTIC = 0xFF3A1E48, BORDER = 0xFF5FE8FF, BORDER_EXOTIC = 0xFFC080FF;

    /** Ficha posicionada. */
    public record Tile(ChemicalElement element, long amount, int x, int y, int width) {}

    private ElementTiles() {}

    public static List<Tile> layout(Composition composition, int x, int y, int width, int maxRows) {
        Font font = Minecraft.getInstance().font;
        List<Tile> tiles = new ArrayList<>();
        int cx = x, cy = y, row = 0;
        for (Map.Entry<ChemicalElement, Long> entry : composition.elements().entrySet()) {
            String text = label(entry.getKey(), entry.getValue());
            int w = font.width(text) + 6;
            if (cx + w > x + width && cx > x) {
                cx = x;
                cy += TILE_H + GAP;
                if (++row >= maxRows) break;
            }
            tiles.add(new Tile(entry.getKey(), entry.getValue(), cx, cy, w));
            cx += w + GAP;
        }
        return tiles;
    }

    private static String label(ChemicalElement element, long amount) {
        return ClientState.symbol(element) + " " + GuiUtil.formatAmount(amount);
    }

    public static void draw(GuiGraphicsExtractor graphics, List<Tile> tiles, int hidden) {
        Font font = Minecraft.getInstance().font;
        for (Tile tile : tiles) {
            boolean exotic = tile.element().exotic();
            graphics.fill(tile.x(), tile.y(), tile.x() + tile.width(), tile.y() + TILE_H, exotic ? BORDER_EXOTIC : BORDER);
            graphics.fill(tile.x() + 1, tile.y() + 1, tile.x() + tile.width() - 1, tile.y() + TILE_H - 1, exotic ? COLOR_EXOTIC : COLOR);
            graphics.text(font, label(tile.element(), tile.amount()), tile.x() + 3, tile.y() + 2, 0xFFFFFFFF, false);
        }
        if (hidden > 0 && !tiles.isEmpty()) {
            Tile last = tiles.get(tiles.size() - 1);
            graphics.text(font, "+" + hidden, last.x() + last.width() + 3, last.y() + 2, 0xFF9AA4B0, false);
        }
    }

    public static @Nullable Tile at(List<Tile> tiles, double mouseX, double mouseY) {
        for (Tile tile : tiles) {
            if (mouseX >= tile.x() && mouseX < tile.x() + tile.width() && mouseY >= tile.y() && mouseY < tile.y() + TILE_H) return tile;
        }
        return null;
    }

    public static Component name(Tile tile) {
        Component name = ClientState.isDiscovered(tile.element()) ? Component.translatable(tile.element().translationKey())
                : Component.translatable("jei.atomcraft.undiscovered");
        return Component.literal(tile.element().symbol().equals(ClientState.symbol(tile.element())) ? tile.element().symbol() + " — " : "")
                .append(name).append(" ×" + tile.amount());
    }
}
