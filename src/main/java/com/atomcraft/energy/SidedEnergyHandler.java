package com.atomcraft.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Visão de um armazenamento de energia que só deixa entrar, só deixa sair, ou os dois (conforme o lado). */
public record SidedEnergyHandler(EnergyHandler delegate, boolean canInsert, boolean canExtract) implements EnergyHandler {

    @Override
    public long getAmountAsLong() {
        return delegate.getAmountAsLong();
    }

    @Override
    public long getCapacityAsLong() {
        return delegate.getCapacityAsLong();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        return canInsert ? delegate.insert(amount, transaction) : 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return canExtract ? delegate.extract(amount, transaction) : 0;
    }
}
