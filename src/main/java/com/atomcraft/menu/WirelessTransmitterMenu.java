package com.atomcraft.menu;

import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;

/** Transmissor Sem Fio: energia para os equipamentos, alcance e a rede alcançada. */
public class WirelessTransmitterMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 112;

    private final ContainerData data;

    public static WirelessTransmitterMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new WirelessTransmitterMenu(id, inventory, buf.readBlockPos(), new SimpleContainerData(WirelessTransmitterBlockEntity.DATA_COUNT));
    }

    public WirelessTransmitterMenu(int id, Inventory inventory, WirelessTransmitterBlockEntity transmitter) {
        this(id, inventory, transmitter.getBlockPos(), transmitter.data());
    }

    private WirelessTransmitterMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(ModMenus.WIRELESS_TRANSMITTER.get(), id, inventory, pos);
        this.data = data;
        addMachineControlsWithoutUpgrades();
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return ModBlocks.WIRELESS_TRANSMITTER.get();
    }

    public int energy() {
        return combine(data.get(WirelessTransmitterBlockEntity.DATA_ENERGY_LO), data.get(WirelessTransmitterBlockEntity.DATA_ENERGY_HI));
    }

    public int capacity() {
        return combine(data.get(WirelessTransmitterBlockEntity.DATA_CAPACITY_LO), data.get(WirelessTransmitterBlockEntity.DATA_CAPACITY_HI));
    }

    /** Alcance em blocos (-1 = sem limite). */
    public int range() {
        return data.get(WirelessTransmitterBlockEntity.DATA_RANGE) - 1;
    }
}
