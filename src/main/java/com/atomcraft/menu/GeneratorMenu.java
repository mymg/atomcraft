package com.atomcraft.menu;

import com.atomcraft.block.entity.CarbonGeneratorBlockEntity;
import com.atomcraft.block.entity.CombustionGeneratorBlockEntity;
import com.atomcraft.block.entity.FusionReactorBlockEntity;
import com.atomcraft.block.entity.GeneratorBlockEntity;
import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.chemistry.Fuel;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModMenus;
import com.atomcraft.storage.FilteredItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Menu dos três geradores. Só o Gerador de Combustão tem slot (combustível) e inventário. */
public class GeneratorMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT_WITH_INVENTORY = 204, HEIGHT_COMPACT = 114;
    public static final int FUEL_X = 34, FUEL_Y = 43;

    private final ContainerData data;
    private final Supplier<Block> block;
    private final @Nullable FilteredItemHandler fuel;

    private GeneratorMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos, @Nullable FilteredItemHandler fuel,
                          ContainerData data, Supplier<Block> block) {
        super(type, id, inventory, pos);
        this.data = data;
        this.block = block;
        this.fuel = fuel;
        if (fuel != null) {
            addSlot(new ResourceHandlerSlot(fuel, fuel::set, 0, FUEL_X, FUEL_Y));
            machineSlots = 1;
            addPlayerInventory(inventory, 8, HEIGHT_WITH_INVENTORY - 82);
        }
        addDataSlots(data);
    }

    // ----- Servidor -----

    public static GeneratorMenu combustion(int id, Inventory inventory, CombustionGeneratorBlockEntity generator) {
        return new GeneratorMenu(ModMenus.COMBUSTION_GENERATOR.get(), id, inventory, generator.getBlockPos(), generator.fuel(),
                generator.data(), ModBlocks.COMBUSTION_GENERATOR::get);
    }

    public static GeneratorMenu carbon(int id, Inventory inventory, CarbonGeneratorBlockEntity generator) {
        return new GeneratorMenu(ModMenus.CARBON_GENERATOR.get(), id, inventory, generator.getBlockPos(), null,
                generator.data(), ModBlocks.CARBON_GENERATOR::get);
    }

    public static GeneratorMenu fusion(int id, Inventory inventory, FusionReactorBlockEntity reactor) {
        return new GeneratorMenu(ModMenus.FUSION_REACTOR.get(), id, inventory, reactor.getBlockPos(), null,
                reactor.data(), ModBlocks.FUSION_REACTOR::get);
    }

    // ----- Cliente -----

    public static GeneratorMenu combustionClient(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        FilteredItemHandler fuel = new FilteredItemHandler(1, 64, (slot, resource) -> Fuel.energy(resource.getItem(), true) > 0, () -> {});
        return new GeneratorMenu(ModMenus.COMBUSTION_GENERATOR.get(), id, inventory, buf.readBlockPos(), fuel,
                new SimpleContainerData(CombustionGeneratorBlockEntity.DATA_COUNT), ModBlocks.COMBUSTION_GENERATOR::get);
    }

    public static GeneratorMenu carbonClient(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new GeneratorMenu(ModMenus.CARBON_GENERATOR.get(), id, inventory, buf.readBlockPos(), null,
                new SimpleContainerData(CarbonGeneratorBlockEntity.DATA_COUNT), ModBlocks.CARBON_GENERATOR::get);
    }

    public static GeneratorMenu fusionClient(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new GeneratorMenu(ModMenus.FUSION_REACTOR.get(), id, inventory, buf.readBlockPos(), null,
                new SimpleContainerData(FusionReactorBlockEntity.DATA_COUNT), ModBlocks.FUSION_REACTOR::get);
    }

    @Override
    protected Block block() {
        return block.get();
    }

    public @Nullable FilteredItemHandler fuel() {
        return fuel;
    }

    public int energy() {
        return combine(data.get(0), data.get(1));
    }

    public int capacity() {
        return combine(data.get(2), data.get(3));
    }

    public int generating() {
        return combine(data.get(4), data.get(5));
    }

    public MachineStatus status() {
        return MachineStatus.byId(data.get(6));
    }

    /** Fração restante do item queimando (Gerador de Combustão). */
    public float burn() {
        return data.get(GeneratorBlockEntity.DATA_COMMON) / 1000f;
    }

    /** Temperatura do plasma, 0..1 (Reator de Fusão). */
    public float heat() {
        return data.get(FusionReactorBlockEntity.DATA_HEAT) / 1000f;
    }

    public int heliumProduced() {
        return combine(data.get(FusionReactorBlockEntity.DATA_HELIUM_LO), data.get(FusionReactorBlockEntity.DATA_HELIUM_HI));
    }
}
