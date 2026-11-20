package com.atomcraft.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * Integração opcional com o Curios: acessórios (anel, costas, amuleto) também contam como equipados.
 * Sem o Curios instalado, tudo aqui devolve listas vazias e nenhuma classe do Curios é carregada.
 */
public final class Curios {

    public static final boolean LOADED = ModList.get().isLoaded("curios");

    private Curios() {}

    /** Itens nos slots do Curios da entidade. */
    public static List<ItemStack> equipped(LivingEntity entity) {
        return LOADED ? CuriosCompat.equipped(entity) : List.of();
    }

    /** Coloca um item num slot do Curios (para o autoteste). Falso se não der. */
    public static boolean equip(LivingEntity entity, String slot, ItemStack stack) {
        return LOADED && CuriosCompat.equip(entity, slot, stack);
    }

    /** Se o tipo de slot existe para jogadores (para o autoteste). */
    public static boolean hasPlayerSlot(net.minecraft.world.level.Level level, String slot) {
        return LOADED && CuriosCompat.hasPlayerSlot(level, slot);
    }
}
