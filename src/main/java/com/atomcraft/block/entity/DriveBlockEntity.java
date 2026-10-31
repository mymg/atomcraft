package com.atomcraft.block.entity;

import com.atomcraft.chemistry.Composition;
import com.atomcraft.menu.DriveMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.registry.ModComponents;
import com.atomcraft.storage.ElementDiskItem;
import com.atomcraft.storage.FilteredItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Drive: guarda até 8 discos de elementos e os disponibiliza para a rede. */
public class DriveBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOTS = 8;

    private final FilteredItemHandler disks = new FilteredItemHandler(SLOTS, 1,
            (slot, resource) -> resource.getItem() instanceof ElementDiskItem, this::setChanged);

    public DriveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRIVE.get(), pos, state);
    }

    public FilteredItemHandler disks() {
        return disks;
    }

    public ItemStack getDisk(int slot) {
        return disks.getStack(slot);
    }

    public boolean hasDisks() {
        for (int i = 0; i < SLOTS; i++) {
            if (disks.getResource(i).getItem() instanceof ElementDiskItem) return true;
        }
        return false;
    }

    public void setDiskContents(int slot, Composition contents) {
        ItemStack stack = getDisk(slot).copy();
        if (stack.isEmpty()) return;
        stack.set(ModComponents.DISK_CONTENTS.get(), contents);
        disks.set(slot, ItemResource.of(stack), 1);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.drive");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DriveMenu(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) MachineBlockEntity.dropContents(level, pos, disks);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        disks.deserialize(input.childOrEmpty("disks"));
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        disks.serialize(output.child("disks"));
    }
}
