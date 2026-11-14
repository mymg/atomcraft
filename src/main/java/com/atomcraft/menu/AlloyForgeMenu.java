package com.atomcraft.menu;

import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.block.entity.AlloyForgeBlockEntity;
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

import java.util.List;

public class AlloyForgeMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 222;
    public static final int OUTPUT_X = 80, OUTPUT_Y = 86;

    private final ContainerData data;

    public static AlloyForgeMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new AlloyForgeMenu(id, inventory, buf.readBlockPos(),
                new FilteredItemHandler(AlloyForgeBlockEntity.OUTPUT_SLOTS), new SimpleContainerData(AlloyForgeBlockEntity.DATA_COUNT));
    }

    public AlloyForgeMenu(int id, Inventory inventory, AlloyForgeBlockEntity machine) {
        this(id, inventory, machine.getBlockPos(), machine.output(), machine.data());
    }

    private AlloyForgeMenu(int id, Inventory inventory, BlockPos pos, FilteredItemHandler output,
                            ContainerData data) {
        super(ModMenus.ALLOY_FORGE.get(), id, inventory, pos);
        this.data = data;
        for (int col = 0; col < AlloyForgeBlockEntity.OUTPUT_SLOTS; col++) {
            addSlot(new ResourceHandlerSlot(output, output::set, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        machineSlots = AlloyForgeBlockEntity.OUTPUT_SLOTS;
        addMachineControls(WIDTH);
        addPlayerInventory(inventory, 8, HEIGHT - 82);
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return ModBlocks.ALLOY_FORGE.get();
    }

    /** Resultados das receitas da forja, na ordem da lista. */
    public List<Item> available() {
        return com.atomcraft.alloy.AlloyRecipes.all().stream().map(com.atomcraft.alloy.AlloyRecipe::result).toList();
    }

    public @Nullable Item selected() {
        int id = combine(data.get(AlloyForgeBlockEntity.DATA_SELECTED_LO), data.get(AlloyForgeBlockEntity.DATA_SELECTED_HI));
        return id <= 0 ? null : BuiltInRegistries.ITEM.byId(id - 1);
    }

    public float progress() {
        return data.get(AlloyForgeBlockEntity.DATA_PROGRESS) / 1000f;
    }

    public int energy() {
        return combine(data.get(AlloyForgeBlockEntity.DATA_ENERGY_LO), data.get(AlloyForgeBlockEntity.DATA_ENERGY_HI));
    }

    public int capacity() {
        return combine(data.get(AlloyForgeBlockEntity.DATA_CAPACITY_LO), data.get(AlloyForgeBlockEntity.DATA_CAPACITY_HI));
    }

    public MachineStatus status() {
        return MachineStatus.byId(data.get(AlloyForgeBlockEntity.DATA_STATUS));
    }
}
