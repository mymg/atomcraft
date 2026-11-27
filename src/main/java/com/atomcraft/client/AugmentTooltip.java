package com.atomcraft.client;

import com.atomcraft.equipment.AugmentEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Map;

/**
 * Tooltip dos equipamentos com Aprimoramento de Encantamento: a linha do encantamento mostra o nível total
 * (o do item + o do aprimoramento), com o bônus entre parênteses. Ex.: "Proteção V (+III)".
 */
public final class AugmentTooltip {

    private AugmentTooltip() {}

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Map<Holder<Enchantment>, Integer> bonus = AugmentEnchantments.bonus(stack);
        if (bonus.isEmpty()) return;
        ItemEnchantments base = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        List<Component> lines = event.getToolTip();
        int insertAt = Math.min(1, lines.size());
        for (var entry : bonus.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int baseLevel = base.getLevel(enchantment);
            Component line = Enchantment.getFullname(enchantment, baseLevel + entry.getValue()).copy()
                    .append(Component.translatable("tooltip.atomcraft.enchant_bonus",
                            Component.translatable("enchantment.level." + entry.getValue())).withStyle(ChatFormatting.AQUA));
            int existing = -1;
            if (baseLevel > 0) {
                String old = Enchantment.getFullname(enchantment, baseLevel).getString();
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).getString().equals(old)) {
                        existing = i;
                        break;
                    }
                }
            }
            if (existing >= 0) {
                lines.set(existing, line);
                insertAt = Math.max(insertAt, existing + 1);
            } else {
                lines.add(insertAt++, line);
            }
        }
    }
}
