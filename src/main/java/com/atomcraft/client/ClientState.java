package com.atomcraft.client;

import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import net.minecraft.world.item.Item;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dados recebidos do servidor, guardados no cliente. Não usa classes exclusivas do cliente. */
public final class ClientState {

    private static Map<Item, Integer> research = Map.of();
    private static Set<ChemicalElement> discovered = Set.of();
    private static Composition networkTotals = Composition.EMPTY;
    private static long networkCapacity;
    private static long networkUsed;

    private ClientState() {}

    public static void setResearch(Map<Item, Integer> progress, List<ChemicalElement> elements) {
        research = Map.copyOf(progress);
        discovered = elements.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(elements));
    }

    /** Se o jogador já descobriu o elemento (analisando um item que o contém). */
    public static boolean isDiscovered(ChemicalElement element) {
        return discovered.contains(element);
    }

    /** Símbolo do elemento, ou "??" se ainda não foi descoberto. */
    public static String symbol(ChemicalElement element) {
        return isDiscovered(element) ? element.symbol() : "??";
    }

    /** Fórmula com os elementos desconhecidos mascarados: ??₂₄ em vez de C₂₄. */
    public static String formula(Composition composition) {
        if (composition.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        composition.elements().forEach((element, amount) -> {
            sb.append(symbol(element));
            if (amount > 1) {
                for (char c : Long.toString(amount).toCharArray()) sb.append((char) ('\u2080' + (c - '0')));
            }
        });
        return sb.toString();
    }

    public static Map<Item, Integer> research() {
        return research;
    }

    public static int researchProgress(Item item) {
        return research.getOrDefault(item, 0);
    }

    public static boolean isResearched(Item item) {
        AtomicValue value = AtomicValues.get(item, true);
        return value != null && researchProgress(item) >= value.researchRequired(item);
    }

    public static void setNetwork(Composition totals, long capacity, long used) {
        networkTotals = totals;
        networkCapacity = capacity;
        networkUsed = used;
    }

    public static Composition networkTotals() {
        return networkTotals;
    }

    public static long networkCapacity() {
        return networkCapacity;
    }

    public static long networkUsed() {
        return networkUsed;
    }

    /** Quantas unidades da composição a rede consegue fornecer. */
    public static long maxFromNetwork(Composition composition) {
        if (composition.isEmpty()) return 0;
        long max = Long.MAX_VALUE;
        for (var entry : composition.elements().entrySet()) {
            max = Math.min(max, networkTotals.get(entry.getKey()) / entry.getValue());
        }
        return max;
    }
}
