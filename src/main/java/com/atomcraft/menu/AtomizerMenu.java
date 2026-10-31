package com.atomcraft.menu;

import com.atomcraft.block.entity.AtomizerBlockEntity;
import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModMenus;
import com.atomcraft.storage.FilteredItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class AtomizerMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 186;

    private final ContainerData data;

    public static AtomizerMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        FilteredItemHandler handler = new FilteredItemHandler(AtomizerBlockEntity.SLOTS, 99,
                (slot, resource) -> AtomicValues.get(resource.getItem(), true) != null, () -> {});
        return new AtomizerMenu(id, inventory, buf.readBlockPos(), handler, new SimpleContainerData(AtomizerBlockEntity.DATA_COUNT));
    }

    public AtomizerMenu(int id, Inventory inventory, AtomizerBlockEntity machine) {
        this(id, inventory, machine.getBlockPos(), machine.buffer(), machine.data());
    }

    private AtomizerMenu(int id, Inventory inventory, BlockPos pos, FilteredItemHandler handler, ContainerData data) {
        super(ModMenus.ATOMIZER.get(), id, inventory, pos);
        this.data = data;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new ResourceHandlerSlot(handler, handler::set, col + row * 9, 8 + col * 18, 20 + row * 18));
            }
        }
        machineSlots = AtomizerBlockEntity.SLOTS;
        addPlayerInventory(inventory, 8, HEIGHT - 82);
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return ModBlocks.ATOMIZER.get();
    }

    public float progress() {
        return data.get(AtomizerBlockEntity.DATA_PROGRESS) / 1000f;
    }

    public MachineStatus status() {
        return MachineStatus.byId(data.get(AtomizerBlockEntity.DATA_STATUS));
    }
}
