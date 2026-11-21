package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.menu.WirelessTransmitterMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.EnergyReceiver;
import com.atomcraft.upgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Transmissor Sem Fio: nó da rede com um estoque de energia (cheio pelos geradores e células da
 * rede) que os equipamentos atômicos usam à distância, junto com os elementos dos Drives. Alimenta os
 * jogadores cujos Cartões de Rede estão nos seus slots.
 */
public class WirelessTransmitterBlockEntity extends MachineBlockEntity implements EnergyReceiver {

    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAPACITY_LO = 2, DATA_CAPACITY_HI = 3,
            DATA_COUNT = 4;
    public static final int CARD_SLOTS = 4;

    /** Cartões de Rede dos jogadores que este transmissor alimenta. */
    private final com.atomcraft.storage.FilteredItemHandler cards = new com.atomcraft.storage.FilteredItemHandler(CARD_SLOTS, 1,
            (slot, resource) -> resource.getItem() instanceof com.atomcraft.wireless.NetworkCardItem, this::setChanged);

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(Config.TRANSMITTER_CAPACITY.get(), 100_000, 0) {
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

    public WirelessTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_TRANSMITTER.get(), pos, state);
    }

    @Override
    public SimpleEnergyHandler energy() {
        return energy;
    }

    public ContainerData data() {
        return data;
    }

    public com.atomcraft.storage.FilteredItemHandler cards() {
        return cards;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        com.atomcraft.wireless.Wireless.register(this);
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        com.atomcraft.wireless.Wireless.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        com.atomcraft.wireless.Wireless.unregister(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        com.atomcraft.wireless.Wireless.unregister(this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) dropContents(level, pos, cards);
    }

    @Override
    public void serverTick() {
        com.atomcraft.wireless.Wireless.register(this);
        status = network().hasStorage() ? MachineStatus.WORKING : MachineStatus.NO_STORAGE;
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.wireless_transmitter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WirelessTransmitterMenu(id, inventory, this);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
        cards.deserialize(input.childOrEmpty("cards"));
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        cards.serialize(output.child("cards"));
    }
}
