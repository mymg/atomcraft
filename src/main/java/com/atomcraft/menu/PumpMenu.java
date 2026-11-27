package com.atomcraft.menu;

import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.fluid.PumpBlockEntity;
import com.atomcraft.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/** Bomba: energia, fluido guardado e progresso do próximo balde. */
public class PumpMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 166;

    private final ContainerData data;

    public static PumpMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new PumpMenu(id, inventory, buf.readBlockPos(), new SimpleContainerData(PumpBlockEntity.DATA_COUNT));
    }

    public PumpMenu(int id, Inventory inventory, PumpBlockEntity pump) {
        this(id, inventory, pump.getBlockPos(), pump.data());
    }

    private PumpMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(ModMenus.PUMP.get(), id, inventory, pos);
        this.data = data;
        machineSlots = 0;
        addMachineControls(WIDTH);
        addPlayerInventory(inventory, 8, HEIGHT - 82);
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return com.atomcraft.fluid.ModFluids.PUMP.get();
    }

    @Override
    protected boolean syncsNetwork() {
        return false;
    }

    public int energy() {
        return combine(data.get(PumpBlockEntity.DATA_ENERGY_LO), data.get(PumpBlockEntity.DATA_ENERGY_HI));
    }

    public int capacity() {
        return combine(data.get(PumpBlockEntity.DATA_CAPACITY_LO), data.get(PumpBlockEntity.DATA_CAPACITY_HI));
    }

    public Fluid fluid() {
        return BuiltInRegistries.FLUID.byId(data.get(PumpBlockEntity.DATA_FLUID));
    }

    public int amount() {
        return combine(data.get(PumpBlockEntity.DATA_AMOUNT_LO), data.get(PumpBlockEntity.DATA_AMOUNT_HI));
    }

    public float progress() {
        return data.get(PumpBlockEntity.DATA_PROGRESS) / 1000f;
    }

    public MachineStatus status() {
        return MachineStatus.byId(data.get(PumpBlockEntity.DATA_STATUS));
    }
}
