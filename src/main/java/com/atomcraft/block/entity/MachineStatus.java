package com.atomcraft.block.entity;

import net.minecraft.network.chat.Component;

/** Situação de uma máquina, exibida na interface. */
public enum MachineStatus {
    IDLE(0xFF707070),
    WORKING(0xFF2E8B2E),
    NO_STORAGE(0xFFB03030),
    STORAGE_FULL(0xFFB03030),
    NO_OWNER(0xFFB03030),
    NO_ENERGY(0xFFB06000),
    NO_ELEMENTS(0xFFB06000),
    OUTPUT_FULL(0xFFB06000),
    NOT_RESEARCHED(0xFFB03030),
    NOTHING_TO_RESEARCH(0xFF707070),
    NO_FUEL(0xFFB06000),
    DISABLED(0xFFB03030);

    private static final MachineStatus[] VALUES = values();

    private final int color;

    MachineStatus(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    public Component label() {
        return Component.translatable("status.atomcraft." + name().toLowerCase());
    }

    public static MachineStatus byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : IDLE;
    }
}
