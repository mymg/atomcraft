package com.atomcraft.equipment;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aprimoramento de Encantamento: os encantamentos dele somam aos do equipamento onde está instalado,
 * até o limite do equipamento ({@link EquipmentKind#enchantCap()}: II no Exoesqueleto, Jetpack e
 * Desintegrador; III na Armadura e Katana Nano). Ex.: aprimoramento com Proteção III num peitoral Nano com
 * Proteção II = Proteção V.
 */
public final class AugmentEnchantments {

    private AugmentEnchantments() {}

    /** Níveis somados por encantamento (já com o limite do equipamento). Vazio se não há aprimoramento. */
    public static Map<Holder<Enchantment>, Integer> bonus(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularItem modular)) return Map.of();
        Map<Holder<Enchantment>, Integer> bonus = new LinkedHashMap<>();
        for (ItemStack installed : ModularItem.modules(stack)) {
            if (!(installed.getItem() instanceof EquipmentModuleItem item) || item.module() != EquipmentModule.ENCHANTMENT) continue;
            ItemEnchantments enchantments = installed.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var entry : enchantments.entrySet()) bonus.merge(entry.getKey(), entry.getIntValue(), Integer::sum);
        }
        int cap = modular.kind().enchantCap();
        bonus.replaceAll((enchantment, level) -> Math.min(level, cap));
        return bonus;
    }

    /** Soma os níveis do aprimoramento no cálculo do jogo (proteção, afiação, eficiência...). */
    public static void onEnchantmentLevel(net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent event) {
        if (!(event.getStack() instanceof ItemStack stack)) return;
        Map<Holder<Enchantment>, Integer> bonus = bonus(stack);
        if (bonus.isEmpty()) return;
        var levels = event.getEnchantments();
        for (var entry : bonus.entrySet()) {
            levels.set(entry.getKey(), levels.getLevel(entry.getKey()) + entry.getValue());
        }
    }
}
