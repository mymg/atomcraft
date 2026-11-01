package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.ChemicalElement;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Servidor → cliente: progresso de pesquisa do jogador. */
public record ResearchSyncPayload(Map<Item, Integer> progress, List<ChemicalElement> discovered) implements CustomPacketPayload {

    public static final Type<ResearchSyncPayload> TYPE = new Type<>(AtomCraft.id("research"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.ITEM), ByteBufCodecs.VAR_INT),
            payload -> new HashMap<>(payload.progress()),
            ChemicalElement.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ResearchSyncPayload::discovered,
            ResearchSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
