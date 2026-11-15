package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: muda o modo (entrada/saída/ambos/nada) de um lado da Célula de Energia. */
public record SideConfigPayload(BlockPos pos, int side, int mode) implements CustomPacketPayload {

    public static final Type<SideConfigPayload> TYPE = new Type<>(AtomCraft.id("side_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SideConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SideConfigPayload::pos,
            ByteBufCodecs.VAR_INT, SideConfigPayload::side,
            ByteBufCodecs.VAR_INT, SideConfigPayload::mode,
            SideConfigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
