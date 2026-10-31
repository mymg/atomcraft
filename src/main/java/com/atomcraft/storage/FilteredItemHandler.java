package com.atomcraft.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.BiPredicate;

/**
 * Inventário das máquinas, com filtro de itens aceitos, limite por slot e aviso
 * de alteração. Também cria as visões usadas pela automação (pipes, hoppers).
 */
public class FilteredItemHandler extends ItemStacksResourceHandler {

    private final BiPredicate<Integer, ItemResource> filter;
    private final Runnable onChange;
    private final int maxPerSlot;

    public FilteredItemHandler(int size, int maxPerSlot, BiPredicate<Integer, ItemResource> filter, Runnable onChange) {
        super(size);
        this.filter = filter;
        this.onChange = onChange;
        this.maxPerSlot = maxPerSlot;
    }

    public FilteredItemHandler(int size) {
        this(size, 99, (slot, resource) -> true, () -> {});
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.isEmpty() || filter.test(index, resource);
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return Math.min(maxPerSlot, super.getCapacity(index, resource));
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChange.run();
    }

    public ItemStack getStack(int index) {
        return getResource(index).toStack(getAmountAsInt(index));
    }

    /** Diminui a pilha do slot em {@code amount} unidades. */
    public void shrink(int index, int amount) {
        int left = getAmountAsInt(index) - amount;
        set(index, left > 0 ? getResource(index) : ItemResource.EMPTY, Math.max(0, left));
    }

    /** Visão para automação que só permite inserir (buffer de entrada). */
    public ResourceHandler<ItemResource> insertOnly() {
        return new DelegatingResourceHandler<>(this) {
            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }

            @Override
            public int extract(ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }
        };
    }

    /** Visão para automação que só permite retirar (buffer de saída). */
    public ResourceHandler<ItemResource> extractOnly() {
        return new DelegatingResourceHandler<>(this) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return resource.isEmpty();
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }

            @Override
            public int insert(ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }
        };
    }
}
