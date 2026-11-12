package com.atomcraft.storage;

import com.atomcraft.chemistry.ChemicalElement;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Partição de um disco: quais elementos ele aceita e, opcionalmente, quanto de cada um.
 * Sem partição (mapa vazio) o disco aceita qualquer elemento. Limite 0 = sem limite.
 */
public record DiskPartition(Map<ChemicalElement, Long> limits) {

    public static final DiskPartition NONE = new DiskPartition(Map.of());

    public static final Codec<DiskPartition> CODEC = Codec.unboundedMap(ChemicalElement.CODEC, Codec.LONG)
            .xmap(DiskPartition::of, DiskPartition::limits);

    public static final StreamCodec<ByteBuf, DiskPartition> STREAM_CODEC = ByteBufCodecs
            .map(HashMap<ChemicalElement, Long>::new, ChemicalElement.STREAM_CODEC, ByteBufCodecs.VAR_LONG)
            .map(DiskPartition::of, partition -> new HashMap<>(partition.limits()));

    public static DiskPartition of(Map<ChemicalElement, Long> limits) {
        if (limits.isEmpty()) return NONE;
        Map<ChemicalElement, Long> copy = new EnumMap<>(ChemicalElement.class);
        limits.forEach((element, limit) -> copy.put(element, Math.max(0, limit)));
        return new DiskPartition(Map.copyOf(copy));
    }

    public boolean isEmpty() {
        return limits.isEmpty();
    }

    public boolean allows(ChemicalElement element) {
        return limits.isEmpty() || limits.containsKey(element);
    }

    /** Quanto ainda cabe do elemento, considerando só a partição (sem a capacidade do disco). */
    public long room(ChemicalElement element, long current) {
        if (limits.isEmpty()) return Long.MAX_VALUE;
        Long limit = limits.get(element);
        if (limit == null) return 0;
        return limit == 0 ? Long.MAX_VALUE : Math.max(0, limit - current);
    }
}
