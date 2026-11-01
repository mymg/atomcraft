package com.atomcraft.registry;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.AnalyzerBlock;
import com.atomcraft.block.AtomizerBlock;
import com.atomcraft.block.CableBlock;
import com.atomcraft.block.CarbonGeneratorBlock;
import com.atomcraft.block.CombustionGeneratorBlock;
import com.atomcraft.block.FusionReactorBlock;
import com.atomcraft.block.TerminalBlock;
import com.atomcraft.block.DriveBlock;
import com.atomcraft.block.ReplicatorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AtomCraft.MOD_ID);

    private static BlockBehaviour.Properties machine(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<AtomizerBlock> ATOMIZER = BLOCKS.registerBlock("atomizer", AtomizerBlock::new, ModBlocks::machine);
    public static final DeferredBlock<AnalyzerBlock> ANALYZER = BLOCKS.registerBlock("analyzer", AnalyzerBlock::new, ModBlocks::machine);
    public static final DeferredBlock<ReplicatorBlock> REPLICATOR = BLOCKS.registerBlock("replicator", ReplicatorBlock::new, ModBlocks::machine);
    public static final DeferredBlock<DriveBlock> DRIVE = BLOCKS.registerBlock("drive", DriveBlock::new, ModBlocks::machine);
    public static final DeferredBlock<CombustionGeneratorBlock> COMBUSTION_GENERATOR =
            BLOCKS.registerBlock("combustion_generator", CombustionGeneratorBlock::new, ModBlocks::machine);
    public static final DeferredBlock<CarbonGeneratorBlock> CARBON_GENERATOR =
            BLOCKS.registerBlock("carbon_generator", CarbonGeneratorBlock::new, ModBlocks::machine);
    public static final DeferredBlock<FusionReactorBlock> FUSION_REACTOR =
            BLOCKS.registerBlock("fusion_reactor", FusionReactorBlock::new, properties -> machine(properties).lightLevel(state -> 10));
    public static final DeferredBlock<TerminalBlock> TERMINAL = BLOCKS.registerBlock("terminal", TerminalBlock::new, ModBlocks::machine);
    public static final DeferredBlock<CableBlock> CABLE = BLOCKS.registerBlock("atomic_cable", CableBlock::new,
            properties -> properties.mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.METAL).noOcclusion());

    private ModBlocks() {}
}
