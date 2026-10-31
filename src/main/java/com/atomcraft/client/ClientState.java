package com.atomcraft.client;

import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.Composition;
import net.minecraft.world.item.Item;

import java.util.Map;

/** Dados recebidos do servidor, guardados no cliente. Não usa classes exclusivas do cliente. */
public final class ClientState {

    private static Map<Item, Integer> research = Map.of();
    private static Composition networkTotals = Composition.EMPTY;
    private static long networkCapacity;
    private static long networkUsed;

    private ClientState() {}

    public static void setResearch(Map<Item, Integer> progress) {
        research = Map.copyOf(progress);
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
