package com.atomcraft.chemistry;

import com.atomcraft.AtomCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Tabela final de valores atômicos (base + derivados das receitas).
 * O servidor calcula; o cliente recebe uma cópia pela rede.
 */
public final class AtomicValues {

    private static Map<Item, AtomicValue> serverValues = Map.of();
    private static Map<Item, AtomicValue> clientValues = Map.of();

    private AtomicValues() {}

    public static @Nullable AtomicValue get(Item item, boolean clientSide) {
        return (clientSide ? clientValues : serverValues).get(item);
    }

    public static @Nullable AtomicValue get(Level level, Item item) {
        return get(item, level.isClientSide());
    }

    public static Map<Item, AtomicValue> serverValues() {
        return serverValues;
    }

    public static Map<Item, AtomicValue> clientValues() {
        return clientValues;
    }

    public static void setClientValues(Map<Item, AtomicValue> values) {
        clientValues = Map.copyOf(values);
    }

    /**
     * Composição obtida ao atomizar esta pilha (uma unidade). Itens
     * danificados rendem proporcionalmente à durabilidade restante.
     */
    public static @Nullable Composition atomizeResult(ItemStack stack, boolean clientSide) {
        AtomicValue value = get(stack.getItem(), clientSide);
        if (value == null) return null;
        Composition composition = value.composition();
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            double remaining = 1.0 - (double) stack.getDamageValue() / stack.getMaxDamage();
            composition = composition.scale(remaining);
        }
        return composition.isEmpty() ? null : composition;
    }

    public static Rarity vanillaRarity(Item item) {
        return Rarity.fromVanilla(item.components().getOrDefault(DataComponents.RARITY, net.minecraft.world.item.Rarity.COMMON));
    }

    /** Recalcula a tabela. Chamado ao iniciar o servidor e a cada /reload. */
    public static void recompute(MinecraftServer server) {
        Map<Item, AtomicValue> base = new HashMap<>();
        var entries = CompositionManager.entries();

        // Tags primeiro, para que entradas de item específicas tenham prioridade.
        for (var entry : entries) {
            if (entry.tag().isEmpty()) continue;
            TagKey<Item> tag = TagKey.create(Registries.ITEM, entry.tag().get());
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                base.put(holder.value(), baseValue(holder.value(), entry));
            }
        }
        for (var entry : entries) {
            if (entry.item().isEmpty()) continue;
            BuiltInRegistries.ITEM.getOptional(entry.item().get())
                    .ifPresent(item -> base.put(item, baseValue(item, entry)));
        }

        Map<Item, AtomicValue> all = RecipeDeriver.derive(server, base);
        serverValues = Map.copyOf(all);
        AtomCraft.LOGGER.info("AtomCraft: {} itens com valor atômico ({} base, {} derivados das receitas)",
                all.size(), base.size(), all.size() - base.size());
    }

    private static AtomicValue baseValue(Item item, CompositionManager.Entry entry) {
        return new AtomicValue(entry.elements(), entry.rarity().orElseGet(() -> vanillaRarity(item)));
    }
}
