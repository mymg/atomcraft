package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.storage.EnergyReceiver;
import com.atomcraft.upgrade.UpgradeType;
import com.atomcraft.upgrade.Upgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Base dos geradores: buffer de energia e distribuição automática para as
 * máquinas da mesma rede (pelo Cabo Atômico) e para blocos vizinhos que
 * aceitem FE (cabos e máquinas de outros mods).
 */
public abstract class GeneratorBlockEntity extends MachineBlockEntity {

    protected final SimpleEnergyHandler energy = new SimpleEnergyHandler(
            Config.GENERATOR_CAPACITY.get(), 0, Config.GENERATOR_MAX_OUTPUT.get()) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    /** FE gerado no último tick (para a interface). */
    protected int lastGenerated;

    protected GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public SimpleEnergyHandler energy() {
        return energy;
    }

    /** Gera energia neste tick e retorna quanto foi gerado. */
    protected abstract int generate();

    protected int room() {
        return energy.getCapacityAsInt() - energy.getAmountAsInt();
    }

    protected void addEnergy(int amount) {
        if (amount > 0) energy.set(Math.min(energy.getCapacityAsInt(), energy.getAmountAsInt() + amount));
    }

    @Override
    public void serverTick() {
        lastGenerated = generate();
        distribute();
    }

    @Override
    protected void disabledTick() {
        lastGenerated = 0;
        distribute();
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return true;
    }

    /** Geração por tick com os chips de velocidade. */
    protected int boostedRate(int baseRate) {
        return (int) Math.round(baseRate * Upgrades.generatorRate(upgradeCount(UpgradeType.SPEED)));
    }

    /** FE por unidade de combustível com os chips de eficiência. */
    protected long boostedYield(long baseEnergy) {
        return Math.round(baseEnergy * Upgrades.generatorYield(upgradeCount(UpgradeType.EFFICIENCY)));
    }

    private void distribute() {
        if (level == null) return;
        int available = Math.min(energy.getAmountAsInt(), Config.GENERATOR_MAX_OUTPUT.get());
        if (available <= 0) return;

        List<EnergyHandler> targets = new ArrayList<>();
        for (EnergyReceiver receiver : network().receivers()) {
            if (!receiver.isRemoved()) targets.add(receiver.energy());
        }
        for (Direction direction : Direction.values()) {
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if (handler != null && handler != energy && !targets.contains(handler)) targets.add(handler);
        }
        if (targets.isEmpty()) return;

        int remaining = available;
        try (Transaction transaction = Transaction.openRoot()) {
            for (EnergyHandler target : targets) {
                if (remaining <= 0) break;
                remaining -= target.insert(remaining, transaction);
            }
            transaction.commit();
        }
        int sent = available - remaining;
        if (sent > 0) energy.set(energy.getAmountAsInt() - sent);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
    }

    /** Dados comuns sincronizados com a interface. */
    protected int commonData(int index) {
        return switch (index) {
            case 0 -> energy.getAmountAsInt() & 0xFFFF;
            case 1 -> (energy.getAmountAsInt() >>> 16) & 0xFFFF;
            case 2 -> energy.getCapacityAsInt() & 0xFFFF;
            case 3 -> (energy.getCapacityAsInt() >>> 16) & 0xFFFF;
            case 4 -> lastGenerated & 0xFFFF;
            case 5 -> (lastGenerated >>> 16) & 0xFFFF;
            case 6 -> status.ordinal();
            default -> 0;
        };
    }

    public static final int DATA_COMMON = 7;
}
