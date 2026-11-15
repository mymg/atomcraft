package com.atomcraft.menu;

import com.atomcraft.block.entity.EnergyCellBlockEntity;
import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.energy.CellSide;
import com.atomcraft.energy.CellTier;
import com.atomcraft.energy.SideMode;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;

/** Célula de Energia / Matriz: sem slots; energia, fluxo e configuração dos lados (ou da estrutura). */
public class EnergyCellMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 124;

    private final ContainerData data;

    public static EnergyCellMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new EnergyCellMenu(id, inventory, buf.readBlockPos(), new SimpleContainerData(EnergyCellBlockEntity.DATA_COUNT));
    }

    public EnergyCellMenu(int id, Inventory inventory, EnergyCellBlockEntity cell) {
        this(id, inventory, cell.getBlockPos(), cell.data());
    }

    private EnergyCellMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(ModMenus.ENERGY_CELL.get(), id, inventory, pos);
        this.data = data;
        addMachineControlsWithoutUpgrades();
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return switch (tier()) {
            case BASIC -> ModBlocks.BASIC_ENERGY_CELL.get();
            case ADVANCED -> ModBlocks.ADVANCED_ENERGY_CELL.get();
            case ELITE -> ModBlocks.ELITE_ENERGY_CELL.get();
            case MATRIX -> ModBlocks.ENERGY_MATRIX.get();
        };
    }

    public CellTier tier() {
        return CellTier.byId(data.get(EnergyCellBlockEntity.DATA_TIER));
    }

    public int energy() {
        return combine(data.get(EnergyCellBlockEntity.DATA_ENERGY_LO), data.get(EnergyCellBlockEntity.DATA_ENERGY_HI));
    }

    public int capacity() {
        return combine(data.get(EnergyCellBlockEntity.DATA_CAPACITY_LO), data.get(EnergyCellBlockEntity.DATA_CAPACITY_HI));
    }

    public int input() {
        return combine(data.get(EnergyCellBlockEntity.DATA_IN_LO), data.get(EnergyCellBlockEntity.DATA_IN_HI));
    }

    public int output() {
        return combine(data.get(EnergyCellBlockEntity.DATA_OUT_LO), data.get(EnergyCellBlockEntity.DATA_OUT_HI));
    }

    public boolean formed() {
        return data.get(EnergyCellBlockEntity.DATA_FORMED) != 0;
    }

    public SideMode side(CellSide side) {
        return SideMode.byId(data.get(EnergyCellBlockEntity.DATA_SIDES + side.ordinal()));
    }

    public int portsIn() {
        return data.get(EnergyCellBlockEntity.DATA_PORTS_IN);
    }

    public int portsOut() {
        return data.get(EnergyCellBlockEntity.DATA_PORTS_OUT);
    }

    public MachineStatus status() {
        return formed() ? (input() > 0 || output() > 0 ? MachineStatus.WORKING : MachineStatus.IDLE) : MachineStatus.INCOMPLETE;
    }
}
