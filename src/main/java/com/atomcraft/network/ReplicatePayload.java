package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

/** Cliente → servidor: pedido de replicação. {@code amount <= 0} cancela o pedido atual. */
public record ReplicatePayload(BlockPos pos, Item item, int amount) implements CustomPacketPayload {

    public static final Type<ReplicatePayload> TYPE = new Type<>(AtomCraft.id("replicate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ReplicatePayload::pos,
            ByteBufCodecs.registry(Registries.ITEM), ReplicatePayload::item,
            ByteBufCodecs.VAR_INT, ReplicatePayload::amount,
            ReplicatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
