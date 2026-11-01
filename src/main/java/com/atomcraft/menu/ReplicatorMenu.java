package com.atomcraft.menu;

import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.block.entity.ReplicatorBlockEntity;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModMenus;
import com.atomcraft.storage.FilteredItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

public class ReplicatorMenu extends BaseMenu {

    public static final int WIDTH = 256, HEIGHT = 274;
    public static final int OUTPUT_X = 12, OUTPUT_Y = 156;
    public static final int INVENTORY_X = 48;

    private final ContainerData data;

    public static ReplicatorMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new ReplicatorMenu(id, inventory, buf.readBlockPos(), new FilteredItemHandler(ReplicatorBlockEntity.SLOTS),
                new SimpleContainerData(ReplicatorBlockEntity.DATA_COUNT));
    }

    public ReplicatorMenu(int id, Inventory inventory, ReplicatorBlockEntity machine) {
        this(id, inventory, machine.getBlockPos(), machine.output(), machine.data());
    }

    private ReplicatorMenu(int id, Inventory inventory, BlockPos pos, FilteredItemHandler handler, ContainerData data) {
        super(ModMenus.REPLICATOR.get(), id, inventory, pos);
        this.data = data;
        for (int col = 0; col < ReplicatorBlockEntity.SLOTS; col++) {
            addSlot(new ResourceHandlerSlot(handler, handler::set, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        machineSlots = ReplicatorBlockEntity.SLOTS;
        addPlayerInventory(inventory, INVENTORY_X, HEIGHT - 82);
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return ModBlocks.REPLICATOR.get();
    }

    @Override
    protected boolean acceptsQuickMove() {
        return false;
    }

    public float progress() {
        return data.get(ReplicatorBlockEntity.DATA_PROGRESS) / 1000f;
    }

    public MachineStatus status() {
        return MachineStatus.byId(data.get(ReplicatorBlockEntity.DATA_STATUS));
    }

    public int energy() {
        return combine(data.get(ReplicatorBlockEntity.DATA_ENERGY_LO), data.get(ReplicatorBlockEntity.DATA_ENERGY_HI));
    }

    public int capacity() {
        return combine(data.get(ReplicatorBlockEntity.DATA_CAPACITY_LO), data.get(ReplicatorBlockEntity.DATA_CAPACITY_HI));
    }

    public @Nullable Item target() {
        int id = combine(data.get(ReplicatorBlockEntity.DATA_TARGET_LO), data.get(ReplicatorBlockEntity.DATA_TARGET_HI)) - 1;
        return id < 0 ? null : BuiltInRegistries.ITEM.byId(id);
    }

    public int remaining() {
        return unsigned(data.get(ReplicatorBlockEntity.DATA_REMAINING));
    }
}
