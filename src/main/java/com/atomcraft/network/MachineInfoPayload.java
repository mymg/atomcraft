package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: informações extras da máquina aberta (dono), para a aba de informações. */
public record MachineInfoPayload(int containerId, String ownerName) implements CustomPacketPayload {

    public static final Type<MachineInfoPayload> TYPE = new Type<>(AtomCraft.id("machine_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineInfoPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachineInfoPayload::containerId,
            ByteBufCodecs.STRING_UTF8, MachineInfoPayload::ownerName,
            MachineInfoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
