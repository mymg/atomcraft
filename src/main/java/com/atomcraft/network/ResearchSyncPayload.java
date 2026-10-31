package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/** Servidor → cliente: progresso de pesquisa do jogador. */
public record ResearchSyncPayload(Map<Item, Integer> progress) implements CustomPacketPayload {

    public static final Type<ResearchSyncPayload> TYPE = new Type<>(AtomCraft.id("research"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchSyncPayload> STREAM_CODEC = ByteBufCodecs
            .map(HashMap::new, ByteBufCodecs.registry(Registries.ITEM), ByteBufCodecs.VAR_INT)
            .map(ResearchSyncPayload::new, payload -> new HashMap<>(payload.progress()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
