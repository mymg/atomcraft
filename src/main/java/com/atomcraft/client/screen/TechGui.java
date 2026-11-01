package com.atomcraft.client.screen;

import com.atomcraft.chemistry.ChemicalElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Tema "tecnológico" das interfaces, desenhado pixel a pixel (sem texturas):
 * moldura metálica com parafusos e suportes dourados, painéis de vidro
 * verde-azulado com scanlines, slots escuros e barras segmentadas.
 */
public final class TechGui {

    public static final int OUT = 0xFF0A0D10;
    public static final int METAL = 0xFF3D4550, METAL_HI = 0xFF8A95A1, METAL_LIGHT = 0xFF66717D,
            METAL_DARK = 0xFF2A3038, METAL_SHADOW = 0xFF1C2127;
    public static final int GLASS_BG = 0xFF0B2224, GLASS_SCAN = 0xFF0D282A, GLASS_EDGE = 0xFF27C9B5, GLASS_EDGE_DIM = 0xFF124C47;
    public static final int SLOT_BG = 0xFF061214, SLOT_DARK = 0xFF020707, SLOT_LIGHT = 0xFF1A4541;
    public static final int GOLD = 0xFFC9A646, GOLD_DARK = 0xFF7F6322, GOLD_LIGHT = 0xFFF0D985;
    public static final int TEXT_TEAL = 0xFF7FF2E1, TEXT_LIGHT = 0xFFD5DCE4, TEXT_DIM = 0xFF8A96A3, TEXT_GOOD = 0xFF5EE28A,
            TEXT_BAD = 0xFFFF6B6B;
    public static final int ENERGY = 0xFFE23232, PROGRESS = 0xFFA855F7, STORAGE = 0xFF3BD15A, SELECTED = 0xFF2E8F7E;

    private TechGui() {}

    public static int scale(int color, double factor) {
        int r = (int) Math.min(255, ((color >> 16) & 255) * factor);
        int g = (int) Math.min(255, ((color >> 8) & 255) * factor);
        int b = (int) Math.min(255, (color & 255) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Moldura metálica com cantos chanfrados, relevo, sulco interno, parafusos e suportes dourados. */
    public static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 3, y, x + w - 3, y + h, OUT);
        g.fill(x, y + 3, x + w, y + h - 3, OUT);
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, OUT);
        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, OUT);
        g.fill(x + 3, y + 1, x + w - 3, y + h - 1, METAL);
        g.fill(x + 1, y + 3, x + w - 1, y + h - 3, METAL);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, METAL);
        g.fill(x + 3, y + 1, x + w - 3, y + 2, METAL_HI);
        g.fill(x + 1, y + 3, x + 2, y + h - 3, METAL_LIGHT);
        g.fill(x + 2, y + 2, x + 3, y + 3, METAL_HI);
        g.fill(x + 3, y + h - 2, x + w - 3, y + h - 1, METAL_SHADOW);
        g.fill(x + w - 2, y + 3, x + w - 1, y + h - 3, METAL_DARK);
        g.fill(x + w - 3, y + h - 3, x + w - 2, y + h - 2, METAL_SHADOW);
        // Sulco interno
        g.fill(x + 4, y + 4, x + w - 4, y + 5, METAL_DARK);
        g.fill(x + 4, y + 4, x + 5, y + h - 4, METAL_DARK);
        g.fill(x + 5, y + h - 5, x + w - 4, y + h - 4, METAL_LIGHT);
        g.fill(x + w - 5, y + 5, x + w - 4, y + h - 4, METAL_LIGHT);
        bolt(g, x + 6, y + 6);
        bolt(g, x + w - 9, y + 6);
        bolt(g, x + 6, y + h - 9);
        bolt(g, x + w - 9, y + h - 9);
        bracket(g, x - 2, y + h / 2 - 14, 28);
        bracket(g, x + w - 1, y + h / 2 - 14, 28);
    }

    private static void bolt(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 3, y + 3, METAL_SHADOW);
        g.fill(x, y, x + 2, y + 2, METAL_HI);
        g.fill(x + 1, y + 1, x + 2, y + 2, METAL_LIGHT);
    }

    private static void bracket(GuiGraphicsExtractor g, int x, int y, int h) {
        g.fill(x, y, x + 3, y + h, OUT);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, GOLD);
        g.fill(x, y + 3, x + 3, y + 4, GOLD_DARK);
        g.fill(x, y + h - 4, x + 3, y + h - 3, GOLD_DARK);
        g.fill(x + 1, y + 1, x + 2, y + 3, GOLD_LIGHT);
    }

    /** Aba de título no topo, centralizada em {@code centerX}. */
    public static void tab(GuiGraphicsExtractor g, Font font, int centerX, int y, String title) {
        int tw = font.width(title) + 16;
        int x = centerX - tw / 2;
        g.fill(x + 2, y, x + tw - 2, y + 14, OUT);
        g.fill(x, y + 2, x + tw, y + 14, OUT);
        g.fill(x + 1, y + 1, x + tw - 1, y + 14, OUT);
        g.fill(x + 2, y + 1, x + tw - 2, y + 14, METAL);
        g.fill(x + 1, y + 2, x + tw - 1, y + 14, METAL);
        g.fill(x + 2, y + 1, x + tw - 2, y + 2, METAL_HI);
        plainGlass(g, x + 4, y + 3, tw - 8, 10);
        g.text(font, title, centerX - font.width(title) / 2, y + 4, TEXT_TEAL, false);
    }

    public static void plainGlass(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OUT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, GLASS_BG);
        for (int yy = y + 2; yy < y + h - 1; yy += 2) g.fill(x + 1, yy, x + w - 1, yy + 1, GLASS_SCAN);
    }

    /** Painel de vidro (tela) com scanlines e cantos de HUD em destaque. */
    public static void glass(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OUT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, GLASS_EDGE_DIM);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, GLASS_BG);
        for (int yy = y + 3; yy < y + h - 2; yy += 2) g.fill(x + 2, yy, x + w - 2, yy + 1, GLASS_SCAN);
        int l = 5;
        corner(g, x + 1, y + 1, 1, 1, l);
        corner(g, x + w - 2, y + 1, -1, 1, l);
        corner(g, x + 1, y + h - 2, 1, -1, l);
        corner(g, x + w - 2, y + h - 2, -1, -1, l);
    }

    private static void corner(GuiGraphicsExtractor g, int cx, int cy, int dx, int dy, int length) {
        int x1 = Math.min(cx, cx + dx * length), x2 = Math.max(cx, cx + dx * length);
        int y1 = Math.min(cy, cy + dy * length), y2 = Math.max(cy, cy + dy * length);
        g.fill(x1, cy, x2 + 1, cy + 1, GLASS_EDGE);
        g.fill(cx, y1, cx + 1, y2 + 1, GLASS_EDGE);
    }

    /** Slot escuro da máquina. (sx, sy) é a posição do item. */
    public static void slot(GuiGraphicsExtractor g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_LIGHT);
        g.fill(sx - 1, sy - 1, sx + 16, sy + 16, SLOT_DARK);
        g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
    }

    /** Slot metálico do inventário do jogador. */
    public static void metalSlot(GuiGraphicsExtractor g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, METAL_LIGHT);
        g.fill(sx - 1, sy - 1, sx + 16, sy + 16, METAL_SHADOW);
        g.fill(sx, sy, sx + 16, sy + 16, 0xFF2C333B);
    }

    /** Campo de texto escuro (fundo para números e valores). */
    public static void field(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OUT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, SLOT_BG);
    }

    /** Barra vertical segmentada, preenchida de baixo para cima. */
    public static void verticalBar(GuiGraphicsExtractor g, int x, int y, int w, int h, float fraction, int color) {
        g.fill(x, y, x + w, y + h, OUT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0A0C0E);
        int segments = (h - 3) / 3;
        int lit = Math.round(segments * Math.max(0, Math.min(1, fraction)));
        if (lit == 0 && fraction > 0) lit = 1;
        int dim = scale(color, 0.22), bright = scale(color, 1.4);
        for (int i = 0; i < segments; i++) {
            int yy = y + h - 4 - i * 3;
            boolean on = i < lit;
            g.fill(x + 2, yy, x + w - 2, yy + 2, on ? color : dim);
            if (on) g.fill(x + 2, yy, x + 3, yy + 2, bright);
        }
    }

    /** Barra horizontal segmentada, preenchida da esquerda para a direita. */
    public static void horizontalBar(GuiGraphicsExtractor g, int x, int y, int w, int h, float fraction, int color) {
        g.fill(x, y, x + w, y + h, OUT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0A0C0E);
        int segments = (w - 3) / 3;
        int lit = Math.round(segments * Math.max(0, Math.min(1, fraction)));
        if (lit == 0 && fraction > 0) lit = 1;
        int dim = scale(color, 0.22), bright = scale(color, 1.4);
        for (int i = 0; i < segments; i++) {
            int xx = x + 2 + i * 3;
            boolean on = i < lit;
            g.fill(xx, y + 2, xx + 2, y + h - 2, on ? color : dim);
            if (on) g.fill(xx, y + 2, xx + 2, y + 3, bright);
        }
    }

    /** Botão metálico com texto verde-azulado. */
    public static void button(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, String label, boolean hover, boolean active) {
        g.fill(x, y, x + w, y + h, OUT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, active && hover ? METAL_LIGHT : METAL);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, METAL_HI);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, METAL_SHADOW);
        int color = !active ? TEXT_DIM : hover ? 0xFFFFFFFF : TEXT_TEAL;
        g.text(font, label, x + (w - font.width(label) + 1) / 2, y + (h - 7) / 2, color, false);
    }

    // ----- Badges dos elementos -----

    /** Círculo pixel art (16×16) colorido com o símbolo do elemento. */
    public static void badge(GuiGraphicsExtractor g, Font font, int x, int y, ChemicalElement element) {
        badge(g, font, x, y, element, false);
    }

    /** Badge apagada (elemento sem estoque) quando {@code dim} é verdadeiro. */
    public static void badge(GuiGraphicsExtractor g, Font font, int x, int y, ChemicalElement element, boolean dim) {
        int color = ElementColors.of(element);
        if (dim) color = scale(color, 0.32);
        disk(g, x, y, 8, 0xFF151515);
        disk(g, x + 1, y + 1, 7, color);
        int highlight = scale(color, 1.35);
        g.fill(x + 4, y + 3, x + 6, y + 4, highlight);
        g.fill(x + 3, y + 4, x + 4, y + 6, highlight);
        String symbol = element.symbol();
        int textColor = dim ? 0xFF8A8A8A : ElementColors.luminance(color) > 140 ? 0xFF151515 : 0xFFFFFFFF;
        // A largura da fonte inclui 1 px de espaçamento à direita; centraliza pela parte visível.
        int visible = font.width(symbol) - 1;
        g.text(font, symbol, x + (16 - visible + 1) / 2, y + 4, textColor, false);
    }

    private static void disk(GuiGraphicsExtractor g, int x, int y, int radius, int color) {
        int size = radius * 2;
        for (int row = 0; row < size; row++) {
            double dy = row + 0.5 - radius;
            int half = (int) (Math.sqrt(Math.max(0, radius * radius - dy * dy)) + 0.5);
            g.fill(x + radius - half, y + row, x + radius + half, y + row + 1, color);
        }
    }

    /** Valor curto (até 4 caracteres): 882, 1.2k, 123k, 4.5M. */
    public static String shortAmount(long amount) {
        if (amount < 1_000) return Long.toString(amount);
        if (amount < 10_000) return String.format("%.1fk", amount / 1000.0);
        if (amount < 1_000_000) return (amount / 1000) + "k";
        if (amount < 10_000_000) return String.format("%.1fM", amount / 1_000_000.0);
        if (amount < 1_000_000_000) return (amount / 1_000_000) + "M";
        return (amount / 1_000_000_000) + "G";
    }
}
