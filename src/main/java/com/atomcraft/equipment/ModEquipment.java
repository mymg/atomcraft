package com.atomcraft.equipment;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.ModuleBenchBlock;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Exoesqueleto, Jetpack a Hidrogênio, acessórios, módulos e a Bancada de Módulos. */
public final class ModEquipment {

    public static final ArmorMaterial EXO_ARMOR = new ArmorMaterial(40, defense(3, 6, 8, 3, 11), 10,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 2f, 0.1f, TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/titanium_alloy")),
            asset("exoskeleton"));

    public static final DeferredBlock<ModuleBenchBlock> MODULE_BENCH = ModBlocks.BLOCKS.registerBlock("module_bench",
            ModuleBenchBlock::new, ModBlocks::machine);
    public static final DeferredItem<BlockItem> MODULE_BENCH_ITEM = ModItems.ITEMS.registerSimpleBlockItem(MODULE_BENCH);

    public static final DeferredItem<ExoArmorItem> EXO_HELMET = exo("exo_helmet", EquipmentKind.EXO_HELMET, ArmorType.HELMET);
    public static final DeferredItem<ExoArmorItem> EXO_CHESTPLATE = exo("exo_chestplate", EquipmentKind.EXO_CHESTPLATE, ArmorType.CHESTPLATE);
    public static final DeferredItem<ExoArmorItem> EXO_LEGGINGS = exo("exo_leggings", EquipmentKind.EXO_LEGGINGS, ArmorType.LEGGINGS);
    public static final DeferredItem<ExoArmorItem> EXO_BOOTS = exo("exo_boots", EquipmentKind.EXO_BOOTS, ArmorType.BOOTS);

    public static final DeferredItem<JetpackItem> JETPACK = ModItems.ITEMS.registerItem("hydrogen_jetpack", JetpackItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON).component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.CHEST).setEquipSound(SoundEvents.ARMOR_EQUIP_IRON).setAsset(asset("hydrogen_jetpack")).build()));
    public static final DeferredItem<MagnetItem> MAGNET = ModItems.ITEMS.registerItem("magnet_ring", MagnetItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<AnalyzerRingItem> ANALYZER_RING = ModItems.ITEMS.registerItem("analyzer_ring", AnalyzerRingItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<Item> BLANK_MODULE = ModItems.ITEMS.registerSimpleItem("blank_module");

    public static final Map<EquipmentModule, DeferredItem<EquipmentModuleItem>> MODULES = new EnumMap<>(EquipmentModule.class);
    public static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    static {
        TAB_ORDER.addAll(List.of(MODULE_BENCH_ITEM, EXO_HELMET, EXO_CHESTPLATE, EXO_LEGGINGS, EXO_BOOTS, JETPACK, MAGNET, ANALYZER_RING, BLANK_MODULE));
        for (EquipmentModule module : EquipmentModule.values()) {
            DeferredItem<EquipmentModuleItem> item = ModItems.ITEMS.registerItem(module.id() + "_module",
                    p -> new EquipmentModuleItem(module, p));
            MODULES.put(module, item);
            TAB_ORDER.add(item);
        }
    }

    private static DeferredItem<ExoArmorItem> exo(String name, EquipmentKind kind, ArmorType type) {
        return ModItems.ITEMS.registerItem(name, p -> new ExoArmorItem(kind, p),
                p -> p.humanoidArmor(EXO_ARMOR, type).rarity(Rarity.RARE));
    }

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, AtomCraft.id(name));
    }

    private static Map<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet, int body) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, leggings);
        map.put(ArmorType.CHESTPLATE, chestplate);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    /** Força o carregamento da classe (os registros acontecem nos campos estáticos). */
    public static void init() {}

    private ModEquipment() {}
}
