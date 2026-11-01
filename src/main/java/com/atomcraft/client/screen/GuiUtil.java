package com.atomcraft.client.screen;

/** Formatação de números para as interfaces. */
public final class GuiUtil {

    private GuiUtil() {}

    /** 1234 → "1234"; 16203 → "16.2k"; 2000000 → "2.0M". */
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
