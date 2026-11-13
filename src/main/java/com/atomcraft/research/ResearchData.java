package com.atomcraft.research;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

/**
 * Progresso de pesquisa de cada jogador (por UUID), salvo no mundo.
 * Fica no servidor para funcionar mesmo com o jogador offline, já que o
 * Analisador pode receber itens por automação.
 */
public class ResearchData extends SavedData {

    private static final Codec<Map<UUID, Map<Item, Integer>>> PROGRESS_CODEC = Codec.unboundedMap(
            UUIDUtil.STRING_CODEC,
            Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT));
    private static final Codec<Map<UUID, List<ChemicalElement>>> DISCOVERED_CODEC = Codec.unboundedMap(
            UUIDUtil.STRING_CODEC, ChemicalElement.CODEC.listOf());

    private static final Codec<ResearchData> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PROGRESS_CODEC.optionalFieldOf("progress", Map.of()).forGetter(data -> data.progress),
            DISCOVERED_CODEC.optionalFieldOf("discovered", Map.of()).forGetter(ResearchData::discoveredLists)
    ).apply(instance, ResearchData::new));

    /** Aceita também o formato antigo (só o mapa de progresso). */
    public static final Codec<ResearchData> CODEC = Codec.withAlternative(CURRENT_CODEC,
            PROGRESS_CODEC.xmap(progress -> new ResearchData(progress, Map.of()), data -> data.progress));

    public static final SavedDataType<ResearchData> TYPE =
            new SavedDataType<>(AtomCraft.id("research"), ResearchData::create, CODEC);

    private final Map<UUID, Map<Item, Integer>> progress = new HashMap<>();
    /** Elementos que cada jogador já descobriu analisando itens. */
    private final Map<UUID, Set<ChemicalElement>> discovered = new HashMap<>();

    private ResearchData(Map<UUID, Map<Item, Integer>> loaded, Map<UUID, List<ChemicalElement>> loadedDiscovered) {
        loaded.forEach((player, items) -> progress.put(player, new HashMap<>(items)));
        loadedDiscovered.forEach((player, elements) -> {
            Set<ChemicalElement> set = EnumSet.noneOf(ChemicalElement.class);
            set.addAll(elements);
            discovered.put(player, set);
        });
    }

    private static ResearchData create() {
        return new ResearchData(Map.of(), Map.of());
    }

    private Map<UUID, List<ChemicalElement>> discoveredLists() {
        Map<UUID, List<ChemicalElement>> result = new HashMap<>();
        discovered.forEach((player, elements) -> result.put(player, List.copyOf(elements)));
        return result;
    }

    public boolean isDiscovered(UUID player, ChemicalElement element) {
        Set<ChemicalElement> elements = discovered.get(player);
        return elements != null && elements.contains(element);
    }

    /** Descobre os elementos da composição e retorna os que eram novos. */
    public List<ChemicalElement> discover(UUID player, Composition composition) {
        Set<ChemicalElement> elements = discovered.computeIfAbsent(player, p -> EnumSet.noneOf(ChemicalElement.class));
        List<ChemicalElement> added = new ArrayList<>();
        for (ChemicalElement element : composition.elements().keySet()) {
            if (elements.add(element)) added.add(element);
        }
        if (!added.isEmpty()) setDirty();
        return added;
    }

    public void discoverAll(UUID player) {
        discovered.computeIfAbsent(player, p -> EnumSet.noneOf(ChemicalElement.class)).addAll(List.of(ChemicalElement.values()));
        setDirty();
    }

    public Set<ChemicalElement> discovered(UUID player) {
        Set<ChemicalElement> elements = discovered.get(player);
        return elements == null ? Set.of() : Set.copyOf(elements);
    }

    public static ResearchData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int getProgress(UUID player, Item item) {
        Map<Item, Integer> items = progress.get(player);
        return items == null ? 0 : items.getOrDefault(item, 0);
    }

    public static int required(Item item) {
        AtomicValue value = AtomicValues.get(item, false);
        return value == null ? Integer.MAX_VALUE : value.researchRequired(item);
    }

    public boolean isResearched(UUID player, Item item) {
        return getProgress(player, item) >= required(item);
    }

    /** Soma progresso e retorna o novo valor (limitado ao necessário). */
    public int addProgress(UUID player, Item item, int amount) {
        Map<Item, Integer> items = progress.computeIfAbsent(player, p -> new HashMap<>());
        int updated = Math.min(required(item), items.getOrDefault(item, 0) + amount);
        items.put(item, updated);
        setDirty();
        return updated;
    }

    /** Marca um item como totalmente pesquisado (comando de administrador). */
    public void unlock(UUID player, Item item) {
        progress.computeIfAbsent(player, p -> new HashMap<>()).put(item, required(item));
        AtomicValue value = AtomicValues.get(item, false);
        if (value != null) discover(player, value.composition());
        setDirty();
    }

    public Map<Item, Integer> snapshot(UUID player) {
        Map<Item, Integer> items = progress.get(player);
        return items == null ? Map.of() : Map.copyOf(items);
    }
}
