package com.atomcraft.registry;

import com.atomcraft.AtomCraft;
import com.atomcraft.storage.ElementDiskItem;
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
    public static final DeferredItem<BlockItem> COMBUSTION_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.COMBUSTION_GENERATOR);
    public static final DeferredItem<BlockItem> CARBON_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.CARBON_GENERATOR);
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
