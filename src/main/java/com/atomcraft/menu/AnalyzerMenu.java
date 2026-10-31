package com.atomcraft.menu;

import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.chemistry.AtomicValues;
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
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

public class AnalyzerMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 176;

    private final ContainerData data;

    public static AnalyzerMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        FilteredItemHandler handler = new FilteredItemHandler(AnalyzerBlockEntity.SLOTS, 99,
                (slot, resource) -> AtomicValues.get(resource.getItem(), true) != null, () -> {});
        return new AnalyzerMenu(id, inventory, buf.readBlockPos(), handler, new SimpleContainerData(AnalyzerBlockEntity.DATA_COUNT));
    }

    public AnalyzerMenu(int id, Inventory inventory, AnalyzerBlockEntity machine) {
        this(id, inventory, machine.getBlockPos(), machine.buffer(), machine.data());
    }

    private AnalyzerMenu(int id, Inventory inventory, BlockPos pos, FilteredItemHandler handler, ContainerData data) {
        super(ModMenus.ANALYZER.get(), id, inventory, pos);
        this.data = data;
        for (int col = 0; col < 9; col++) {
            addSlot(new ResourceHandlerSlot(handler, handler::set, col, 8 + col * 18, 20));
        }
        machineSlots = AnalyzerBlockEntity.SLOTS;
        addPlayerInventory(inventory, 8, HEIGHT - 82);
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return ModBlocks.ANALYZER.get();
    }

    @Override
    protected boolean syncsNetwork() {
        return false;
    }

    public float progress() {
        return data.get(AnalyzerBlockEntity.DATA_PROGRESS) / 1000f;
    }

    public MachineStatus status() {
        return MachineStatus.byId(data.get(AnalyzerBlockEntity.DATA_STATUS));
    }

    public @Nullable Item currentItem() {
        int id = combine(data.get(AnalyzerBlockEntity.DATA_ITEM_LO), data.get(AnalyzerBlockEntity.DATA_ITEM_HI)) - 1;
        return id < 0 ? null : BuiltInRegistries.ITEM.byId(id);
    }

    public int researched() {
        return unsigned(data.get(AnalyzerBlockEntity.DATA_RESEARCHED));
    }

    public int required() {
        return unsigned(data.get(AnalyzerBlockEntity.DATA_REQUIRED));
    }
}
