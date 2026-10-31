package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.AtomicValue;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/** Servidor → cliente: tabela de valores atômicos (para tooltips e interfaces). */
public record ValuesSyncPayload(Map<Item, AtomicValue> values) implements CustomPacketPayload {

    public static final Type<ValuesSyncPayload> TYPE = new Type<>(AtomCraft.id("values"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ValuesSyncPayload> STREAM_CODEC = ByteBufCodecs
            .map(HashMap::new, ByteBufCodecs.registry(Registries.ITEM), AtomicValue.STREAM_CODEC)
            .map(ValuesSyncPayload::new, payload -> new HashMap<>(payload.values()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
