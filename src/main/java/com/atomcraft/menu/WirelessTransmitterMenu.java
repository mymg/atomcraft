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

    public static final int WIDTH = 176, HEIGHT = 196, CARD_X = 36, CARD_Y = 40, CARD_GAP = 22;

    private final ContainerData data;

    public static WirelessTransmitterMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new WirelessTransmitterMenu(id, inventory, buf.readBlockPos(), new SimpleContainerData(WirelessTransmitterBlockEntity.DATA_COUNT),
                new com.atomcraft.storage.FilteredItemHandler(WirelessTransmitterBlockEntity.CARD_SLOTS, 1,
                        (slot, resource) -> resource.getItem() instanceof com.atomcraft.wireless.NetworkCardItem, () -> {}));
    }

    public WirelessTransmitterMenu(int id, Inventory inventory, WirelessTransmitterBlockEntity transmitter) {
        this(id, inventory, transmitter.getBlockPos(), transmitter.data(), transmitter.cards());
    }

    private WirelessTransmitterMenu(int id, Inventory inventory, BlockPos pos, ContainerData data, com.atomcraft.storage.FilteredItemHandler cards) {
        super(ModMenus.WIRELESS_TRANSMITTER.get(), id, inventory, pos);
        this.data = data;
        for (int i = 0; i < WirelessTransmitterBlockEntity.CARD_SLOTS; i++) {
            addSlot(new net.neoforged.neoforge.transfer.item.ResourceHandlerSlot(cards, cards::set, i, CARD_X + i * CARD_GAP, CARD_Y));
        }
        machineSlots = WirelessTransmitterBlockEntity.CARD_SLOTS;
        addPlayerInventory(inventory, 8, HEIGHT - 82);
        addMachineControlsWithoutUpgrades();
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return com.atomcraft.tools.ModTools.WIRELESS_TRANSMITTER.get();
    }

    public int energy() {
        return combine(data.get(WirelessTransmitterBlockEntity.DATA_ENERGY_LO), data.get(WirelessTransmitterBlockEntity.DATA_ENERGY_HI));
    }

    public int capacity() {
        return combine(data.get(WirelessTransmitterBlockEntity.DATA_CAPACITY_LO), data.get(WirelessTransmitterBlockEntity.DATA_CAPACITY_HI));
    }


}
