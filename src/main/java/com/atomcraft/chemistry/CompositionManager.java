package com.atomcraft.chemistry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Carrega as composições químicas dos itens a partir de data packs:
 * {@code data/<namespace>/compositions/*.json}
 *
 * <pre>
 * {
 *   "item": "minecraft:sugar",
 *   "elements": { "C": 12, "H": 22, "O": 11 }
 * }
 * </pre>
 *
 * Qualquer mod ou data pack pode adicionar/sobrescrever composições.
 */
public class CompositionManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private record Entry(Item item, Composition composition) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
                Composition.CODEC.fieldOf("elements").forGetter(Entry::composition)
        ).apply(i, Entry::new));
    }

    private static volatile Map<Item, Composition> compositions = Map.of();

    public CompositionManager() {
        super(GSON, "compositions");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Item, Composition> loaded = new HashMap<>();
        files.forEach((id, json) -> Entry.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LOGGER.error("Composição inválida em {}: {}", id, error))
                .ifPresent(entry -> loaded.put(entry.item(), entry.composition())));
        compositions = Map.copyOf(loaded);
        LOGGER.info("AtomCraft: {} composições químicas carregadas", loaded.size());
    }

    public static Optional<Composition> get(Item item) {
        return Optional.ofNullable(compositions.get(item));
    }

    public static Map<Item, Composition> all() {
        return compositions;
    }
}
