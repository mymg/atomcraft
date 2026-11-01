package com.atomcraft.chemistry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Elementos químicos conhecidos pelo mod.
 * <p>
 * A massa atômica é usada para calcular o custo de energia: elementos pesados
 * (ouro) custam mais para replicar que elementos leves (hidrogênio).
 * Os elementos "exóticos" são fictícios e representam materiais mágicos do
 * Minecraft (redstone, pérolas do Ender, netherita...).
 */
public enum ChemicalElement {
    HYDROGEN(1, "H", 1.008, false),
    HELIUM(2, "He", 4.0026, false),
    BERYLLIUM(4, "Be", 9.012, false),
    CARBON(6, "C", 12.011, false),
    NITROGEN(7, "N", 14.007, false),
    OXYGEN(8, "O", 15.999, false),
    SODIUM(11, "Na", 22.990, false),
    MAGNESIUM(12, "Mg", 24.305, false),
    ALUMINIUM(13, "Al", 26.982, false),
    SILICON(14, "Si", 28.085, false),
    PHOSPHORUS(15, "P", 30.974, false),
    SULFUR(16, "S", 32.06, false),
    CHLORINE(17, "Cl", 35.45, false),
    POTASSIUM(19, "K", 39.098, false),
    CALCIUM(20, "Ca", 40.078, false),
    IRON(26, "Fe", 55.845, false),
    COPPER(29, "Cu", 63.546, false),
    GOLD(79, "Au", 196.967, false),
    // Exóticos (fictícios)
    REDSTONIUM(119, "Rs", 280, true),
    LUMINIUM(120, "Lm", 260, true),
    ENDERIUM(121, "En", 310, true),
    NETHERIUM(122, "Nt", 350, true),
    SCULKIUM(123, "Sk", 330, true),
    STELLARIUM(124, "Sx", 400, true);

    private static final ChemicalElement[] VALUES = values();
    private static final Map<String, ChemicalElement> BY_SYMBOL = Arrays.stream(VALUES)
            .collect(Collectors.toMap(ChemicalElement::symbol, Function.identity()));

    /** Serializa pelo símbolo químico ("H", "O", "Fe"...). */
    public static final Codec<ChemicalElement> CODEC = Codec.STRING.comapFlatMap(
            symbol -> {
                ChemicalElement element = BY_SYMBOL.get(symbol);
                return element != null
                        ? DataResult.success(element)
                        : DataResult.error(() -> "Elemento químico desconhecido: " + symbol);
            },
            ChemicalElement::symbol);

    public static final StreamCodec<ByteBuf, ChemicalElement> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> VALUES[i], Enum::ordinal);

    private final int atomicNumber;
    private final String symbol;
    private final double atomicMass;
    private final boolean exotic;

    ChemicalElement(int atomicNumber, String symbol, double atomicMass, boolean exotic) {
        this.atomicNumber = atomicNumber;
        this.symbol = symbol;
        this.atomicMass = atomicMass;
        this.exotic = exotic;
    }

    public int atomicNumber() {
        return atomicNumber;
    }

    public String symbol() {
        return symbol;
    }

    public double atomicMass() {
        return atomicMass;
    }

    public boolean exotic() {
        return exotic;
    }

    /** Chave de tradução, ex.: element.atomcraft.oxygen */
    public String translationKey() {
        return "element.atomcraft." + name().toLowerCase();
    }

    public static ChemicalElement bySymbol(String symbol) {
        return BY_SYMBOL.get(symbol);
    }
}
