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

    /** Duração legível: 8.5s, 1m 40s, 2h 05m. */
    public static String formatDuration(long ticks) {
        double seconds = ticks / 20.0;
        if (seconds < 60) return String.format("%.1fs", seconds);
        long total = Math.round(seconds);
        if (total < 3600) return String.format("%dm %02ds", total / 60, total % 60);
        return String.format("%dh %02dm", total / 3600, (total % 3600) / 60);
    }
}
