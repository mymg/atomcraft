package com.atomcraft.upgrade;

/** Tipos de chip de upgrade. */
public enum UpgradeType {
    /** Máquinas trabalham mais rápido; geradores queimam mais rápido. */
    SPEED,
    /** Máquinas gastam menos FE; geradores rendem mais FE por combustível. */
    EFFICIENCY;

    public String translationKey() {
        return "upgrade.atomcraft." + name().toLowerCase();
    }
}
