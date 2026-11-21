package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.equipment.Battery;
import com.atomcraft.equipment.ExoArmorItem;
import com.atomcraft.menu.ModuleBenchMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.EnergyReceiver;
import com.atomcraft.tools.AtomicDisintegratorItem;
import com.atomcraft.upgrade.UpgradeType;
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
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Bancada de Módulos: guarda energia (da rede ou de cabos de energia) e recarrega o equipamento na
 * bancada e a armadura vestida de quem está com ela aberta.
 */
public class ModuleBenchBlockEntity extends MachineBlockEntity implements EnergyReceiver {

    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAPACITY_LO = 2, DATA_CAPACITY_HI = 3, DATA_COUNT = 4;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(Config.BENCH_CAPACITY.get(), Config.BENCH_CHARGE.get() * 2, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int stored = energy.getAmountAsInt(), capacity = energy.getCapacityAsInt();
            return switch (index) {
                case DATA_ENERGY_LO -> stored & 0xFFFF;
                case DATA_ENERGY_HI -> (stored >>> 16) & 0xFFFF;
                case DATA_CAPACITY_LO -> capacity & 0xFFFF;
                case DATA_CAPACITY_HI -> (capacity >>> 16) & 0xFFFF;
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

    public ModuleBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MODULE_BENCH.get(), pos, state);
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
        status = energy.getAmountAsInt() > 0 ? MachineStatus.IDLE : MachineStatus.NO_ENERGY;
    }

    /**
     * Recarrega o item com a energia da bancada (exoesqueleto e desintegrador), até {@code max} FE.
     * Devolve quanto passou.
     */
    public int charge(ItemStack stack, int max) {
        int capacity, current;
        if (stack.getItem() instanceof ExoArmorItem) {
            capacity = ExoArmorItem.CAPACITY;
            current = Battery.energy(stack);
        } else if (stack.getItem() instanceof AtomicDisintegratorItem) {
            capacity = AtomicDisintegratorItem.CAPACITY;
            current = AtomicDisintegratorItem.energy(stack);
        } else {
            return 0;
        }
        int amount = Math.min(Math.min(capacity - current, max), energy.getAmountAsInt());
        if (amount <= 0 || !isEnabled()) return 0;
        energy.set(energy.getAmountAsInt() - amount);
        if (stack.getItem() instanceof ExoArmorItem) Battery.set(stack, current + amount, capacity);
        else AtomicDisintegratorItem.setEnergy(stack, current + amount);
        return amount;
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.module_bench");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ModuleBenchMenu(id, inventory, worldPosition);
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
}
