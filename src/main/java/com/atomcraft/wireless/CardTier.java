package com.atomcraft.wireless;

import com.atomcraft.Config;
import net.minecraft.network.chat.Component;

/** Níveis do Cartão de Rede: alcance do transmissor até o jogador. */
public enum CardTier {
    BASIC("network_card"),
    ADVANCED("advanced_network_card"),
    INFINITE("infinite_network_card"),
    DIMENSIONAL("dimensional_network_card");

    private final String id;

    CardTier(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Alcance em blocos na mesma dimensão (-1 = sem limite). */
    public int range() {
        return switch (this) {
            case BASIC -> Config.CARD_BASIC_RANGE.get();
            case ADVANCED -> Config.CARD_ADVANCED_RANGE.get();
            default -> -1;
        };
    }

    public boolean crossDimension() {
        return this == DIMENSIONAL;
    }

    public Component rangeText() {
        if (crossDimension()) return Component.translatable("tooltip.atomcraft.card_range_dimensional");
        int range = range();
        return range < 0 ? Component.translatable("tooltip.atomcraft.card_range_infinite")
                : Component.translatable("tooltip.atomcraft.card_range", range);
    }
}
