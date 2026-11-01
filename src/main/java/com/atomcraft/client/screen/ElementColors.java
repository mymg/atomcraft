package com.atomcraft.client.screen;

import com.atomcraft.chemistry.ChemicalElement;

/** Cor da badge de cada elemento, inspirada nas cores usuais de modelos moleculares. */
public final class ElementColors {

    private ElementColors() {}

    public static int of(ChemicalElement element) {
        return switch (element) {
            case HYDROGEN -> 0xFFF2F2F2;
            case HELIUM -> 0xFFC8F7FF;
            case BERYLLIUM -> 0xFFB8E85A;
            case CARBON -> 0xFFE02626;
            case NITROGEN -> 0xFF3A5BF0;
            case OXYGEN -> 0xFF39B6E8;
            case SODIUM -> 0xFFAB5CF2;
            case MAGNESIUM -> 0xFF6FCF3A;
            case ALUMINIUM -> 0xFFBFA6A6;
            case SILICON -> 0xFFE8C9A0;
            case PHOSPHORUS -> 0xFFFF8A1F;
            case SULFUR -> 0xFFFFE433;
            case CHLORINE -> 0xFF2FE07A;
            case POTASSIUM -> 0xFF8F40D4;
            case CALCIUM -> 0xFF9CD69C;
            case IRON -> 0xFFC9CED4;
            case COPPER -> 0xFFC97B3A;
            case GOLD -> 0xFFFFCC33;
            case REDSTONIUM -> 0xFFB31414;
            case LUMINIUM -> 0xFFFFF09A;
            case ENDERIUM -> 0xFF1E9E8C;
            case NETHERIUM -> 0xFF5A4646;
            case SCULKIUM -> 0xFF0E5566;
            case STELLARIUM -> 0xFFE6E6FF;
        };
    }

    public static double luminance(int color) {
        return 0.299 * ((color >> 16) & 255) + 0.587 * ((color >> 8) & 255) + 0.114 * (color & 255);
    }
}
