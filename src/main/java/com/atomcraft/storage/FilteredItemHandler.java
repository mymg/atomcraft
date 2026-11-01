package com.atomcraft.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.BiPredicate;
import java.util.function.ToIntBiFunction;

/**
 * Inventário das máquinas, com filtro de itens aceitos, limite por slot e aviso
 * de alteração. Também cria as visões usadas pela automação (pipes, hoppers).
 */
public class FilteredItemHandler extends ItemStacksResourceHandler {

    private final int expectedSize;
    private final BiPredicate<Integer, ItemResource> filter;
    private final Runnable onChange;
    private final int maxPerSlot;
    private ToIntBiFunction<Integer, ItemResource> capacityLimit = (slot, resource) -> Integer.MAX_VALUE;

    public FilteredItemHandler(int size, int maxPerSlot, BiPredicate<Integer, ItemResource> filter, Runnable onChange) {
        super(size);
        this.expectedSize = size;
        this.filter = filter;
        this.onChange = onChange;
        this.maxPerSlot = maxPerSlot;
    }

    public FilteredItemHandler(int size) {
        this(size, 99, (slot, resource) -> true, () -> {});
    }

    /**
     * Limite extra de capacidade por slot e item (ex.: o Analisador só aceita o
     * que falta para concluir a pesquisa). Vale para jogador, shift-clique e automação.
     */
    public FilteredItemHandler withCapacityLimit(ToIntBiFunction<Integer, ItemResource> limit) {
        this.capacityLimit = limit;
        return this;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.isEmpty() || filter.test(index, resource);
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        int capacity = Math.min(maxPerSlot, super.getCapacity(index, resource));
        if (resource.isEmpty()) return capacity;
        return Math.max(0, Math.min(capacity, capacityLimit.applyAsInt(index, resource)));
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onChange.run();
    }

    /** Ao carregar um mundo salvo com outro número de slots, ajusta sem perder itens. */
    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        if (size() != expectedSize) {
            NonNullList<ItemStack> resized = NonNullList.withSize(expectedSize, ItemStack.EMPTY);
            NonNullList<ItemStack> current = copyToList();
            for (int i = 0; i < Math.min(expectedSize, current.size()); i++) resized.set(i, current.get(i));
            setStacks(resized);
        }
    }

    public ItemStack getStack(int index) {
        return getResource(index).toStack(getAmountAsInt(index));
    }

    /** Soma de um item em todos os slots, exceto {@code exceptIndex}. */
    public int countOf(ItemResource resource, int exceptIndex) {
        int total = 0;
        for (int i = 0; i < size(); i++) {
            if (i != exceptIndex && getResource(i).getItem() == resource.getItem()) total += getAmountAsInt(i);
        }
        return total;
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
