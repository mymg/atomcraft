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
}
