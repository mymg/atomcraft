package com.atomcraft.menu;

import com.atomcraft.chemistry.Composition;
import com.atomcraft.network.NetworkContentsPayload;
import com.atomcraft.storage.ElementNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Base dos menus: inventário do jogador, shift-clique e envio periódico do
 * conteúdo da rede de elementos para a interface.
 */
public abstract class BaseMenu extends AbstractContainerMenu {

    protected final BlockPos pos;
    protected final Player player;
    protected int machineSlots;

    private int syncTimer = 10;
    private @Nullable Composition lastTotals;
    private long lastCapacity = -1;
    private long lastUsed = -1;

    protected BaseMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos) {
        super(type, id);
        this.pos = pos;
        this.player = inventory.player;
    }

    public BlockPos pos() {
        return pos;
    }

    /** Quantidade de slots da máquina (os demais são do inventário do jogador). */
    public int machineSlotCount() {
        return machineSlots;
    }

    protected abstract Block block();

    /** Se a interface mostra o estoque da rede. */
    protected boolean syncsNetwork() {
        return true;
    }

    /** Se itens do inventário podem ir para os slots da máquina com shift-clique. */
    protected boolean acceptsQuickMove() {
        return true;
    }

    protected void addPlayerInventory(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, x + col * 18, y + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(block())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!acceptsQuickMove() || !moveItemStackTo(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!syncsNetwork() || !(player instanceof ServerPlayer serverPlayer)) return;
        if (++syncTimer < 10) return;
        syncTimer = 0;
        ElementNetwork network = ElementNetwork.scan(serverPlayer.level(), pos);
        Composition totals = network.totals();
        long capacity = network.capacity();
        long used = network.used();
        if (!totals.equals(lastTotals) || capacity != lastCapacity || used != lastUsed) {
            lastTotals = totals;
            lastCapacity = capacity;
            lastUsed = used;
            PacketDistributor.sendToPlayer(serverPlayer, new NetworkContentsPayload(totals, capacity, used));
        }
    }

    protected static int unsigned(int value) {
        return value & 0xFFFF;
    }

    protected static int combine(int lo, int hi) {
        return (unsigned(hi) << 16) | unsigned(lo);
    }
}
