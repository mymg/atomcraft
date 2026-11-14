package com.atomcraft.material;

/**
 * Minérios gerados no mundo (variante de pedra e de deepslate), que soltam o minério bruto
 * ({@code raw_<id>}). Os elementos que faltavam no vanilla (Sn, Zn, Pb, Ni, Cr, Ti, V, W, Li, Co)
 * vêm daqui: atomize o minério bruto, ou funda-o para ter o lingote (quando o metal tem lingote).
 * Tamanho, quantidade e altura dos veios ficam em {@code data/atomcraft/worldgen}.
 */
public enum OreType {
    TIN("tin", 1),
    ZINC("zinc", 1),
    LEAD("lead", 1),
    LITHIUM("lithium", 1),
    NICKEL("nickel", 2),
    CHROMIUM("chromium", 2),
    COBALT("cobalt", 2),
    TITANIUM("titanium", 2),
    TUNGSTEN("tungsten", 3);

    private final String id;
    private final int toolTier;

    OreType(String id, int toolTier) {
        this.id = id;
        this.toolTier = toolTier;
    }

    public String id() {
        return id;
    }

    /** Ferramenta mínima: 1 = pedra, 2 = ferro, 3 = diamante. */
    public int toolTier() {
        return toolTier;
    }
}
