package com.atomcraft.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Desenho simples das interfaces, no estilo vanilla, sem precisar de texturas. */
public final class GuiUtil {

    public static final int TEXT = 0xFF404040;
    public static final int TEXT_LIGHT = 0xFF707070;

    private GuiUtil() {}

    /** Painel cinza com borda em relevo, como os baús vanilla. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 2, y + 2, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFFC6C6C6);
    }

    /** Fundo de slot. (x, y) é a posição do item (16×16). */
    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFFFFFF);
        g.fill(x - 1, y - 1, x + 16, y + 16, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    /** Área afundada (para listas e barras). */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFFFFFFF);
        g.fill(x, y, x + w - 1, y + h - 1, 0xFF373737);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF8B8B8B);
    }

    public static void horizontalBar(GuiGraphicsExtractor g, int x, int y, int w, int h, float fraction, int color) {
        inset(g, x, y, w, h);
        int filled = Math.round((w - 2) * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) g.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, color);
    }

    public static void verticalBar(GuiGraphicsExtractor g, int x, int y, int w, int h, float fraction, int color) {
        inset(g, x, y, w, h);
        int filled = Math.round((h - 2) * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) g.fill(x + 1, y + h - 1 - filled, x + w - 1, y + h - 1, color);
    }

    public static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** 1234 → "1,2k"; 2500000 → "2,5M". */
    public static String formatAmount(long amount) {
        if (amount < 10_000) return Long.toString(amount);
        if (amount < 1_000_000) return String.format("%.1fk", amount / 1000.0);
        if (amount < 1_000_000_000) return String.format("%.1fM", amount / 1_000_000.0);
        return String.format("%.1fG", amount / 1_000_000_000.0);
    }

    public static String ellipsize(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, Math.max(0, maxChars - 1)) + "…";
    }
}
