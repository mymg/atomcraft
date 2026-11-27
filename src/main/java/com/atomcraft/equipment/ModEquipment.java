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
    public static final DeferredItem<AnalyzerGlassesItem> ANALYZER_GLASSES = ModItems.ITEMS.registerItem("analyzer_glasses", AnalyzerGlassesItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON).component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_GENERIC).build()));

    /** Armadura Nano Desgastada (fim de jogo): só em baús das Cidades Ancestrais. */
    public static final ArmorMaterial NANO_ARMOR = new ArmorMaterial(45, defense(5, 8, 10, 5, 13), 18,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 4f, 0.2f, TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/superalloy")),
            asset("weathered_nano"));
    public static final DeferredItem<NanoArmorItem> NANO_HELMET = nano("weathered_nano_helmet", EquipmentKind.NANO_HELMET, ArmorType.HELMET);
    public static final DeferredItem<NanoArmorItem> NANO_CHESTPLATE = nano("weathered_nano_chestplate", EquipmentKind.NANO_CHESTPLATE, ArmorType.CHESTPLATE);
    public static final DeferredItem<NanoArmorItem> NANO_LEGGINGS = nano("weathered_nano_leggings", EquipmentKind.NANO_LEGGINGS, ArmorType.LEGGINGS);
    public static final DeferredItem<NanoArmorItem> NANO_BOOTS = nano("weathered_nano_boots", EquipmentKind.NANO_BOOTS, ArmorType.BOOTS);
    /** Katana Nano Desgastada: 12 de dano e 1,8 de velocidade enquanto tem energia (sem energia, 1 de dano). */
    public static final DeferredItem<com.atomcraft.tools.NanoKatanaItem> NANO_KATANA = ModItems.ITEMS.registerItem("weathered_nano_katana",
            com.atomcraft.tools.NanoKatanaItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant().enchantable(18)
                    .attributes(com.atomcraft.tools.NanoKatanaItem.attributes()));

    public static final DeferredItem<Item> BLANK_MODULE = ModItems.ITEMS.registerSimpleItem("blank_augment");

    public static final Map<EquipmentModule, DeferredItem<EquipmentModuleItem>> MODULES = new EnumMap<>(EquipmentModule.class);
    public static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    static {
        TAB_ORDER.addAll(List.of(MODULE_BENCH_ITEM, EXO_HELMET, EXO_CHESTPLATE, EXO_LEGGINGS, EXO_BOOTS, JETPACK, MAGNET, ANALYZER_GLASSES, BLANK_MODULE,
                NANO_HELMET, NANO_CHESTPLATE, NANO_LEGGINGS, NANO_BOOTS, NANO_KATANA));
        for (EquipmentModule module : EquipmentModule.values()) {
            DeferredItem<EquipmentModuleItem> item = module == EquipmentModule.ENCHANTMENT
                    ? ModItems.ITEMS.registerItem(module.id() + "_augment", p -> new EquipmentModuleItem(module, p), p -> p.enchantable(15))
                    : ModItems.ITEMS.registerItem(module.id() + "_augment", p -> new EquipmentModuleItem(module, p));
            MODULES.put(module, item);
            TAB_ORDER.add(item);
        }
    }

    private static DeferredItem<ExoArmorItem> exo(String name, EquipmentKind kind, ArmorType type) {
        return ModItems.ITEMS.registerItem(name, p -> new ExoArmorItem(kind, p),
                p -> p.humanoidArmor(EXO_ARMOR, type).rarity(Rarity.RARE));
    }

    private static DeferredItem<NanoArmorItem> nano(String name, EquipmentKind kind, ArmorType type) {
        return ModItems.ITEMS.registerItem(name, p -> new NanoArmorItem(kind, p),
                p -> p.humanoidArmor(NANO_ARMOR, type).rarity(Rarity.EPIC).fireResistant());
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
