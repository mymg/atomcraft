package com.atomcraft.wireless;

import com.atomcraft.Config;
import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.atomcraft.registry.ModComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Acesso sem fio à rede: procura no inventário do jogador um Cartão de Rede vinculado a ele e a um
 * Transmissor Sem Fio ligado, ao alcance. Ferramentas e equipamentos usam o transmissor encontrado
 * para tirar energia e guardar/usar elementos da rede.
 */
public final class Wireless {

    private Wireless() {}

    /** Itens onde procurar cartões (inventário; os acessórios do Curios entram aqui depois). */
    private static List<ItemStack> candidates(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) stacks.add(inventory.getItem(i));
        stacks.addAll(com.atomcraft.compat.Curios.equipped(player));
        return stacks;
    }

    /** Transmissor alcançável pelo jogador, ou null. */
    public static @Nullable WirelessTransmitterBlockEntity find(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        for (ItemStack stack : candidates(player)) {
            WirelessLink link = stack.get(ModComponents.WIRELESS_LINK.get());
            if (link == null || !link.owner().equals(player.getUUID())) continue;
            WirelessTransmitterBlockEntity transmitter = resolve(level, player, link);
            if (transmitter != null) return transmitter;
        }
        return null;
    }

    /** Transmissor do vínculo, se existir, estiver ligado e ao alcance. */
    public static @Nullable WirelessTransmitterBlockEntity resolve(ServerLevel level, Player player, WirelessLink link) {
        ServerLevel target = level.getServer().getLevel(link.dimension());
        if (target == null) return null;
        if (target != level && !Config.WIRELESS_CROSS_DIMENSION.get()) return null;
        int range = Config.WIRELESS_RANGE.get();
        if (target == level && range >= 0 && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(link.pos())) > (double) range * range) return null;
        if (!target.isLoaded(link.pos())) return null;
        if (target.getBlockEntity(link.pos()) instanceof WirelessTransmitterBlockEntity transmitter && transmitter.isEnabled()) {
            return transmitter;
        }
        return null;
    }

    /** Tira {@code amount} FE do transmissor; falso (sem tirar nada) se não houver o bastante. */
    public static boolean drain(WirelessTransmitterBlockEntity transmitter, int amount) {
        var energy = transmitter.energy();
        if (energy.getAmountAsInt() < amount) return false;
        energy.set(energy.getAmountAsInt() - amount);
        return true;
    }
}
