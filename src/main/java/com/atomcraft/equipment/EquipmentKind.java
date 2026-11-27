package com.atomcraft.equipment;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Equipamentos que aceitam aprimoramentos (instalados na Bancada de Aprimoramentos), quantos slots cada um tem e
 * até que nível o Aprimoramento de Encantamento pode somar nele.
 */
public enum EquipmentKind {
    EXO_HELMET("exo_helmet", 2, 2, null),
    EXO_CHESTPLATE("exo_chestplate", 3, 2, null),
    EXO_LEGGINGS("exo_leggings", 2, 2, null),
    EXO_BOOTS("exo_boots", 2, 2, null),
    JETPACK("jetpack", 2, 2, null),
    DISINTEGRATOR("disintegrator", 3, 2, null),
    // Armadura Nano Desgastada: aceita os mesmos aprimoramentos da peça equivalente do Exoesqueleto
    NANO_HELMET("nano_helmet", 3, 3, EXO_HELMET),
    NANO_CHESTPLATE("nano_chestplate", 3, 3, EXO_CHESTPLATE),
    NANO_LEGGINGS("nano_leggings", 3, 3, EXO_LEGGINGS),
    NANO_BOOTS("nano_boots", 3, 3, EXO_BOOTS),
    KATANA("katana", 3, 3, null);

    /** Maior quantidade de slots de aprimoramento entre os equipamentos (slots da bancada). */
    public static final int MAX_SLOTS = 3;

    private final String id;
    private final int slots;
    private final int enchantCap;
    private final @Nullable EquipmentKind base;

    EquipmentKind(String id, int slots, int enchantCap, @Nullable EquipmentKind base) {
        this.id = id;
        this.slots = slots;
        this.enchantCap = enchantCap;
        this.base = base;
    }

    public String id() {
        return id;
    }

    public int slots() {
        return slots;
    }

    /** Nível máximo que o Aprimoramento de Encantamento soma em cada encantamento deste equipamento. */
    public int enchantCap() {
        return enchantCap;
    }

    /** Peça do Exoesqueleto cujos aprimoramentos esta também aceita (ou null). */
    public @Nullable EquipmentKind base() {
        return base;
    }

    public Component label() {
        return Component.translatable("equipment_kind.atomcraft." + id);
    }
}
