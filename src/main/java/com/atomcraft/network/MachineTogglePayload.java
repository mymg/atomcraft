package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: botão de ligar/desligar da máquina. */
public record MachineTogglePayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<MachineTogglePayload> TYPE = new Type<>(AtomCraft.id("machine_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineTogglePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MachineTogglePayload::pos,
            MachineTogglePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
