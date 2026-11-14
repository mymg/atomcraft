package com.atomcraft.replication;

import com.atomcraft.Config;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Níveis do Replicador. Cada nível só replica itens até certa complexidade:
 * quantos elementos diferentes a composição tem e se pode haver elementos exóticos.
 * Os limites vêm da configuração ({@code atomcraft-common.toml}), então modpacks escolhem a experiência.
 */
public enum ReplicatorTier {
    BASIC, ADVANCED, QUANTUM;

    private static final ReplicatorTier[] VALUES = values();

    /** Máximo de elementos diferentes (-1 = sem limite). */
    public int maxElements() {
        return switch (this) {
            case BASIC -> Config.REPLICATOR_MAX_ELEMENTS.get();
            case ADVANCED -> Config.ADVANCED_MAX_ELEMENTS.get();
            case QUANTUM -> Config.QUANTUM_MAX_ELEMENTS.get();
        };
    }

    public boolean allowsExotics() {
        return switch (this) {
            case BASIC -> Config.REPLICATOR_EXOTICS.get();
            case ADVANCED -> Config.ADVANCED_EXOTICS.get();
            case QUANTUM -> Config.QUANTUM_EXOTICS.get();
        };
    }

    public String translationKey() {
        return "replicator_tier.atomcraft." + name().toLowerCase();
    }

    public static ReplicatorTier byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : BASIC;
    }

    /** Elementos diferentes na composição. */
    public static int complexity(Composition composition) {
        return composition.elements().size();
    }

    public static boolean hasExotics(Composition composition) {
        return composition.elements().keySet().stream().anyMatch(ChemicalElement::exotic);
    }

    /**
     * Motivo para não replicar com estes limites, ou {@code null} se pode.
     * Os limites são passados à parte porque o cliente os recebe do servidor (pela interface).
     */
    public static @Nullable Component refusal(Composition composition, int maxElements, boolean exotics) {
        int count = complexity(composition);
        if (maxElements >= 0 && count > maxElements) {
            return Component.translatable("message.atomcraft.too_complex", count, maxElements);
        }
        if (!exotics && hasExotics(composition)) {
            return Component.translatable("message.atomcraft.needs_exotics");
        }
        return null;
    }

    public @Nullable Component refusal(Composition composition) {
        return refusal(composition, maxElements(), allowsExotics());
    }
}
