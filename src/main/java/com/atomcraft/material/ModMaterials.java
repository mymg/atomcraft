package com.atomcraft.material;

import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Registro dos itens e blocos de materiais: lingotes, chapas, blocos de construção, minérios e componentes. */
public final class ModMaterials {

    public static final Map<Material, DeferredItem<Item>> INGOTS = new EnumMap<>(Material.class);
    public static final Map<Material, DeferredItem<Item>> PLATES = new EnumMap<>(Material.class);
    public static final Map<Material, DeferredBlock<Block>> BLOCKS = new EnumMap<>(Material.class);
    public static final Map<Material, DeferredBlock<SlabBlock>> SLABS = new EnumMap<>(Material.class);
    public static final Map<Material, DeferredBlock<StairBlock>> STAIRS = new EnumMap<>(Material.class);
    public static final Map<Material, DeferredBlock<WallBlock>> WALLS = new EnumMap<>(Material.class);

    public static final Map<OreType, DeferredBlock<Block>> ORES = new EnumMap<>(OreType.class);
    public static final Map<OreType, DeferredBlock<Block>> DEEPSLATE_ORES = new EnumMap<>(OreType.class);
    public static final Map<OreType, DeferredItem<Item>> RAW = new EnumMap<>(OreType.class);

    /** Itens na ordem da aba de materiais do modo criativo. */
    public static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    public static final DeferredItem<Item> COPPER_WIRE = component("copper_wire");
    public static final DeferredItem<Item> SILICON_WAFER = component("silicon_wafer");
    public static final DeferredItem<Item> CIRCUIT_BOARD = component("circuit_board");

    static {
        for (OreType ore : OreType.values()) {
            DeferredBlock<Block> stone = ModBlocks.BLOCKS.registerSimpleBlock(ore.id() + "_ore",
                    p -> p.mapColor(MapColor.STONE).strength(3f, 3f).requiresCorrectToolForDrops());
            DeferredBlock<Block> deepslate = ModBlocks.BLOCKS.registerSimpleBlock("deepslate_" + ore.id() + "_ore",
                    p -> p.mapColor(MapColor.DEEPSLATE).strength(4.5f, 3f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops());
            ORES.put(ore, stone);
            DEEPSLATE_ORES.put(ore, deepslate);
            TAB_ORDER.add(ModItems.ITEMS.registerSimpleBlockItem(stone));
            TAB_ORDER.add(ModItems.ITEMS.registerSimpleBlockItem(deepslate));
        }
        for (OreType ore : OreType.values()) {
            DeferredItem<Item> raw = ModItems.ITEMS.registerSimpleItem("raw_" + ore.id());
            RAW.put(ore, raw);
            TAB_ORDER.add(raw);
        }
        for (Material material : Material.values()) {
            String id = material.id();
            DeferredItem<Item> ingot = ModItems.ITEMS.registerSimpleItem(id + "_ingot");
            DeferredItem<Item> plate = ModItems.ITEMS.registerSimpleItem(id + "_plate");
            INGOTS.put(material, ingot);
            PLATES.put(material, plate);
            DeferredBlock<Block> block = ModBlocks.BLOCKS.registerSimpleBlock(id + "_block", ModMaterials::metal);
            DeferredBlock<SlabBlock> slab = ModBlocks.BLOCKS.registerBlock(id + "_slab", SlabBlock::new, ModMaterials::metal);
            DeferredBlock<StairBlock> stairs = ModBlocks.BLOCKS.registerBlock(id + "_stairs",
                    p -> new StairBlock(block.get().defaultBlockState(), p), ModMaterials::metal);
            DeferredBlock<WallBlock> wall = ModBlocks.BLOCKS.registerBlock(id + "_wall", WallBlock::new,
                    p -> metal(p).forceSolidOn());
            BLOCKS.put(material, block);
            SLABS.put(material, slab);
            STAIRS.put(material, stairs);
            WALLS.put(material, wall);
            TAB_ORDER.add(ingot);
            TAB_ORDER.add(plate);
            TAB_ORDER.add(ModItems.ITEMS.registerSimpleBlockItem(block));
            TAB_ORDER.add(ModItems.ITEMS.registerSimpleBlockItem(slab));
            TAB_ORDER.add(ModItems.ITEMS.registerSimpleBlockItem(stairs));
            TAB_ORDER.add(ModItems.ITEMS.registerSimpleBlockItem(wall));
        }
        TAB_ORDER.add(COPPER_WIRE);
        TAB_ORDER.add(SILICON_WAFER);
        TAB_ORDER.add(CIRCUIT_BOARD);
    }

    private static DeferredItem<Item> component(String name) {
        return ModItems.ITEMS.registerSimpleItem(name);
    }

    private static BlockBehaviour.Properties metal(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).strength(5f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    /** Força o carregamento da classe (os registros acontecem no bloco estático). */
    public static void init() {}

    private ModMaterials() {}
}
