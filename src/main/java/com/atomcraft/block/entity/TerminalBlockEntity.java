package com.atomcraft.block.entity;

import com.atomcraft.menu.TerminalMenu;
import com.atomcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Terminal de Elementos: mostra todos os elementos guardados na rede. */
public class TerminalBlockEntity extends BlockEntity implements MenuProvider {

    public TerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERMINAL.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TerminalMenu(id, inventory, worldPosition);
    }
}
