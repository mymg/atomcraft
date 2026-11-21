package com.atomcraft.equipment;

import net.minecraft.network.chat.Component;

/** Equipamentos que aceitam módulos (instalados na Bancada de Módulos) e quantos slots cada um tem. */
public enum EquipmentKind {
    EXO_HELMET("exo_helmet", 2),
    EXO_CHESTPLATE("exo_chestplate", 3),
    EXO_LEGGINGS("exo_leggings", 2),
    EXO_BOOTS("exo_boots", 2),
    JETPACK("jetpack", 2),
    DISINTEGRATOR("disintegrator", 3);

    /** Maior quantidade de slots de módulo entre os equipamentos (slots da bancada). */
    public static final int MAX_SLOTS = 3;

    private final String id;
    private final int slots;

    EquipmentKind(String id, int slots) {
        this.id = id;
        this.slots = slots;
    }

    public String id() {
        return id;
    }

    public int slots() {
        return slots;
    }

    public Component label() {
        return Component.translatable("equipment_kind.atomcraft." + id);
    }
}
