package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Cliente → servidor: estado do jetpack ({@code state} 0 parado, 1 subindo, 2 flutuando) ou, com
 * {@code toggleHover}, a tecla de flutuação.
 */
public record JetpackPayload(int state, boolean toggleHover) implements CustomPacketPayload {

    public static final Type<JetpackPayload> TYPE = new Type<>(AtomCraft.id("jetpack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JetpackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, JetpackPayload::state,
            ByteBufCodecs.BOOL, JetpackPayload::toggleHover,
            JetpackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
