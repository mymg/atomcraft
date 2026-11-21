package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: clique numa célula da grade do Carregador de Chunks. */
public record ChunkLoaderPayload(BlockPos pos, int cell) implements CustomPacketPayload {

    public static final Type<ChunkLoaderPayload> TYPE = new Type<>(AtomCraft.id("chunk_loader"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkLoaderPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ChunkLoaderPayload::pos,
            ByteBufCodecs.VAR_INT, ChunkLoaderPayload::cell,
            ChunkLoaderPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
