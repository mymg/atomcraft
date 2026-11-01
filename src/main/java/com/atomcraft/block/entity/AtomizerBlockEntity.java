package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.menu.AtomizerMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.ElementNetwork;
import com.atomcraft.storage.EnergyReceiver;
import com.atomcraft.storage.FilteredItemHandler;
import com.atomcraft.upgrade.UpgradeType;
import com.atomcraft.upgrade.Upgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Atomizador: consome os itens do buffer de entrada e guarda os elementos na
 * rede. Aceita itens de hoppers, pipes e esteiras.
 */
public class AtomizerBlockEntity extends MachineBlockEntity implements EnergyReceiver {

    public static final int SLOTS = 21;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_ENERGY_LO = 2, DATA_ENERGY_HI = 3,
            DATA_CAPACITY_LO = 4, DATA_CAPACITY_HI = 5, DATA_COUNT = 6;

    private final FilteredItemHandler buffer = new FilteredItemHandler(SLOTS, 99,
            (slot, resource) -> AtomicValues.get(resource.getItem(), isClientSide()) != null, this::setChanged);
    private final ResourceHandler<ItemResource> automation = buffer.insertOnly();
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(
            Config.ATOMIZER_CAPACITY.get(), Config.ATOMIZER_MAX_INPUT.get(), 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private int progress;
    private int maxProgress;
    private int currentSlot = -1;
    private long energyNeeded;
    private long energyUsed;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> permille(progress, maxProgress);
                case DATA_STATUS -> status.ordinal();
                case DATA_ENERGY_LO -> energy.getAmountAsInt() & 0xFFFF;
                case DATA_ENERGY_HI -> (energy.getAmountAsInt() >>> 16) & 0xFFFF;
                case DATA_CAPACITY_LO -> energy.getCapacityAsInt() & 0xFFFF;
                case DATA_CAPACITY_HI -> (energy.getCapacityAsInt() >>> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AtomizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATOMIZER.get(), pos, state);
    }

    public FilteredItemHandler buffer() {
        return buffer;
    }

    public ResourceHandler<ItemResource> automation() {
        return automation;
    }

    @Override
    public SimpleEnergyHandler energy() {
        return energy;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public void serverTick() {
        if (currentSlot < 0 || buffer.getAmountAsInt(currentSlot) == 0) {
            currentSlot = findWork();
            progress = 0;
            if (currentSlot < 0) {
                maxProgress = 0;
                status = MachineStatus.IDLE;
                return;
            }
        }

        ItemStack stack = buffer.getStack(currentSlot);
        AtomicValue value = AtomicValues.get(stack.getItem(), false);
        Composition result = AtomicValues.atomizeResult(stack, false);
        if (value == null || result == null) {
            currentSlot = -1;
            return;
        }

        ElementNetwork network = network();
        if (!network.hasStorage()) {
            status = MachineStatus.NO_STORAGE;
            return;
        }
        if (!network.canInsert(result, 1)) {
            status = MachineStatus.STORAGE_FULL;
            return;
        }

        int speed = upgradeCount(UpgradeType.SPEED);
        maxProgress = Upgrades.ticks(value.atomizeTicks(), speed);
        if (progress == 0) {
            energyNeeded = Upgrades.energy((long) Math.ceil(result.weightedMass() * Config.ATOMIZE_FE_PER_MASS.get()),
                    speed, upgradeCount(UpgradeType.EFFICIENCY));
            energyUsed = 0;
        }
        long perTick = (energyNeeded + maxProgress - 1) / Math.max(1, maxProgress);
        long want = Math.min(perTick, energyNeeded - energyUsed);
        if (want > 0) {
            if (energy.getAmountAsLong() < want) {
                status = MachineStatus.NO_ENERGY;
                return;
            }
            energy.set((int) (energy.getAmountAsLong() - want));
            energyUsed += want;
        }

        status = MachineStatus.WORKING;
        if (++progress >= maxProgress && energyUsed >= energyNeeded) {
            progress = 0;
            if (network.insert(result, 1)) {
                buffer.shrink(currentSlot, 1);
            }
        }
        setChanged();
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return true;
    }

    private int findWork() {
        for (int i = 0; i < SLOTS; i++) {
            if (buffer.getAmountAsInt(i) > 0) return i;
        }
        return -1;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.atomizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AtomizerMenu(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) dropContents(level, pos, buffer);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.deserialize(input.childOrEmpty("buffer"));
        energy.deserialize(input.childOrEmpty("energy"));
        progress = input.getIntOr("progress", 0);
        energyNeeded = input.getLongOr("energy_needed", 0L);
        energyUsed = input.getLongOr("energy_used", 0L);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        buffer.serialize(output.child("buffer"));
        energy.serialize(output.child("energy"));
        output.putInt("progress", progress);
        output.putLong("energy_needed", energyNeeded);
        output.putLong("energy_used", energyUsed);
    }
}
