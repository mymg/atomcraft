package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.menu.GeneratorMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.ElementNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Map;

/**
 * Gerador de Carbono: queima o carbono guardado na rede (C + O₂ → CO₂), um
 * átomo por vez. Totalmente automático e mais eficiente que queimar itens.
 */
public class CarbonGeneratorBlockEntity extends GeneratorBlockEntity {

    public static final int DATA_COUNT = DATA_COMMON;
    private static final Composition ONE_CARBON = new Composition(Map.of(ChemicalElement.CARBON, 1L));

    private long pending;
    private long burned;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return commonData(index);
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CarbonGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARBON_GENERATOR.get(), pos, state);
    }

    public ContainerData data() {
        return data;
    }

    @Override
    protected int generate() {
        int rate = boostedRate(Config.CARBON_RATE.get());
        if (room() <= 0) {
            status = MachineStatus.IDLE;
            return 0;
        }
        if (pending <= 0) {
            ElementNetwork network = network();
            if (!network.hasStorage()) {
                status = MachineStatus.NO_STORAGE;
                return 0;
            }
            if (!network.extract(ONE_CARBON, 1)) {
                status = MachineStatus.NO_ELEMENTS;
                return 0;
            }
            pending = boostedYield(Config.CARBON_FE_PER_ATOM.get());
            burned++;
        }
        int generated = (int) Math.min(Math.min(rate, pending), room());
        pending -= generated;
        addEnergy(generated);
        status = MachineStatus.WORKING;
        return generated;
    }

    public long burned() {
        return burned;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.carbon_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return GeneratorMenu.carbon(id, inventory, this);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pending = input.getLongOr("pending", 0L);
        burned = input.getLongOr("burned", 0L);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("pending", pending);
        output.putLong("burned", burned);
    }
}
