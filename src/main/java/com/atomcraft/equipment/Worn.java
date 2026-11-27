package com.atomcraft.equipment;

import com.atomcraft.compat.Curios;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Onde procurar equipamentos e acessórios do jogador: armadura, inventário e slots do Curios. */
public final class Worn {

    private Worn() {}

    /** Jetpack vestido (peito ou costas do Curios). */
    public static ItemStack jetpack(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof JetpackItem) return chest;
        for (ItemStack stack : Curios.equipped(player)) {
            if (stack.getItem() instanceof JetpackItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Peça do exoesqueleto vestida no slot, ou vazio. */
    public static ItemStack exo(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getItem() instanceof ExoArmorItem ? stack : ItemStack.EMPTY;
    }

    /** Todos os itens carregados: inventário (com armadura e mão secundária) e Curios. */
    public static List<ItemStack> carried(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stacks.stream().noneMatch(s -> s == stack)) stacks.add(stack);
        }
        stacks.addAll(Curios.equipped(player));
        return stacks;
    }

    public static @Nullable ItemStack find(Player player, Predicate<ItemStack> predicate) {
        for (ItemStack stack : carried(player)) {
            if (predicate.test(stack)) return stack;
        }
        return null;
    }

    /** Se o jogador vê a composição dos blocos (Óculos Analisadores ou capacete com o módulo Analisador). */
    public static boolean hasAnalyzer(Player player) {
        if (ModularItem.level(exo(player, EquipmentSlot.HEAD), EquipmentModule.ANALYZER) > 0) return true;
        return find(player, stack -> stack.getItem() instanceof AnalyzerGlassesItem) != null;
    }
}
