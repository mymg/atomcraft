package com.atomcraft.fluid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.util.function.Predicate;

/** Tanque de um fluido só, com filtro e aviso de alteração (tanque, bomba, atomizador). */
public class FluidTank extends FluidStacksResourceHandler {

    private final Predicate<FluidResource> filter;
    private final Runnable onChange;

    public FluidTank(int capacity, Predicate<FluidResource> filter, Runnable onChange) {
        super(1, capacity);
        this.filter = filter;
        this.onChange = onChange;
    }

    public FluidTank(int capacity, Runnable onChange) {
        this(capacity, resource -> true, onChange);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.isEmpty() || filter.test(resource);
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        onChange.run();
    }

    public FluidResource resource() {
        return getResource(0);
    }

    public Fluid fluid() {
        return getResource(0).isEmpty() ? Fluids.EMPTY : getResource(0).getFluid();
    }

    public int amount() {
        return getAmountAsInt(0);
    }

    public int capacity() {
        return getCapacityAsInt(0, FluidResource.EMPTY);
    }

    public boolean isEmpty() {
        return amount() <= 0 || getResource(0).isEmpty();
    }

    public FluidStack stack() {
        return isEmpty() ? FluidStack.EMPTY : getResource(0).toStack(amount());
    }

    public void setStack(FluidStack stack) {
        if (stack.isEmpty()) {
            set(0, FluidResource.EMPTY, 0);
        } else {
            set(0, FluidResource.of(stack), Math.min(stack.getAmount(), capacity()));
        }
    }

    /** Tira {@code amount} mB direto (sem transação), para o consumo interno das máquinas. */
    public void drain(int amount) {
        int left = amount() - amount;
        if (left <= 0) {
            set(0, FluidResource.EMPTY, 0);
        } else {
            set(0, resource(), left);
        }
    }

    /** Visão para automação que só aceita fluido (entrada do Atomizador). */
    public net.neoforged.neoforge.transfer.ResourceHandler<FluidResource> insertOnly() {
        return new net.neoforged.neoforge.transfer.DelegatingResourceHandler<>(this) {
            @Override
            public int extract(int index, FluidResource resource, int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
                return 0;
            }

            @Override
            public int extract(FluidResource resource, int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
                return 0;
            }
        };
    }

    /** Visão para automação que só deixa tirar (saída da Bomba). */
    public net.neoforged.neoforge.transfer.ResourceHandler<FluidResource> extractOnly() {
        return new net.neoforged.neoforge.transfer.DelegatingResourceHandler<>(this) {
            @Override
            public boolean isValid(int index, FluidResource resource) {
                return resource.isEmpty();
            }

            @Override
            public int insert(int index, FluidResource resource, int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
                return 0;
            }

            @Override
            public int insert(FluidResource resource, int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
                return 0;
            }
        };
    }

    /** Id numérico do fluido, para sincronizar com a interface (ContainerData). */
    public int fluidId() {
        return isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(fluid());
    }
}
