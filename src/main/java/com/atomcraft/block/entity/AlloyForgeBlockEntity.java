package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.alloy.AlloyRecipe;
import com.atomcraft.alloy.AlloyRecipes;
import com.atomcraft.menu.AlloyForgeMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.ElementNetwork;
import com.atomcraft.storage.EnergyReceiver;
import com.atomcraft.storage.FilteredItemHandler;
import com.atomcraft.upgrade.UpgradeType;
import com.atomcraft.upgrade.Upgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;


/**
 * Forja de Ligas: escolhida uma receita ({@link AlloyRecipes}), tira os elementos da
 * rede e energia e produz o lingote, repetindo enquanto houver elementos, energia e
 * espaço na saída.
 */
public class AlloyForgeBlockEntity extends MachineBlockEntity implements EnergyReceiver {

    public static final int OUTPUT_SLOTS = 4;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_ENERGY_LO = 2, DATA_ENERGY_HI = 3,
            DATA_CAPACITY_LO = 4, DATA_CAPACITY_HI = 5, DATA_SELECTED_LO = 6, DATA_SELECTED_HI = 7, DATA_COUNT = 8;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(
            Config.REPLICATOR_CAPACITY.get(), Config.REPLICATOR_MAX_INPUT.get(), 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };
    private final FilteredItemHandler output = new FilteredItemHandler(OUTPUT_SLOTS, 64, (slot, resource) -> true, this::setChanged);
    private final ResourceHandler<ItemResource> automation = output.extractOnly();

    private @Nullable Item selected;
    private boolean reserved;
    private int progress;
    private int maxProgress;
    private long energyNeeded;
    private long energyUsed;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int selectedId = selected == null ? 0 : BuiltInRegistries.ITEM.getId(selected) + 1;
            int stored = energy.getAmountAsInt(), capacity = energy.getCapacityAsInt();
            return switch (index) {
                case DATA_PROGRESS -> reserved ? permille(progress, maxProgress) : 0;
                case DATA_STATUS -> status.ordinal();
                case DATA_ENERGY_LO -> stored & 0xFFFF;
                case DATA_ENERGY_HI -> (stored >>> 16) & 0xFFFF;
                case DATA_CAPACITY_LO -> capacity & 0xFFFF;
                case DATA_CAPACITY_HI -> (capacity >>> 16) & 0xFFFF;
                case DATA_SELECTED_LO -> selectedId & 0xFFFF;
                case DATA_SELECTED_HI -> (selectedId >>> 16) & 0xFFFF;
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

    public AlloyForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_FORGE.get(), pos, state);
    }

    @Override
    public SimpleEnergyHandler energy() {
        return energy;
    }

    public FilteredItemHandler output() {
        return output;
    }

    public ResourceHandler<ItemResource> automation() {
        return automation;
    }

    public ContainerData data() {
        return data;
    }

    public @Nullable Item selected() {
        return selected;
    }

    /** Escolhe a receita pelo resultado (ar limpa). */
    public void select(Item item) {
        Item target = item == Items.AIR || AlloyRecipes.get(item) == null ? null : item;
        if (target == selected) return;
        refund();
        selected = target;
        setChanged();
    }

    private void refund() {
        if (reserved && selected != null) {
            AlloyRecipe recipe = AlloyRecipes.get(selected);
            if (recipe != null) network().insert(recipe.elements(), 1);
        }
        reserved = false;
        progress = maxProgress = 0;
        energyNeeded = energyUsed = 0;
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return true;
    }

    @Override
    public void serverTick() {
        if (selected == null) {
            status = MachineStatus.IDLE;
            return;
        }
        AlloyRecipe recipe = AlloyRecipes.get(selected);
        if (recipe == null) {
            // A receita saiu do datapack (/reload).
            reserved = false;
            selected = null;
            status = MachineStatus.IDLE;
            setChanged();
            return;
        }

        if (!reserved) {
            if (!fitsInOutput(selected, recipe.count())) {
                status = MachineStatus.OUTPUT_FULL;
                return;
            }
            ElementNetwork network = network();
            if (!network.hasStorage()) {
                status = MachineStatus.NO_STORAGE;
                return;
            }
            if (!network.extract(recipe.elements(), 1)) {
                status = MachineStatus.NO_ELEMENTS;
                return;
            }
            reserved = true;
            progress = 0;
            maxProgress = 0;
            energyUsed = 0;
        }

        // Recalculado a cada tick: chips instalados no meio da produção já valem.
        int speed = upgradeCount(UpgradeType.SPEED);
        int newMax = Upgrades.ticks(recipe.ticks(), speed);
        progress = rescaleProgress(progress, maxProgress, newMax);
        maxProgress = newMax;
        energyNeeded = Upgrades.energy(recipe.energy(), speed, upgradeCount(UpgradeType.EFFICIENCY));

        if (progress < maxProgress || energyUsed < energyNeeded) {
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
            if (progress < maxProgress) progress++;
            status = MachineStatus.WORKING;
            setChanged();
            if (progress < maxProgress || energyUsed < energyNeeded) return;
        }

        try (Transaction transaction = Transaction.openRoot()) {
            if (output.insert(ItemResource.of(selected), recipe.count(), transaction) == recipe.count()) {
                transaction.commit();
                reserved = false;
                progress = 0;
            } else {
                status = MachineStatus.OUTPUT_FULL;
            }
        }
        setChanged();
    }

    private boolean fitsInOutput(Item item, int count) {
        try (Transaction transaction = Transaction.openRoot()) {
            return output.insert(ItemResource.of(item), count, transaction) == count;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.alloy_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AlloyForgeMenu(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            refund();
            dropContents(level, pos, output);
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
        output.deserialize(input.childOrEmpty("output"));
        selected = input.getString("selected").map(Identifier::tryParse).flatMap(BuiltInRegistries.ITEM::getOptional).orElse(null);
        reserved = input.getBooleanOr("reserved", false);
        progress = input.getIntOr("progress", 0);
        maxProgress = input.getIntOr("max_progress", 0);
        energyNeeded = input.getLongOr("energy_needed", 0L);
        energyUsed = input.getLongOr("energy_used", 0L);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        this.output.serialize(output.child("output"));
        if (selected != null) output.putString("selected", BuiltInRegistries.ITEM.getKey(selected).toString());
        output.putBoolean("reserved", reserved);
        output.putInt("progress", progress);
        output.putInt("max_progress", maxProgress);
        output.putLong("energy_needed", energyNeeded);
        output.putLong("energy_used", energyUsed);
    }
}
