package com.atomcraft.equipment;

import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Módulos instaláveis nos equipamentos. Cada nível é um item no slot (até {@link #maxLevel()} no mesmo slot);
 * {@link #energyPerTick()} é o gasto por nível enquanto o módulo funciona (0 = passivo).
 */
public enum EquipmentModule {
    NIGHT_VISION("night_vision", 1, 4, EquipmentKind.EXO_HELMET),
    RESPIRATOR("respirator", 1, 4, EquipmentKind.EXO_HELMET),
    ANALYZER("analyzer", 1, 0, EquipmentKind.EXO_HELMET),
    PLATING("plating", 3, 0, EquipmentKind.EXO_CHESTPLATE, EquipmentKind.EXO_LEGGINGS),
    SHIELD("shield", 1, 0, EquipmentKind.EXO_CHESTPLATE),
    SPEED("speed", 3, 2, EquipmentKind.EXO_LEGGINGS),
    STEP_ASSIST("step_assist", 1, 1, EquipmentKind.EXO_LEGGINGS),
    JUMP("jump", 3, 1, EquipmentKind.EXO_BOOTS),
    SHOCK_ABSORBER("shock_absorber", 3, 0, EquipmentKind.EXO_BOOTS),
    THRUSTER("thruster", 3, 0, EquipmentKind.JETPACK),
    HOVER("hover", 1, 0, EquipmentKind.JETPACK),
    EFFICIENCY("efficiency", 3, 0, EquipmentKind.DISINTEGRATOR),
    AREA("area", 1, 0, EquipmentKind.DISINTEGRATOR);

    private final String id;
    private final int maxLevel;
    private final int energyPerTick;
    private final Set<EquipmentKind> kinds;

    EquipmentModule(String id, int maxLevel, int energyPerTick, EquipmentKind first, EquipmentKind... rest) {
        this.id = id;
        this.maxLevel = maxLevel;
        this.energyPerTick = energyPerTick;
        this.kinds = EnumSet.of(first, rest);
    }

    public String id() {
        return id;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public int energyPerTick() {
        return energyPerTick;
    }

    public boolean fits(EquipmentKind kind) {
        return kinds.contains(kind);
    }

    public Set<EquipmentKind> kinds() {
        return kinds;
    }

    public Component label() {
        return Component.translatable("module.atomcraft." + id);
    }

    public Component description() {
        return Component.translatable("module.atomcraft." + id + ".desc");
    }
}
