package com.atomcraft.registry;

import com.atomcraft.AtomCraft;
import com.atomcraft.menu.AnalyzerMenu;
import com.atomcraft.menu.AtomizerMenu;
import com.atomcraft.menu.DriveMenu;
import com.atomcraft.menu.GeneratorMenu;
import com.atomcraft.menu.TerminalMenu;
import com.atomcraft.menu.ReplicatorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AtomCraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AtomizerMenu>> ATOMIZER =
            MENUS.register("atomizer", () -> IMenuTypeExtension.create(AtomizerMenu::client));
    public static final DeferredHolder<MenuType<?>, MenuType<AnalyzerMenu>> ANALYZER =
            MENUS.register("analyzer", () -> IMenuTypeExtension.create(AnalyzerMenu::client));
    public static final DeferredHolder<MenuType<?>, MenuType<ReplicatorMenu>> REPLICATOR =
            MENUS.register("replicator", () -> IMenuTypeExtension.create(ReplicatorMenu::client));
    public static final DeferredHolder<MenuType<?>, MenuType<DriveMenu>> DRIVE =
            MENUS.register("drive", () -> IMenuTypeExtension.create(DriveMenu::client));

    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> COMBUSTION_GENERATOR =
            MENUS.register("combustion_generator", () -> IMenuTypeExtension.create(GeneratorMenu::combustionClient));
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> CARBON_GENERATOR =
            MENUS.register("carbon_generator", () -> IMenuTypeExtension.create(GeneratorMenu::carbonClient));
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> FUSION_REACTOR =
            MENUS.register("fusion_reactor", () -> IMenuTypeExtension.create(GeneratorMenu::fusionClient));
    public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> TERMINAL =
            MENUS.register("terminal", () -> IMenuTypeExtension.create(TerminalMenu::client));

    private ModMenus() {}
}
