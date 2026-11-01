package com.atomcraft.menu;

import com.atomcraft.block.entity.DriveBlockEntity;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModMenus;
import com.atomcraft.storage.ElementDiskItem;
import com.atomcraft.storage.FilteredItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class DriveMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 204;
    public static final int GRID_X = 12, GRID_Y = 26;

    public static DriveMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        FilteredItemHandler handler = new FilteredItemHandler(DriveBlockEntity.SLOTS, 1,
                (slot, resource) -> resource.getItem() instanceof ElementDiskItem, () -> {});
        return new DriveMenu(id, inventory, buf.readBlockPos(), handler);
    }

    public DriveMenu(int id, Inventory inventory, DriveBlockEntity drive) {
        this(id, inventory, drive.getBlockPos(), drive.disks());
    }

    private DriveMenu(int id, Inventory inventory, BlockPos pos, FilteredItemHandler handler) {
        super(ModMenus.DRIVE.get(), id, inventory, pos);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new ResourceHandlerSlot(handler, handler::set, col + row * 4, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        machineSlots = DriveBlockEntity.SLOTS;
        addPlayerInventory(inventory, 8, HEIGHT - 82);
    }

    @Override
    protected Block block() {
        return ModBlocks.DRIVE.get();
    }
}
