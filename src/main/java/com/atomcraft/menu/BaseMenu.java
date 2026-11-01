package com.atomcraft.menu;

import com.atomcraft.chemistry.Composition;
import com.atomcraft.block.entity.MachineBlockEntity;
import com.atomcraft.network.MachineInfoPayload;
import com.atomcraft.network.NetworkContentsPayload;
import com.atomcraft.storage.FilteredItemHandler;
import com.atomcraft.upgrade.UpgradeChipItem;
import com.atomcraft.upgrade.Upgrades;
import net.minecraft.world.inventory.DataSlot;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
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

    /** Largura da coluna de abas laterais e do painel que elas abrem. */
    public static final int TABS_W = 22, PANEL_W = 88;
    /** Posição dos slots de upgrade dentro do painel (relativa ao painel). */
    public static final int UPGRADE_SLOT_X = 18, UPGRADE_SLOT_Y = 24, UPGRADE_SLOT_GAP = 34;

    protected final BlockPos pos;
    protected final Player player;
    protected int machineSlots;

    /** Máquina no servidor (null no cliente). */
    protected final @Nullable MachineBlockEntity machine;
    private int enabledClient = 1;
    private boolean controls;
    private boolean upgradesVisible;
    private int upgradeSlotStart = -1;
    private String ownerName = "";
    private boolean infoSent;

    private int syncTimer = 10;
    private @Nullable Composition lastTotals;
    private long lastCapacity = -1;
    private long lastUsed = -1;

    protected BaseMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos) {
        super(type, id);
        this.pos = pos;
        this.player = inventory.player;
        this.machine = !player.level().isClientSide() && player.level().getBlockEntity(pos) instanceof MachineBlockEntity found
                ? found : null;
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return machine != null ? (machine.isEnabled() ? 1 : 0) : enabledClient;
            }

            @Override
            public void set(int value) {
                enabledClient = value;
            }
        });
    }

    /**
     * Ativa as abas laterais (ligar/desligar, dono e upgrades) e cria os slots de
     * upgrade, que ficam no painel à direita da interface ({@code guiWidth}).
     * Deve ser chamado logo depois dos slots da máquina.
     */
    protected void addMachineControls(int guiWidth) {
        controls = true;
        FilteredItemHandler handler = machine != null ? machine.upgrades()
                : new FilteredItemHandler(Upgrades.SLOTS, Upgrades.MAX_PER_SLOT,
                        (slot, resource) -> resource.getItem() instanceof UpgradeChipItem, () -> {});
        upgradeSlotStart = slots.size();
        int panelX = guiWidth + TABS_W - 1;
        for (int i = 0; i < Upgrades.SLOTS; i++) {
            addSlot(new ResourceHandlerSlot(handler, handler::set, i, panelX + UPGRADE_SLOT_X + i * UPGRADE_SLOT_GAP, 10 + UPGRADE_SLOT_Y) {
                @Override
                public boolean isActive() {
                    return upgradesVisible || !player.level().isClientSide();
                }
            });
        }
        machineSlots += Upgrades.SLOTS;
    }

    public boolean hasMachineControls() {
        return controls;
    }

    public boolean isUpgradeSlot(int index) {
        return upgradeSlotStart >= 0 && index >= upgradeSlotStart && index < upgradeSlotStart + Upgrades.SLOTS;
    }

    public void setUpgradesVisible(boolean visible) {
        this.upgradesVisible = visible;
    }

    public boolean isEnabled() {
        return enabledClient != 0;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
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
            if (!moveToSlots(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!acceptsQuickMove() || !moveToSlots(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /**
     * Como o moveItemStackTo do vanilla, mas respeita {@link Slot#mayPlace} também
     * ao completar pilhas: itens nunca entram em slots de saída.
     */
    protected boolean moveToSlots(ItemStack stack, int start, int end, boolean reverse) {
        boolean moved = false;
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int k = 0; k < end - start && !stack.isEmpty(); k++) {
                Slot slot = slots.get(reverse ? end - 1 - k : start + k);
                if (!slot.mayPlace(stack)) continue;
                ItemStack current = slot.getItem();
                // 1ª passada: só completa pilhas iguais; 2ª: usa slots vazios.
                boolean candidate = pass == 0
                        ? !current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)
                        : current.isEmpty();
                if (!candidate) continue;
                int before = stack.getCount();
                slot.safeInsert(stack);
                if (stack.getCount() != before) moved = true;
            }
        }
        return moved;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!infoSent && machine != null && player instanceof ServerPlayer target) {
            infoSent = true;
            PacketDistributor.sendToPlayer(target, new MachineInfoPayload(containerId, machine.ownerName()));
        }
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
