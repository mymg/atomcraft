package com.atomcraft.network;

import com.atomcraft.AtomCraft;
import com.atomcraft.alloy.AlloyRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Servidor → cliente: receitas da Forja de Ligas (vêm de datapacks, então o cliente não as conhece). */
public record AlloySyncPayload(List<AlloyRecipe> recipes) implements CustomPacketPayload {

    public static final Type<AlloySyncPayload> TYPE = new Type<>(AtomCraft.id("alloys"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloySyncPayload> STREAM_CODEC =
            AlloyRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()).map(AlloySyncPayload::new, AlloySyncPayload::recipes);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
