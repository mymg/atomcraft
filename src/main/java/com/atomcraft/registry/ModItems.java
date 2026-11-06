package com.atomcraft.registry;

import com.atomcraft.AtomCraft;
import com.atomcraft.storage.ElementDiskItem;
import com.atomcraft.synthesis.SchematicItem;
import com.atomcraft.fusion.ModuleType;
import com.atomcraft.fusion.ReactorModuleItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.atomcraft.upgrade.UpgradeChipItem;
import com.atomcraft.upgrade.UpgradeType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AtomCraft.MOD_ID);

    public static final DeferredItem<BlockItem> ATOMIZER = ITEMS.registerSimpleBlockItem(ModBlocks.ATOMIZER);
    public static final DeferredItem<BlockItem> ANALYZER = ITEMS.registerSimpleBlockItem(ModBlocks.ANALYZER);
    public static final DeferredItem<BlockItem> REPLICATOR = ITEMS.registerSimpleBlockItem(ModBlocks.REPLICATOR);
    public static final DeferredItem<BlockItem> DRIVE = ITEMS.registerSimpleBlockItem(ModBlocks.DRIVE);
    public static final DeferredItem<BlockItem> CABLE = ITEMS.registerSimpleBlockItem(ModBlocks.CABLE);
    public static final DeferredItem<BlockItem> TERMINAL = ITEMS.registerSimpleBlockItem(ModBlocks.TERMINAL);
    public static final DeferredItem<BlockItem> SYNTHESIZER = ITEMS.registerSimpleBlockItem(ModBlocks.SYNTHESIZER,
            properties -> properties.rarity(Rarity.UNCOMMON));

    public static final DeferredItem<UpgradeChipItem> SPEED_UPGRADE = ITEMS.registerItem("speed_upgrade",
            properties -> new UpgradeChipItem(properties, UpgradeType.SPEED), properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<UpgradeChipItem> EFFICIENCY_UPGRADE = ITEMS.registerItem("efficiency_upgrade",
            properties -> new UpgradeChipItem(properties, UpgradeType.EFFICIENCY), properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SchematicItem> SPEED_UPGRADE_SCHEMATIC = ITEMS.registerItem("speed_upgrade_schematic",
            properties -> new SchematicItem(properties, SPEED_UPGRADE), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SchematicItem> EFFICIENCY_UPGRADE_SCHEMATIC = ITEMS.registerItem("efficiency_upgrade_schematic",
            properties -> new SchematicItem(properties, EFFICIENCY_UPGRADE), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> COMBUSTION_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.COMBUSTION_GENERATOR);
    public static final DeferredItem<BlockItem> CARBON_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.CARBON_GENERATOR);
    public static final DeferredItem<BlockItem> REACTOR_CASING = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_CASING);
    public static final DeferredItem<BlockItem> REACTOR_GLASS = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_GLASS);
    public static final DeferredItem<BlockItem> MAGNETIC_COIL = ITEMS.registerSimpleBlockItem(ModBlocks.MAGNETIC_COIL);
    public static final DeferredItem<BlockItem> CENTRAL_SOLENOID = ITEMS.registerSimpleBlockItem(ModBlocks.CENTRAL_SOLENOID,
            properties -> properties.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> REACTOR_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_PORT);

    /** Módulos do Reator de Fusão, por tipo e nível (I..III), e as schematics de cada um. */
    public static final Map<ModuleType, List<DeferredItem<ReactorModuleItem>>> MODULES = new EnumMap<>(ModuleType.class);
    public static final Map<ModuleType, List<DeferredItem<SchematicItem>>> MODULE_SCHEMATICS = new EnumMap<>(ModuleType.class);

    static {
        for (ModuleType type : ModuleType.values()) {
            List<DeferredItem<ReactorModuleItem>> modules = new ArrayList<>();
            List<DeferredItem<SchematicItem>> schematics = new ArrayList<>();
            for (int tier = 1; tier <= ModuleType.MAX_TIER; tier++) {
                final int t = tier;
                Rarity rarity = tier == 1 ? Rarity.UNCOMMON : tier == 2 ? Rarity.RARE : Rarity.EPIC;
                DeferredItem<ReactorModuleItem> module = ITEMS.registerItem(type.id() + "_module_" + tier,
                        properties -> new ReactorModuleItem(properties, type, t), properties -> properties.stacksTo(1).rarity(rarity));
                modules.add(module);
                schematics.add(ITEMS.registerItem(type.id() + "_module_" + tier + "_schematic",
                        properties -> new SchematicItem(properties, module), properties -> properties.stacksTo(1).rarity(rarity)));
            }
            MODULES.put(type, List.copyOf(modules));
            MODULE_SCHEMATICS.put(type, List.copyOf(schematics));
        }
    }

    public static final DeferredItem<BlockItem> FUSION_REACTOR = ITEMS.registerSimpleBlockItem(ModBlocks.FUSION_REACTOR,
            properties -> properties.rarity(Rarity.EPIC));

    public static final DeferredItem<ElementDiskItem> DISK_16K = ITEMS.registerItem("element_disk_16k",
            properties -> new ElementDiskItem(properties, 16_384L), properties -> properties.stacksTo(1));
    public static final DeferredItem<ElementDiskItem> DISK_64K = ITEMS.registerItem("element_disk_64k",
            properties -> new ElementDiskItem(properties, 65_536L), properties -> properties.stacksTo(1));
    public static final DeferredItem<ElementDiskItem> DISK_256K = ITEMS.registerItem("element_disk_256k",
            properties -> new ElementDiskItem(properties, 262_144L), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<ElementDiskItem> DISK_1M = ITEMS.registerItem("element_disk_1m",
            properties -> new ElementDiskItem(properties, 1_048_576L), properties -> properties.stacksTo(1).rarity(Rarity.RARE));

    private ModItems() {}
}
