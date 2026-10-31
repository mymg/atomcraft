package com.atomcraft.block.entity;

import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.menu.AtomizerMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.ElementNetwork;
import com.atomcraft.storage.FilteredItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Atomizador: consome os itens do buffer de entrada e guarda os elementos na
 * rede. Aceita itens de hoppers, pipes e esteiras.
 */
public class AtomizerBlockEntity extends MachineBlockEntity {

    public static final int SLOTS = 18;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_COUNT = 2;

    private final FilteredItemHandler buffer = new FilteredItemHandler(SLOTS, 99,
            (slot, resource) -> AtomicValues.get(resource.getItem(), isClientSide()) != null, this::setChanged);
    private final ResourceHandler<ItemResource> automation = buffer.insertOnly();

    private int progress;
    private int maxProgress;
    private int currentSlot = -1;
    private MachineStatus status = MachineStatus.IDLE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> permille(progress, maxProgress);
                case DATA_STATUS -> status.ordinal();
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

    public AtomizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATOMIZER.get(), pos, state);
    }

    public FilteredItemHandler buffer() {
        return buffer;
    }

    public ResourceHandler<ItemResource> automation() {
        return automation;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public void serverTick() {
        if (currentSlot < 0 || buffer.getAmountAsInt(currentSlot) == 0) {
            currentSlot = findWork();
            progress = 0;
            if (currentSlot < 0) {
                maxProgress = 0;
                status = MachineStatus.IDLE;
                return;
            }
        }

        ItemStack stack = buffer.getStack(currentSlot);
        AtomicValue value = AtomicValues.get(stack.getItem(), false);
        Composition result = AtomicValues.atomizeResult(stack, false);
        if (value == null || result == null) {
            currentSlot = -1;
            return;
        }

        ElementNetwork network = network();
        if (!network.hasStorage()) {
            status = MachineStatus.NO_STORAGE;
            return;
        }
        if (!network.canInsert(result, 1)) {
            status = MachineStatus.STORAGE_FULL;
            return;
        }

        status = MachineStatus.WORKING;
        maxProgress = value.atomizeTicks();
        if (++progress >= maxProgress) {
            progress = 0;
            if (network.insert(result, 1)) {
                buffer.shrink(currentSlot, 1);
            }
        }
        setChanged();
    }

    private int findWork() {
        for (int i = 0; i < SLOTS; i++) {
            if (buffer.getAmountAsInt(i) > 0) return i;
        }
        return -1;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.atomizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AtomizerMenu(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) dropContents(level, pos, buffer);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.deserialize(input.childOrEmpty("buffer"));
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        buffer.serialize(output.child("buffer"));
        output.putInt("progress", progress);
    }
}
