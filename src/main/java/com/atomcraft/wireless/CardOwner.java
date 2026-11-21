package com.atomcraft.wireless;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** Jogador a quem o Cartão de Rede pertence (agachado + clique direito com o cartão na mão). */
public record CardOwner(UUID owner, String ownerName) {

    public static final Codec<CardOwner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(CardOwner::owner),
            Codec.STRING.fieldOf("owner_name").forGetter(CardOwner::ownerName)
    ).apply(instance, CardOwner::new));

    public static final StreamCodec<ByteBuf, CardOwner> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CardOwner::owner,
            ByteBufCodecs.STRING_UTF8, CardOwner::ownerName,
            CardOwner::new);
}
