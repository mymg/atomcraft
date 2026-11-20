package com.atomcraft.wireless;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Vínculo do Cartão de Rede: qual Transmissor Sem Fio (dimensão + posição) e de qual jogador. */
public record WirelessLink(ResourceKey<Level> dimension, BlockPos pos, UUID owner, String ownerName) {

    public static final Codec<WirelessLink> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(WirelessLink::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(WirelessLink::pos),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(WirelessLink::owner),
            Codec.STRING.optionalFieldOf("owner_name", "").forGetter(WirelessLink::ownerName)
    ).apply(i, WirelessLink::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WirelessLink> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), WirelessLink::dimension,
            BlockPos.STREAM_CODEC, WirelessLink::pos,
            UUIDUtil.STREAM_CODEC, WirelessLink::owner,
            ByteBufCodecs.STRING_UTF8, WirelessLink::ownerName,
            WirelessLink::new);
}
