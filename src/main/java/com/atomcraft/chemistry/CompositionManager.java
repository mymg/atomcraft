package com.atomcraft.chemistry;

import com.atomcraft.AtomCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carrega os valores atômicos "base" de data packs:
 * {@code data/<namespace>/atomcraft/compositions/*.json}
 *
 * <pre>
 * {
 *   "item": "minecraft:diamond",          // ou "tag": "minecraft:logs"
 *   "elements": { "C": 512 },
 *   "rarity": "rare"                      // opcional; padrão = raridade vanilla do item
 * }
 * </pre>
 *
 * Itens sem entrada própria têm o valor derivado das receitas
 * (ver {@link RecipeDeriver}).
 */
public class CompositionManager extends SimpleJsonResourceReloadListener<CompositionManager.Entry> {

    public record Entry(Optional<Identifier> item, Optional<Identifier> tag, Composition elements, Optional<Rarity> rarity) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("item").forGetter(Entry::item),
                Identifier.CODEC.optionalFieldOf("tag").forGetter(Entry::tag),
                Composition.CODEC.fieldOf("elements").forGetter(Entry::elements),
                Rarity.CODEC.optionalFieldOf("rarity").forGetter(Entry::rarity)
        ).apply(i, Entry::new));
    }

    private static volatile List<Entry> entries = List.of();

    public CompositionManager() {
        super(Entry.CODEC, FileToIdConverter.json("atomcraft/compositions"));
    }

    @Override
    protected void apply(Map<Identifier, Entry> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        entries = List.copyOf(loaded.values());
        AtomCraft.LOGGER.info("AtomCraft: {} composições base carregadas", entries.size());
    }

    public static List<Entry> entries() {
        return entries;
    }
}
