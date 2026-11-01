package com.atomcraft.storage;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/** Máquina que recebe energia dos geradores ligados à mesma rede (pelo Cabo Atômico). */
public interface EnergyReceiver {

    EnergyHandler energy();

    boolean isRemoved();
}
