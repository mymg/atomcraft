package com.atomcraft.registry;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.AssemblerBlock;
import com.atomcraft.block.AtomizerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AtomCraft.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AtomCraft.MOD_ID);

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<AtomizerBlock> ATOMIZER =
            BLOCKS.register("atomizer", () -> new AtomizerBlock(machine()));
    public static final DeferredBlock<AssemblerBlock> ASSEMBLER =
            BLOCKS.register("assembler", () -> new AssemblerBlock(machine()));

    public static final DeferredItem<BlockItem> ATOMIZER_ITEM = ITEMS.registerSimpleBlockItem(ATOMIZER);
    public static final DeferredItem<BlockItem> ASSEMBLER_ITEM = ITEMS.registerSimpleBlockItem(ASSEMBLER);

    private ModBlocks() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
