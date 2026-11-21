package com.atomcraft.tools;

import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.atomcraft.wireless.Wireless;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Eventos das ferramentas: o Desintegrador manda os blocos quebrados para a rede como elementos. */
public final class ToolEvents {

    private ToolEvents() {}

    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getTool().getItem() instanceof AtomicDisintegratorItem) || !AtomicDisintegratorItem.atomizes(event.getTool())) return;
        if (!(event.getBreaker() instanceof Player player)) return;
        WirelessTransmitterBlockEntity transmitter = Wireless.find(player);
        if (transmitter == null) return;
        // O que couber vira elementos; o resto cai normalmente.
        event.getDrops().removeIf((ItemEntity drop) -> AtomicDisintegratorItem.atomize(transmitter, drop.getItem()));
    }

    /** Toque Suave e Fortuna do desintegrador valem como os encantamentos (sem aparecer no item). */
    public static void onEnchantmentLevel(net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent event) {
        if (!(event.getStack() instanceof net.minecraft.world.item.ItemStack stack) || !(stack.getItem() instanceof AtomicDisintegratorItem)) return;
        int silk = com.atomcraft.equipment.ModularItem.level(stack, com.atomcraft.equipment.EquipmentModule.SILK_TOUCH);
        int fortune = com.atomcraft.equipment.ModularItem.level(stack, com.atomcraft.equipment.EquipmentModule.FORTUNE);
        if (silk > 0 && event.isTargetting(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)) {
            event.getHolder(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH).ifPresent(h -> event.getEnchantments().set(h, silk));
        }
        if (fortune > 0 && event.isTargetting(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)) {
            event.getHolder(net.minecraft.world.item.enchantment.Enchantments.FORTUNE).ifPresent(h -> event.getEnchantments().set(h, fortune));
        }
    }
}
