package com.atomcraft.material;

/**
 * Metais e ligas do mod. Cada um tem lingote, chapa e um conjunto de blocos para
 * construção (bloco, laje, escada e muro). A composição de cada lingote (9 átomos,
 * como a barra de ferro do vanilla) fica em {@code data/atomcraft/atomcraft/compositions}
 * e as ligas são feitas na Forja de Ligas ({@code data/atomcraft/atomcraft/alloys}).
 * <p>
 * Os nomes, cores e fórmulas ficam em {@code tools/gen_resources.py}; aqui só o que o código usa.
 */
public enum Material {
    // Metais puros (minério próprio, fundem na fornalha)
    TIN("tin", false, 1),
    ZINC("zinc", false, 1),
    LEAD("lead", false, 1),
    NICKEL("nickel", false, 2),
    COBALT("cobalt", false, 2),
    TITANIUM("titanium", false, 3),
    TUNGSTEN("tungsten", false, 3),
    // Ligas (Forja de Ligas), por tier
    STEEL("steel", true, 1),
    BRONZE("bronze", true, 1),
    BRASS("brass", true, 1),
    STAINLESS_STEEL("stainless_steel", true, 2),
    DURALUMIN("duralumin", true, 2),
    INVAR("invar", true, 2),
    TITANIUM_ALLOY("titanium_alloy", true, 3),
    TUNGSTEN_CARBIDE("tungsten_carbide", true, 3),
    SUPERALLOY("superalloy", true, 4);

    private final String id;
    private final boolean alloy;
    private final int tier;

    Material(String id, boolean alloy, int tier) {
        this.id = id;
        this.alloy = alloy;
        this.tier = tier;
    }

    public String id() {
        return id;
    }

    public boolean alloy() {
        return alloy;
    }

    /** 1 = início (aço, bronze), 4 = superliga. */
    public int tier() {
        return tier;
    }
}
