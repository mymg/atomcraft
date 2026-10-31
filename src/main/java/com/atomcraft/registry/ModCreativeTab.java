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
                output.accept(ModItems.DRIVE.get());
                output.accept(ModItems.CABLE.get());
                output.accept(ModItems.DISK_16K.get());
                output.accept(ModItems.DISK_64K.get());
                output.accept(ModItems.DISK_256K.get());
                output.accept(ModItems.DISK_1M.get());
            })
            .build());

    private ModCreativeTab() {}
}
