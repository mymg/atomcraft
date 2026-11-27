package com.atomcraft.fluid;

import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Blocos e itens de fluidos: tanques, canos e a bomba. */
public final class ModFluids {

    public static final Map<FluidTier, DeferredBlock<TankBlock>> TANKS = new EnumMap<>(FluidTier.class);
    public static final Map<FluidTier, DeferredItem<TankItem>> TANK_ITEMS = new EnumMap<>(FluidTier.class);
    public static final Map<FluidTier, DeferredBlock<PipeBlock>> PIPES = new EnumMap<>(FluidTier.class);
    public static final Map<FluidTier, DeferredItem<BlockItem>> PIPE_ITEMS = new EnumMap<>(FluidTier.class);
    public static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    public static final DeferredBlock<PumpBlock> PUMP = ModBlocks.BLOCKS.registerBlock("pump", PumpBlock::new, ModBlocks::machine);
    public static final DeferredItem<BlockItem> PUMP_ITEM = ModItems.ITEMS.registerSimpleBlockItem(PUMP);

    static {
        TAB_ORDER.add(PUMP_ITEM);
        for (FluidTier tier : FluidTier.values()) {
            DeferredBlock<TankBlock> tank = ModBlocks.BLOCKS.registerBlock(tier.id() + "_fluid_tank",
                    properties -> new TankBlock(tier, properties),
                    properties -> properties.mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
                            .isRedstoneConductor((state, level, pos) -> false));
            TANKS.put(tier, tank);
            DeferredItem<TankItem> tankItem = ModItems.ITEMS.registerItem(tier.id() + "_fluid_tank",
                    properties -> new TankItem(tank.get(), properties), properties -> properties.useBlockDescriptionPrefix().stacksTo(1));
            TANK_ITEMS.put(tier, tankItem);
            TAB_ORDER.add(tankItem);
        }
        for (FluidTier tier : FluidTier.values()) {
            DeferredBlock<PipeBlock> pipe = ModBlocks.BLOCKS.registerBlock(tier.id() + "_fluid_pipe",
                    properties -> new PipeBlock(tier, properties),
                    properties -> properties.mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.METAL).noOcclusion());
            PIPES.put(tier, pipe);
            DeferredItem<BlockItem> pipeItem = ModItems.ITEMS.registerSimpleBlockItem(pipe);
            PIPE_ITEMS.put(tier, pipeItem);
            TAB_ORDER.add(pipeItem);
        }
    }

    public static Block[] tankBlocks() {
        return TANKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
    }

    public static Block[] pipeBlocks() {
        return PIPES.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
    }

    public static void init() {}

    private ModFluids() {}
}
