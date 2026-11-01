package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.chemistry.Fuel;
import com.atomcraft.menu.GeneratorMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.FilteredItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Gerador de Combustão: queima itens que contêm carbono. A energia de cada item
 * depende da sua composição (ver {@link Fuel}). É o gerador inicial do mod.
 */
public class CombustionGeneratorBlockEntity extends GeneratorBlockEntity {

    public static final int DATA_BURN = DATA_COMMON, DATA_COUNT = DATA_COMMON + 1;

    private final FilteredItemHandler fuel = new FilteredItemHandler(1, 64,
            (slot, resource) -> Fuel.energy(resource.getItem(), isClientSide()) > 0, this::setChanged);
    private final ResourceHandler<ItemResource> automation = fuel.insertOnly();

    private long burnRemaining;
    private long burnTotal;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_BURN) return burnTotal <= 0 ? 0 : (int) (burnRemaining * 1000 / burnTotal);
            return commonData(index);
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state);
    }

    public FilteredItemHandler fuel() {
        return fuel;
    }

    public ResourceHandler<ItemResource> automation() {
        return automation;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    protected int generate() {
        int rate = Config.COMBUSTION_RATE.get();
        if (room() <= 0) {
            status = MachineStatus.IDLE;
            return 0;
        }
        if (burnRemaining <= 0) {
            burnTotal = 0;
            long energy = fuel.getAmountAsInt(0) > 0 ? Fuel.energy(fuel.getResource(0).getItem(), false) : 0;
            if (energy <= 0) {
                status = MachineStatus.NO_FUEL;
                return 0;
            }
            fuel.shrink(0, 1);
            burnRemaining = burnTotal = energy;
        }
        int generated = (int) Math.min(Math.min(rate, burnRemaining), room());
        burnRemaining -= generated;
        addEnergy(generated);
        status = MachineStatus.WORKING;
        return generated;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.combustion_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return GeneratorMenu.combustion(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) dropContents(level, pos, fuel);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel.deserialize(input.childOrEmpty("fuel"));
        burnRemaining = input.getLongOr("burn_remaining", 0L);
        burnTotal = input.getLongOr("burn_total", 0L);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fuel.serialize(output.child("fuel"));
        output.putLong("burn_remaining", burnRemaining);
        output.putLong("burn_total", burnTotal);
    }
}
