package com.atomcraft.chemistry;

import com.atomcraft.Config;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * O "valor atômico" de um item: o que ele rende ao ser atomizado e o que custa
 * para ser replicado.
 */
public record AtomicValue(Composition composition, Rarity rarity) {

    public static final Codec<AtomicValue> CODEC = RecordCodecBuilder.create(i -> i.group(
            Composition.CODEC.fieldOf("elements").forGetter(AtomicValue::composition),
            Rarity.CODEC.fieldOf("rarity").forGetter(AtomicValue::rarity)
    ).apply(i, AtomicValue::new));

    public static final StreamCodec<ByteBuf, AtomicValue> STREAM_CODEC = StreamCodec.composite(
            Composition.STREAM_CODEC, AtomicValue::composition,
            Rarity.STREAM_CODEC, AtomicValue::rarity,
            AtomicValue::new);

    /** Energia (FE) para replicar uma unidade. */
    public long energyCost() {
        double cost = composition.weightedMass() * Config.FE_PER_MASS.get() * rarity.energyMultiplier();
        return Math.max(1, (long) Math.ceil(cost));
    }

    /** Ticks para replicar uma unidade (20 ticks = 1 segundo). */
    public int replicateTicks() {
        double ticks = (10 + Math.sqrt(composition.weightedMass())) * rarity.timeMultiplier()
                * Config.REPLICATION_TIME.get();
        return (int) Math.max(1, Math.min(72_000, Math.ceil(ticks)));
    }

    /** Ticks para atomizar uma unidade. */
    public int atomizeTicks() {
        double ticks = (4 + Math.sqrt(composition.weightedMass()) / 4) * Config.ATOMIZE_TIME.get();
        return (int) Math.max(1, Math.min(72_000, Math.ceil(ticks)));
    }

    /**
     * Quantas unidades precisam ser pesquisadas. Itens que não empilham
     * (ferramentas, armaduras) precisam de apenas 1, como no modo Journey do Terraria.
     */
    public int researchRequired(Item item) {
        return Math.max(1, Math.min(rarity.researchCount(), item.getDefaultMaxStackSize()));
    }
}
