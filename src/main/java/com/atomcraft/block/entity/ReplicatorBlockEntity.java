package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.menu.ReplicatorMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.research.ResearchData;
import com.atomcraft.storage.ElementNetwork;
import com.atomcraft.storage.EnergyReceiver;
import com.atomcraft.storage.FilteredItemHandler;
import net.minecraft.ChatFormatting;
import com.atomcraft.upgrade.UpgradeType;
import com.atomcraft.upgrade.Upgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Replicador Atômico: cria cópias de itens já pesquisados, consumindo os
 * elementos da rede e energia (FE). Energia e tempo crescem com a massa atômica
 * e a raridade do item. Os itens prontos vão para o buffer de saída, de onde
 * hoppers e pipes podem retirá-los.
 */
public class ReplicatorBlockEntity extends MachineBlockEntity implements EnergyReceiver {

    public static final int SLOTS = 9;
    public static final int MAX_REQUEST = 9999;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_ENERGY_LO = 2, DATA_ENERGY_HI = 3,
            DATA_CAPACITY_LO = 4, DATA_CAPACITY_HI = 5, DATA_TARGET_LO = 6, DATA_TARGET_HI = 7,
            DATA_REMAINING = 8, DATA_COUNT = 9;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(
            Config.REPLICATOR_CAPACITY.get(), Config.REPLICATOR_MAX_INPUT.get(), 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };
    private final FilteredItemHandler output = new FilteredItemHandler(SLOTS, 99, (slot, resource) -> true, this::setChanged);
    private final ResourceHandler<ItemResource> automation = output.extractOnly();

    private @Nullable Item target;
    private int remaining;
    private @Nullable UUID requester;
    private boolean reserved;
    private int progress;
    private int maxProgress;
    private long energyNeeded;
    private long energyUsed;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int targetId = target == null ? 0 : BuiltInRegistries.ITEM.getId(target) + 1;
            int stored = energy.getAmountAsInt();
            int capacity = energy.getCapacityAsInt();
            return switch (index) {
                case DATA_PROGRESS -> reserved ? permille(progress, maxProgress) : 0;
                case DATA_STATUS -> status.ordinal();
                case DATA_ENERGY_LO -> stored & 0xFFFF;
                case DATA_ENERGY_HI -> (stored >>> 16) & 0xFFFF;
                case DATA_CAPACITY_LO -> capacity & 0xFFFF;
                case DATA_CAPACITY_HI -> (capacity >>> 16) & 0xFFFF;
                case DATA_TARGET_LO -> targetId & 0xFFFF;
                case DATA_TARGET_HI -> (targetId >>> 16) & 0xFFFF;
                case DATA_REMAINING -> remaining;
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

    public ReplicatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REPLICATOR.get(), pos, state);
    }

    public FilteredItemHandler output() {
        return output;
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

    /** Inicia (ou substitui) um pedido de replicação feito por um jogador na interface. */
    public void startJob(ServerPlayer player, Item item, int amount) {
        if (level == null || level.getServer() == null || amount <= 0) return;
        if (!ResearchData.get(level.getServer()).isResearched(player.getUUID(), item)) {
            player.sendSystemMessage(Component.translatable("message.atomcraft.not_researched",
                    new ItemStack(item).getHoverName()).withStyle(ChatFormatting.RED));
            return;
        }
        startJob(player.getUUID(), item, amount);
    }

    public void startJob(UUID player, Item item, int amount) {
        if (amount <= 0 || AtomicValues.get(item, false) == null) return;
        if (target == item && player.equals(requester)) {
            remaining = Math.min(MAX_REQUEST, remaining + amount);
        } else {
            cancelJob();
            target = item;
            remaining = Math.min(MAX_REQUEST, amount);
            requester = player;
        }
        setChanged();
    }

    /** Cancela o pedido. Elementos já reservados para a unidade em produção voltam para a rede. */
    public void cancelJob() {
        if (reserved && target != null) {
            AtomicValue value = AtomicValues.get(target, false);
            if (value != null) network().insert(value.composition(), 1);
        }
        target = null;
        remaining = 0;
        requester = null;
        reserved = false;
        progress = maxProgress = 0;
        energyNeeded = energyUsed = 0;
        status = MachineStatus.IDLE;
        setChanged();
    }

    @Override
    public void serverTick() {
        if (level == null || level.getServer() == null) return;
        if (target == null) {
            status = MachineStatus.IDLE;
            return;
        }
        AtomicValue value = AtomicValues.get(target, false);
        if (value == null) {
            cancelJob();
            return;
        }

        if (!reserved) {
            if (requester == null || !ResearchData.get(level.getServer()).isResearched(requester, target)) {
                status = MachineStatus.NOT_RESEARCHED;
                return;
            }
            if (!fitsInOutput(target)) {
                status = MachineStatus.OUTPUT_FULL;
                return;
            }
            ElementNetwork network = network();
            if (!network.hasStorage()) {
                status = MachineStatus.NO_STORAGE;
                return;
            }
            if (!network.extract(value.composition(), 1)) {
                status = MachineStatus.NO_ELEMENTS;
                return;
            }
            reserved = true;
            progress = 0;
            int speed = upgradeCount(UpgradeType.SPEED);
            maxProgress = Upgrades.ticks(value.replicateTicks(), speed);
            energyNeeded = Upgrades.energy(value.energyCost(), speed, upgradeCount(UpgradeType.EFFICIENCY));
            energyUsed = 0;
        }

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

        // Unidade pronta: tenta colocar no buffer de saída.
        if (!insertOutput(target)) {
            status = MachineStatus.OUTPUT_FULL;
            return;
        }
        reserved = false;
        progress = 0;
        remaining--;
        if (remaining <= 0) {
            target = null;
            requester = null;
            remaining = 0;
            status = MachineStatus.IDLE;
        }
        setChanged();
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return true;
    }

    private boolean fitsInOutput(Item item) {
        try (Transaction transaction = Transaction.openRoot()) {
            return output.insert(ItemResource.of(item), 1, transaction) == 1;
        }
    }

    private boolean insertOutput(Item item) {
        try (Transaction transaction = Transaction.openRoot()) {
            if (output.insert(ItemResource.of(item), 1, transaction) == 1) {
                transaction.commit();
                return true;
            }
            return false;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.replicator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReplicatorMenu(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            cancelJob();
            dropContents(level, pos, output);
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
        output.deserialize(input.childOrEmpty("output"));
        target = input.getString("target")
                .map(Identifier::tryParse)
                .flatMap(BuiltInRegistries.ITEM::getOptional)
                .orElse(null);
        remaining = input.getIntOr("remaining", 0);
        requester = input.read("requester", UUIDUtil.CODEC).orElse(null);
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
        if (target != null) output.putString("target", BuiltInRegistries.ITEM.getKey(target).toString());
        output.putInt("remaining", remaining);
        if (requester != null) output.store("requester", UUIDUtil.CODEC, requester);
        output.putBoolean("reserved", reserved);
        output.putInt("progress", progress);
        output.putInt("max_progress", maxProgress);
        output.putLong("energy_needed", energyNeeded);
        output.putLong("energy_used", energyUsed);
    }
}
