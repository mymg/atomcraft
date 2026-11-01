package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

/** Cliente → servidor: escolhe o que o Sintetizador fabrica. Ar limpa a escolha. */
public record SynthesizePayload(BlockPos pos, Item item) implements CustomPacketPayload {

    public static final Type<SynthesizePayload> TYPE = new Type<>(AtomCraft.id("synthesize"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SynthesizePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SynthesizePayload::pos,
            ByteBufCodecs.registry(Registries.ITEM), SynthesizePayload::item,
            SynthesizePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
