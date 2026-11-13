package com.atomcraft.menu;

import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.block.entity.SynthesizerBlockEntity;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModMenus;
import com.atomcraft.storage.FilteredItemHandler;
import com.atomcraft.synthesis.SchematicItem;
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

public class SynthesizerMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 222;
    public static final int LIBRARY_X = 26, LIBRARY_Y = 25, OUTPUT_X = 80, OUTPUT_Y = 86;

    private final ContainerData data;
    private final FilteredItemHandler library;

    public static SynthesizerMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        FilteredItemHandler library = new FilteredItemHandler(SynthesizerBlockEntity.LIBRARY_SLOTS, 1,
                (slot, resource) -> resource.getItem() instanceof SchematicItem, () -> {});
        return new SynthesizerMenu(id, inventory, buf.readBlockPos(), library,
                new FilteredItemHandler(SynthesizerBlockEntity.OUTPUT_SLOTS), new SimpleContainerData(SynthesizerBlockEntity.DATA_COUNT));
    }

    public SynthesizerMenu(int id, Inventory inventory, SynthesizerBlockEntity machine) {
        this(id, inventory, machine.getBlockPos(), machine.library(), machine.output(), machine.data());
    }

    private SynthesizerMenu(int id, Inventory inventory, BlockPos pos, FilteredItemHandler library, FilteredItemHandler output,
                            ContainerData data) {
        super(ModMenus.SYNTHESIZER.get(), id, inventory, pos);
        this.data = data;
        this.library = library;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new ResourceHandlerSlot(library, library::set, col + row * 2, LIBRARY_X + col * 18, LIBRARY_Y + row * 18));
            }
        }
        for (int col = 0; col < SynthesizerBlockEntity.OUTPUT_SLOTS; col++) {
            addSlot(new ResourceHandlerSlot(output, output::set, col, OUTPUT_X + col * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        machineSlots = SynthesizerBlockEntity.LIBRARY_SLOTS + SynthesizerBlockEntity.OUTPUT_SLOTS;
        addMachineControls(WIDTH);
        addPlayerInventory(inventory, 8, HEIGHT - 82);
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return ModBlocks.SYNTHESIZER.get();
    }

    /** Itens que as schematics da biblioteca permitem fabricar. */
    public List<Item> available() {
        return SynthesizerBlockEntity.available(library);
    }

    public @Nullable Item selected() {
        int id = combine(data.get(SynthesizerBlockEntity.DATA_SELECTED_LO), data.get(SynthesizerBlockEntity.DATA_SELECTED_HI));
        return id <= 0 ? null : BuiltInRegistries.ITEM.byId(id - 1);
    }

    public float progress() {
        return data.get(SynthesizerBlockEntity.DATA_PROGRESS) / 1000f;
    }

    public int energy() {
        return combine(data.get(SynthesizerBlockEntity.DATA_ENERGY_LO), data.get(SynthesizerBlockEntity.DATA_ENERGY_HI));
    }

    public int capacity() {
        return combine(data.get(SynthesizerBlockEntity.DATA_CAPACITY_LO), data.get(SynthesizerBlockEntity.DATA_CAPACITY_HI));
    }

    public MachineStatus status() {
        return MachineStatus.byId(data.get(SynthesizerBlockEntity.DATA_STATUS));
    }
}
