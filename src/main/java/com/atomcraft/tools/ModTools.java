package com.atomcraft.tools;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.WirelessTransmitterBlock;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModItems;
import com.atomcraft.wireless.NetworkCardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ferramentas e armaduras de liga, o Desintegrador Atômico, o Cartão de Rede e o Transmissor Sem Fio.
 * <ul>
 *   <li>Aço: um pouco melhor que ferro (minera o que o ferro minera).</li>
 *   <li>Liga de Titânio: nível de diamante, bem mais durável e mais rápida.</li>
 *   <li>Carbeto de Tungstênio: nível de netherite, a mais rápida para minerar (sem armadura: pesado demais).</li>
 * </ul>
 */
public final class ModTools {

    public enum Tier {
        STEEL("steel", new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 7f, 2.5f, 12, ingots("steel"))),
        TITANIUM_ALLOY("titanium_alloy", new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8.5f, 3f, 16, ingots("titanium_alloy"))),
        TUNGSTEN_CARBIDE("tungsten_carbide", new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2400, 11f, 4f, 8, ingots("tungsten_carbide")));

        private final String id;
        private final ToolMaterial material;

        Tier(String id, ToolMaterial material) {
            this.id = id;
            this.material = material;
        }

        public String id() {
            return id;
        }

        public ToolMaterial material() {
            return material;
        }
    }

    public static final ArmorMaterial STEEL_ARMOR = new ArmorMaterial(20, defense(2, 6, 7, 3, 7), 12,
            SoundEvents.ARMOR_EQUIP_IRON, 1f, 0f, ingots("steel"), asset("steel"));
    public static final ArmorMaterial TITANIUM_ARMOR = new ArmorMaterial(35, defense(3, 6, 8, 3, 11), 15,
            SoundEvents.ARMOR_EQUIP_DIAMOND, 2.5f, 0.05f, ingots("titanium_alloy"), asset("titanium_alloy"));

    public static final Map<Tier, DeferredItem<Item>> SWORDS = new EnumMap<>(Tier.class);
    public static final Map<Tier, DeferredItem<Item>> PICKAXES = new EnumMap<>(Tier.class);
    public static final Map<Tier, DeferredItem<AxeItem>> AXES = new EnumMap<>(Tier.class);
    public static final Map<Tier, DeferredItem<ShovelItem>> SHOVELS = new EnumMap<>(Tier.class);
    public static final Map<Tier, DeferredItem<HoeItem>> HOES = new EnumMap<>(Tier.class);
    /** Armaduras: aço e liga de titânio, na ordem capacete, peitoral, calças, botas. */
    public static final List<DeferredItem<Item>> ARMOR = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    public static final DeferredBlock<WirelessTransmitterBlock> WIRELESS_TRANSMITTER = ModBlocks.BLOCKS.registerBlock("wireless_transmitter",
            WirelessTransmitterBlock::new, ModBlocks::machine);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> WIRELESS_TRANSMITTER_ITEM =
            ModItems.ITEMS.registerSimpleBlockItem(WIRELESS_TRANSMITTER);
    public static final DeferredItem<NetworkCardItem> NETWORK_CARD = ModItems.ITEMS.registerItem("network_card",
            NetworkCardItem::new, properties -> properties.stacksTo(1));
    public static final DeferredItem<AtomicDisintegratorItem> DISINTEGRATOR = ModItems.ITEMS.registerItem("atomic_disintegrator",
            AtomicDisintegratorItem::new, properties -> properties.stacksTo(1).rarity(Rarity.RARE));

    static {
        TAB_ORDER.add(WIRELESS_TRANSMITTER_ITEM);
        TAB_ORDER.add(NETWORK_CARD);
        TAB_ORDER.add(DISINTEGRATOR);
        for (Tier tier : Tier.values()) {
            ToolMaterial m = tier.material();
            String id = tier.id();
            SWORDS.put(tier, ModItems.ITEMS.registerItem(id + "_sword", Item::new, p -> p.sword(m, 3f, -2.4f)));
            PICKAXES.put(tier, ModItems.ITEMS.registerItem(id + "_pickaxe", Item::new, p -> p.pickaxe(m, 1f, -2.8f)));
            AXES.put(tier, ModItems.ITEMS.registerItem(id + "_axe", p -> new AxeItem(m, 6f, -3.1f, p)));
            SHOVELS.put(tier, ModItems.ITEMS.registerItem(id + "_shovel", p -> new ShovelItem(m, 1.5f, -3f, p)));
            HOES.put(tier, ModItems.ITEMS.registerItem(id + "_hoe", p -> new HoeItem(m, -2f, -1f, p)));
            TAB_ORDER.addAll(List.of(SWORDS.get(tier), PICKAXES.get(tier), AXES.get(tier), SHOVELS.get(tier), HOES.get(tier)));
        }
        for (var entry : List.of(Map.entry("steel", STEEL_ARMOR), Map.entry("titanium_alloy", TITANIUM_ARMOR))) {
            for (var type : List.of(Map.entry("helmet", ArmorType.HELMET), Map.entry("chestplate", ArmorType.CHESTPLATE),
                    Map.entry("leggings", ArmorType.LEGGINGS), Map.entry("boots", ArmorType.BOOTS))) {
                DeferredItem<Item> item = ModItems.ITEMS.registerItem(entry.getKey() + "_" + type.getKey(), Item::new,
                        p -> p.humanoidArmor(entry.getValue(), type.getValue()));
                ARMOR.add(item);
                TAB_ORDER.add(item);
            }
        }
    }

    private static TagKey<Item> ingots(String material) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/" + material));
    }

    private static ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset(String name) {
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

    private ModTools() {}
}
