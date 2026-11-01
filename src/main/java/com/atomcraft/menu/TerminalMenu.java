package com.atomcraft.menu;

import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

/** Terminal de Elementos: sem slots, só recebe o estoque da rede. */
public class TerminalMenu extends BaseMenu {

    public static final int WIDTH = 244, HEIGHT = 222;

    public static TerminalMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new TerminalMenu(id, inventory, buf.readBlockPos());
    }

    public TerminalMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.TERMINAL.get(), id, inventory, pos);
    }

    @Override
    protected Block block() {
        return ModBlocks.TERMINAL.get();
    }
}
