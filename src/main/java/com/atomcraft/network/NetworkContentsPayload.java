package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.Composition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: estoque da rede da máquina aberta. */
public record NetworkContentsPayload(Composition totals, long capacity, long used) implements CustomPacketPayload {

    public static final Type<NetworkContentsPayload> TYPE = new Type<>(AtomCraft.id("network_contents"));

    public static final StreamCodec<ByteBuf, NetworkContentsPayload> STREAM_CODEC = StreamCodec.composite(
            Composition.STREAM_CODEC, NetworkContentsPayload::totals,
            ByteBufCodecs.VAR_LONG, NetworkContentsPayload::capacity,
            ByteBufCodecs.VAR_LONG, NetworkContentsPayload::used,
            NetworkContentsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
