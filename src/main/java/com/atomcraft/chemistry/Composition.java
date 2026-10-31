package com.atomcraft.chemistry;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Uma quantidade de átomos de cada elemento. Imutável.
 * <p>
 * Usada para a composição dos itens e também para o conteúdo dos discos de
 * elementos.
 */
public record Composition(Map<ChemicalElement, Long> elements) {

    public static final Composition EMPTY = new Composition(Map.of());

    public static final Codec<Composition> CODEC = Codec.unboundedMap(ChemicalElement.CODEC, Codec.LONG)
            .xmap(Composition::new, Composition::elements);

    public static final StreamCodec<ByteBuf, Composition> STREAM_CODEC = ByteBufCodecs
            .map(HashMap::new, ChemicalElement.STREAM_CODEC, ByteBufCodecs.VAR_LONG)
            .map(Composition::new, c -> new HashMap<>(c.elements()));

    private static final char[] SUBSCRIPTS = {'₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉'};

    public Composition {
        EnumMap<ChemicalElement, Long> copy = new EnumMap<>(ChemicalElement.class);
        elements.forEach((element, amount) -> {
            if (amount != null && amount > 0) copy.put(element, amount);
        });
        elements = Collections.unmodifiableMap(copy);
    }

    public long get(ChemicalElement element) {
        return elements.getOrDefault(element, 0L);
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /** Total de átomos, somando todos os elementos. */
    public long totalAtoms() {
        long total = 0;
        for (long amount : elements.values()) total += amount;
        return total;
    }

    /** Soma de (quantidade × massa atômica). Base do custo de energia e tempo. */
    public double weightedMass() {
        double total = 0;
        for (var entry : elements.entrySet()) total += entry.getValue() * entry.getKey().atomicMass();
        return total;
    }

    public Composition plus(Composition other) {
        EnumMap<ChemicalElement, Long> result = mutable();
        other.elements.forEach((element, amount) -> result.merge(element, amount, Long::sum));
        return new Composition(result);
    }

    /** Subtrai, sem deixar nenhum elemento negativo. */
    public Composition minusClamped(Composition other) {
        EnumMap<ChemicalElement, Long> result = mutable();
        other.elements.forEach((element, amount) -> result.computeIfPresent(element, (e, v) -> Math.max(0, v - amount)));
        return new Composition(result);
    }

    public Composition times(long factor) {
        if (factor == 1) return this;
        EnumMap<ChemicalElement, Long> result = new EnumMap<>(ChemicalElement.class);
        elements.forEach((element, amount) -> result.put(element, amount * factor));
        return new Composition(result);
    }

    /**
     * Divide arredondando para baixo. Se tudo virar zero, mantém 1 átomo do
     * elemento mais abundante, para que nenhum item derivado fique sem valor.
     */
    public Composition divideFloor(long divisor) {
        if (divisor <= 1) return this;
        EnumMap<ChemicalElement, Long> result = new EnumMap<>(ChemicalElement.class);
        elements.forEach((element, amount) -> result.put(element, amount / divisor));
        Composition divided = new Composition(result);
        if (divided.isEmpty() && !isEmpty()) {
            ChemicalElement largest = elements.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElseThrow().getKey();
            return new Composition(Map.of(largest, 1L));
        }
        return divided;
    }

    /** Multiplica por uma fração (0..1), arredondando para baixo. Usado para itens danificados. */
    public Composition scale(double fraction) {
        if (fraction >= 1) return this;
        EnumMap<ChemicalElement, Long> result = new EnumMap<>(ChemicalElement.class);
        elements.forEach((element, amount) -> result.put(element, (long) Math.floor(amount * Math.max(0, fraction))));
        return new Composition(result);
    }

    /** Fórmula legível com subscritos, ex.: H₂O ou C₁₂H₂₂O₁₁. */
    public String formula() {
        if (isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        elements.forEach((element, amount) -> {
            sb.append(element.symbol());
            if (amount > 1) {
                for (char c : Long.toString(amount).toCharArray()) sb.append(SUBSCRIPTS[c - '0']);
            }
        });
        return sb.toString();
    }

    private EnumMap<ChemicalElement, Long> mutable() {
        EnumMap<ChemicalElement, Long> map = new EnumMap<>(ChemicalElement.class);
        map.putAll(elements);
        return map;
    }
}
