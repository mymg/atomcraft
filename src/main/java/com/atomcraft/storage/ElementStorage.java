package com.atomcraft.storage;

import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * "Sistema" onde ficam guardados os elementos químicos atomizados.
 * Versão inicial: um único estoque compartilhado por todo o mundo,
 * salvo junto com o save (data/atomcraft_elements.dat).
 */
public class ElementStorage extends SavedData {

    private static final String FILE_NAME = "atomcraft_elements";

    private final EnumMap<ChemicalElement, Long> amounts = new EnumMap<>(ChemicalElement.class);

    public static ElementStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ElementStorage::new, ElementStorage::load, null),
                FILE_NAME);
    }

    private static ElementStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        ElementStorage storage = new ElementStorage();
        for (String symbol : tag.getAllKeys()) {
            ChemicalElement element = ChemicalElement.bySymbol(symbol);
            if (element != null) storage.amounts.put(element, tag.getLong(symbol));
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        amounts.forEach((element, amount) -> tag.putLong(element.symbol(), amount));
        return tag;
    }

    public long getAmount(ChemicalElement element) {
        return amounts.getOrDefault(element, 0L);
    }

    public Map<ChemicalElement, Long> getAll() {
        return Collections.unmodifiableMap(amounts);
    }

    /** Guarda os elementos de {@code times} itens com essa composição. */
    public void add(Composition composition, int times) {
        composition.elements().forEach((element, amount) ->
                amounts.merge(element, (long) amount * times, Long::sum));
        setDirty();
    }

    /** Quantas cópias do item podem ser criadas com o estoque atual. */
    public long maxCraftable(Composition composition) {
        long max = Long.MAX_VALUE;
        for (var e : composition.elements().entrySet()) {
            max = Math.min(max, getAmount(e.getKey()) / e.getValue());
        }
        return composition.isEmpty() ? 0 : max;
    }

    /** Consome os elementos de {@code times} itens. Retorna false (sem consumir nada) se faltar algo. */
    public boolean consume(Composition composition, int times) {
        if (maxCraftable(composition) < times) return false;
        composition.elements().forEach((element, amount) ->
                amounts.merge(element, -(long) amount * times, Long::sum));
        amounts.values().removeIf(v -> v <= 0);
        setDirty();
        return true;
    }
}
