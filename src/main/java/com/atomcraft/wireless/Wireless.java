package com.atomcraft.wireless;

import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Acesso sem fio à rede: procura um Transmissor Sem Fio carregado, ligado e com um Cartão de Rede do
 * jogador num dos slots, cujo nível alcance o jogador. Ferramentas e equipamentos usam o transmissor
 * encontrado para tirar energia e guardar/usar elementos da rede.
 */
public final class Wireless {

    /** Transmissores carregados no servidor. */
    private static final Set<WirelessTransmitterBlockEntity> TRANSMITTERS = Collections.newSetFromMap(new WeakHashMap<>());

    private Wireless() {}

    public static void register(WirelessTransmitterBlockEntity transmitter) {
        if (transmitter.getLevel() instanceof ServerLevel) TRANSMITTERS.add(transmitter);
    }

    public static void unregister(WirelessTransmitterBlockEntity transmitter) {
        TRANSMITTERS.remove(transmitter);
    }

    /** Transmissor que alcança o jogador (o que tiver mais energia), ou null. */
    public static @Nullable WirelessTransmitterBlockEntity find(Player player) {
        if (!(player.level() instanceof ServerLevel)) return null;
        WirelessTransmitterBlockEntity best = null;
        for (WirelessTransmitterBlockEntity transmitter : TRANSMITTERS) {
            if (transmitter.isRemoved() || !transmitter.isEnabled() || !reaches(transmitter, player)) continue;
            if (best == null || transmitter.energy().getAmountAsInt() > best.energy().getAmountAsInt()) best = transmitter;
        }
        return best;
    }

    /** Se algum cartão do transmissor é do jogador e o alcança. */
    public static boolean reaches(WirelessTransmitterBlockEntity transmitter, Player player) {
        for (int i = 0; i < transmitter.cards().size(); i++) {
            ItemStack stack = transmitter.cards().getStack(i);
            if (stack.getItem() instanceof NetworkCardItem card && reaches(transmitter, card.tier(), NetworkCardItem.owner(stack), player)) return true;
        }
        return false;
    }

    public static boolean reaches(WirelessTransmitterBlockEntity transmitter, CardTier tier, @Nullable CardOwner owner, Player player) {
        if (owner == null || !owner.owner().equals(player.getUUID()) || transmitter.getLevel() == null) return false;
        if (transmitter.getLevel() != player.level()) return tier.crossDimension();
        int range = tier.range();
        return range < 0 || player.distanceToSqr(Vec3.atCenterOf(transmitter.getBlockPos())) <= (double) range * range;
    }

    /** Tira {@code amount} FE do transmissor; falso (sem tirar nada) se não houver o bastante. */
    public static boolean drain(WirelessTransmitterBlockEntity transmitter, int amount) {
        var energy = transmitter.energy();
        if (energy.getAmountAsInt() < amount) return false;
        energy.set(energy.getAmountAsInt() - amount);
        return true;
    }
}
