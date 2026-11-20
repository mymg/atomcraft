package com.atomcraft.menu;

import com.atomcraft.equipment.EquipmentKind;
import com.atomcraft.equipment.EquipmentModuleItem;
import com.atomcraft.equipment.ModularItem;
import com.atomcraft.equipment.ModEquipment;
import com.atomcraft.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Bancada de Módulos: o equipamento vai no slot da esquerda e os módulos instalados aparecem nos slots
 * da direita. Tirar ou pôr um módulo grava na hora no equipamento (componente {@code installed_modules});
 * tirar o equipamento leva os módulos junto.
 */
public class ModuleBenchMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 200;
    public static final int EQUIPMENT_X = 30, EQUIPMENT_Y = 40, MODULE_X = 86, MODULE_Y = 40, MODULE_GAP = 24;

    private final SimpleContainer container = new SimpleContainer(1 + EquipmentKind.MAX_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            containerChanged(this);
        }
    };
    private ItemStack loadedFor = ItemStack.EMPTY;
    private boolean updating;

    public static ModuleBenchMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new ModuleBenchMenu(id, inventory, buf.readBlockPos());
    }

    public ModuleBenchMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.MODULE_BENCH.get(), id, inventory, pos);
        addSlot(new Slot(container, 0, EQUIPMENT_X, EQUIPMENT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ModularItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < EquipmentKind.MAX_SLOTS; i++) {
            int index = i;
            addSlot(new Slot(container, 1 + i, MODULE_X + i * MODULE_GAP, MODULE_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return accepts(index, stack);
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return stack.getItem() instanceof EquipmentModuleItem module ? module.module().maxLevel() : 1;
                }

                @Override
                public boolean isActive() {
                    EquipmentKind kind = kind();
                    return kind != null && index < kind.slots();
                }
            });
        }
        machineSlots = 1 + EquipmentKind.MAX_SLOTS;
        addPlayerInventory(inventory, 8, HEIGHT - 82);
    }

    @Override
    protected Block block() {
        return ModEquipment.MODULE_BENCH.get();
    }

    @Override
    protected boolean syncsNetwork() {
        return false;
    }

    public ItemStack equipment() {
        return container.getItem(0);
    }

    /** Tipo do equipamento na bancada, ou null. */
    public @Nullable EquipmentKind kind() {
        return equipment().getItem() instanceof ModularItem modular ? modular.kind() : null;
    }

    /** Se o módulo pode ir no slot: cabe no equipamento, o slot existe e não repete módulo de outro slot. */
    public boolean accepts(int index, ItemStack stack) {
        EquipmentKind kind = kind();
        if (kind == null || index >= kind.slots() || !(stack.getItem() instanceof EquipmentModuleItem item)) return false;
        if (!item.module().fits(kind)) return false;
        for (int i = 0; i < EquipmentKind.MAX_SLOTS; i++) {
            if (i == index) continue;
            if (container.getItem(1 + i).getItem() instanceof EquipmentModuleItem other && other.module() == item.module()) return false;
        }
        return true;
    }

    private void containerChanged(Container changed) {
        if (updating || player.level().isClientSide()) return;
        updating = true;
        try {
            ItemStack equipment = container.getItem(0);
            if (equipment.isEmpty()) {
                // Equipamento saiu: os módulos foram junto (estão gravados nele).
                loadedFor = ItemStack.EMPTY;
                for (int i = 0; i < EquipmentKind.MAX_SLOTS; i++) container.setItem(1 + i, ItemStack.EMPTY);
            } else if (equipment != loadedFor) {
                loadedFor = equipment;
                List<ItemStack> modules = ModularItem.modules(equipment);
                for (int i = 0; i < EquipmentKind.MAX_SLOTS; i++) container.setItem(1 + i, modules.get(i).copy());
            } else {
                List<ItemStack> modules = new ArrayList<>();
                for (int i = 0; i < EquipmentKind.MAX_SLOTS; i++) modules.add(container.getItem(1 + i).copy());
                ModularItem.setModules(equipment, modules);
            }
        } finally {
            updating = false;
        }
    }

    /** Coloca um equipamento na bancada (vitrine e autoteste). */
    public void insertEquipment(ItemStack stack) {
        container.setItem(0, stack);
    }

    /** Coloca um módulo no slot (autoteste): como se o jogador o tivesse posto. */
    public void insertModule(int index, ItemStack stack) {
        slots.get(1 + index).set(stack);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide()) return;
        updating = true;
        for (int i = 0; i < EquipmentKind.MAX_SLOTS; i++) container.setItem(1 + i, ItemStack.EMPTY);
        updating = false;
        clearContainer(player, container);
    }
}
