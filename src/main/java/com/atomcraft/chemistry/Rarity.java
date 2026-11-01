package com.atomcraft.chemistry;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Raridade de um item no AtomCraft. Define:
 * <ul>
 *     <li>quantas unidades precisam ser pesquisadas no Analisador;</li>
 *     <li>o multiplicador de energia e de tempo no Replicador.</li>
 * </ul>
 */
public enum Rarity implements StringRepresentable {
    COMMON(50, 1, 1.0, ChatFormatting.WHITE, 0xFFE0E0E0),
    UNCOMMON(20, 2, 1.5, ChatFormatting.YELLOW, 0xFFFFE066),
    RARE(5, 4, 2.0, ChatFormatting.AQUA, 0xFF66E0FF),
    EPIC(2, 8, 3.0, ChatFormatting.LIGHT_PURPLE, 0xFFE080FF),
    LEGENDARY(1, 16, 4.0, ChatFormatting.GOLD, 0xFFFFB833);

    private static final Rarity[] VALUES = values();

    public static final Codec<Rarity> CODEC = StringRepresentable.fromEnum(Rarity::values);
    public static final StreamCodec<ByteBuf, Rarity> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> VALUES[i], Enum::ordinal);

    private final int researchCount;
    private final double energyMultiplier;
    private final double timeMultiplier;
    private final ChatFormatting color;
    private final int argb;

    Rarity(int researchCount, double energyMultiplier, double timeMultiplier, ChatFormatting color, int argb) {
        this.argb = argb;
        this.researchCount = researchCount;
        this.energyMultiplier = energyMultiplier;
        this.timeMultiplier = timeMultiplier;
        this.color = color;
    }

    public int researchCount() {
        return researchCount;
    }

    public double energyMultiplier() {
        return energyMultiplier;
    }

    public double timeMultiplier() {
        return timeMultiplier;
    }

    public ChatFormatting color() {
        return color;
    }

    /** Cor ARGB para textos nas interfaces. */
    public int argb() {
        return argb;
    }

    public String translationKey() {
        return "rarity.atomcraft." + getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Rarity max(Rarity a, Rarity b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    public static Rarity fromVanilla(net.minecraft.world.item.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> COMMON;
            case UNCOMMON -> UNCOMMON;
            case RARE -> RARE;
            case EPIC -> EPIC;
        };
    }
}
