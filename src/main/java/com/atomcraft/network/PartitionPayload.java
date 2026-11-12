package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import com.atomcraft.storage.DiskPartition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: nova partição do disco na mão do jogador. */
public record PartitionPayload(boolean offhand, DiskPartition partition) implements CustomPacketPayload {

    public static final Type<PartitionPayload> TYPE = new Type<>(AtomCraft.id("disk_partition"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartitionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PartitionPayload::offhand,
            DiskPartition.STREAM_CODEC, PartitionPayload::partition,
            PartitionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
