package com.atomcraft.registry;

import com.atomcraft.AtomCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AtomCraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.atomcraft"))
            .icon(() -> new ItemStack(ModItems.ATOMIZER.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.ATOMIZER.get());
                output.accept(ModItems.ANALYZER.get());
                output.accept(ModItems.REPLICATOR.get());
                output.accept(ModItems.ADVANCED_REPLICATOR.get());
                output.accept(ModItems.QUANTUM_REPLICATOR.get());
                output.accept(ModItems.QUANTUM_CASING.get());
                output.accept(ModItems.QUANTUM_GLASS.get());
                output.accept(ModItems.QUANTUM_CORE.get());
                output.accept(ModItems.QUANTUM_PORT.get());
                output.accept(ModItems.DRIVE.get());
                output.accept(ModItems.CABLE.get());
                output.accept(ModItems.TERMINAL.get());
                output.accept(ModItems.COMBUSTION_GENERATOR.get());
                output.accept(ModItems.CARBON_GENERATOR.get());
                output.accept(ModItems.FUSION_REACTOR.get());
                output.accept(ModItems.REACTOR_CASING.get());
                output.accept(ModItems.REACTOR_GLASS.get());
                output.accept(ModItems.MAGNETIC_COIL.get());
                output.accept(ModItems.CENTRAL_SOLENOID.get());
                output.accept(ModItems.REACTOR_PORT.get());
                output.accept(ModItems.SYNTHESIZER.get());
                output.accept(ModItems.ALLOY_FORGE.get());
                output.accept(ModItems.CHUNK_LOADER.get());
                for (var cell : java.util.List.of(ModItems.BASIC_ENERGY_CELL, ModItems.ADVANCED_ENERGY_CELL, ModItems.ELITE_ENERGY_CELL)) {
                    output.accept(cell.get());
                    output.accept(com.atomcraft.block.EnergyCellItem.full(cell.get()));
                }
                output.accept(ModItems.ENERGY_MATRIX.get());
                output.accept(ModItems.MATRIX_CASING.get());
                output.accept(ModItems.MATRIX_GLASS.get());
                output.accept(ModItems.MATRIX_CORE.get());
                output.accept(ModItems.MATRIX_PORT.get());
                output.accept(ModItems.SPEED_UPGRADE.get());
                output.accept(ModItems.EFFICIENCY_UPGRADE.get());
                output.accept(ModItems.SPEED_UPGRADE_SCHEMATIC.get());
                output.accept(ModItems.EFFICIENCY_UPGRADE_SCHEMATIC.get());
                ModItems.MODULES.values().forEach(list -> list.forEach(item -> output.accept(item.get())));
                ModItems.MODULE_SCHEMATICS.values().forEach(list -> list.forEach(item -> output.accept(item.get())));
                output.accept(ModItems.DISK_16K.get());
                output.accept(ModItems.DISK_64K.get());
                output.accept(ModItems.DISK_256K.get());
                output.accept(ModItems.DISK_1M.get());
            })
            .build());

    /** Aba de materiais: minérios, lingotes, chapas, blocos de construção e componentes. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIALS = TABS.register("materials", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.atomcraft.materials"))
            .icon(() -> new ItemStack(com.atomcraft.material.ModMaterials.INGOTS.get(com.atomcraft.material.Material.STEEL).get()))
            .withTabsBefore(MAIN.getKey())
            .displayItems((parameters, output) ->
                    com.atomcraft.material.ModMaterials.TAB_ORDER.forEach(item -> output.accept(item.get())))
            .build());

    /** Aba de equipamentos: ferramentas e armaduras de liga, Desintegrador, Transmissor e Cartão de Rede. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EQUIPMENT = TABS.register("equipment", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.atomcraft.equipment"))
            .icon(() -> new ItemStack(com.atomcraft.tools.ModTools.DISINTEGRATOR.get()))
            .withTabsBefore(MATERIALS.getKey())
            .displayItems((parameters, output) -> {
                for (var item : com.atomcraft.tools.ModTools.TAB_ORDER) {
                    if (item.get() instanceof com.atomcraft.tools.AtomicDisintegratorItem) {
                        ItemStack charged = new ItemStack(item.get());
                        com.atomcraft.tools.AtomicDisintegratorItem.setEnergy(charged, com.atomcraft.tools.AtomicDisintegratorItem.CAPACITY);
                        output.accept(charged);
                    } else {
                        output.accept(item.get());
                    }
                }
                for (var item : com.atomcraft.equipment.ModEquipment.TAB_ORDER) {
                    ItemStack stack = new ItemStack(item.get());
                    if (item.get() instanceof com.atomcraft.equipment.ExoArmorItem) {
                        com.atomcraft.equipment.Battery.set(stack, com.atomcraft.equipment.ExoArmorItem.CAPACITY, com.atomcraft.equipment.ExoArmorItem.CAPACITY);
                    } else if (item.get() instanceof com.atomcraft.equipment.JetpackItem) {
                        com.atomcraft.equipment.JetpackItem.setFuel(stack, com.atomcraft.equipment.JetpackItem.capacity());
                    }
                    output.accept(stack);
                }
            })
            .build());

    private ModCreativeTab() {}
}
