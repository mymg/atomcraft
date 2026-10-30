package com.atomcraft.chemistry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Elementos químicos conhecidos pelo mod.
 * Para adicionar um novo elemento basta incluir uma entrada aqui;
 * os arquivos JSON de composição o referenciam pelo símbolo (ex.: "Fe").
 */
public enum ChemicalElement {
    HYDROGEN(1, "H"),
    CARBON(6, "C"),
    NITROGEN(7, "N"),
    OXYGEN(8, "O"),
    SODIUM(11, "Na"),
    MAGNESIUM(12, "Mg"),
    ALUMINIUM(13, "Al"),
    SILICON(14, "Si"),
    PHOSPHORUS(15, "P"),
    SULFUR(16, "S"),
    CHLORINE(17, "Cl"),
    POTASSIUM(19, "K"),
    CALCIUM(20, "Ca"),
    IRON(26, "Fe"),
    COPPER(29, "Cu"),
    SILVER(47, "Ag"),
    GOLD(79, "Au");

    private static final Map<String, ChemicalElement> BY_SYMBOL = Arrays.stream(values())
            .collect(Collectors.toMap(ChemicalElement::symbol, Function.identity()));

    /** Serializa o elemento pelo símbolo químico ("H", "O", "Fe"...). */
    public static final Codec<ChemicalElement> CODEC = Codec.STRING.comapFlatMap(
            symbol -> {
                ChemicalElement element = BY_SYMBOL.get(symbol);
                return element != null
                        ? DataResult.success(element)
                        : DataResult.error(() -> "Elemento químico desconhecido: " + symbol);
            },
            ChemicalElement::symbol
    );

    private final int atomicNumber;
    private final String symbol;

    ChemicalElement(int atomicNumber, String symbol) {
        this.atomicNumber = atomicNumber;
        this.symbol = symbol;
    }

    public int atomicNumber() {
        return atomicNumber;
    }

    public String symbol() {
        return symbol;
    }

    /** Chave de tradução, ex.: element.atomcraft.oxygen */
    public String translationKey() {
        return "element.atomcraft." + name().toLowerCase();
    }

    public static ChemicalElement bySymbol(String symbol) {
        return BY_SYMBOL.get(symbol);
    }
}
