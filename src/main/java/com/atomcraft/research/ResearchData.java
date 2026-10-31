package com.atomcraft.research;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Progresso de pesquisa de cada jogador (por UUID), salvo no mundo.
 * Fica no servidor para funcionar mesmo com o jogador offline, já que o
 * Analisador pode receber itens por automação.
 */
public class ResearchData extends SavedData {

    private static final Codec<Map<UUID, Map<Item, Integer>>> DATA_CODEC = Codec.unboundedMap(
            UUIDUtil.STRING_CODEC,
            Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT));

    public static final Codec<ResearchData> CODEC = DATA_CODEC.xmap(ResearchData::new, data -> data.progress);

    public static final SavedDataType<ResearchData> TYPE =
            new SavedDataType<>(AtomCraft.id("research"), ResearchData::create, CODEC);

    private final Map<UUID, Map<Item, Integer>> progress = new HashMap<>();

    private ResearchData(Map<UUID, Map<Item, Integer>> loaded) {
        loaded.forEach((player, items) -> progress.put(player, new HashMap<>(items)));
    }

    private static ResearchData create() {
        return new ResearchData(Map.of());
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
        setDirty();
    }

    public Map<Item, Integer> snapshot(UUID player) {
        Map<Item, Integer> items = progress.get(player);
        return items == null ? Map.of() : Map.copyOf(items);
    }
}
