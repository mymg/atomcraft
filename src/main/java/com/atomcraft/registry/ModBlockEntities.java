package com.atomcraft.registry;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.atomcraft.block.entity.AtomizerBlockEntity;
import com.atomcraft.block.entity.CarbonGeneratorBlockEntity;
import com.atomcraft.block.entity.CombustionGeneratorBlockEntity;
import com.atomcraft.block.entity.FusionReactorBlockEntity;
import com.atomcraft.block.entity.SynthesizerBlockEntity;
import com.atomcraft.block.entity.ReactorPortBlockEntity;
import com.atomcraft.block.entity.TerminalBlockEntity;
import com.atomcraft.block.entity.DriveBlockEntity;
import com.atomcraft.block.entity.ReplicatorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AtomCraft.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AtomizerBlockEntity>> ATOMIZER =
            BLOCK_ENTITIES.register("atomizer", () -> new BlockEntityType<>(AtomizerBlockEntity::new, ModBlocks.ATOMIZER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnalyzerBlockEntity>> ANALYZER =
            BLOCK_ENTITIES.register("analyzer", () -> new BlockEntityType<>(AnalyzerBlockEntity::new, ModBlocks.ANALYZER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReplicatorBlockEntity>> REPLICATOR =
            BLOCK_ENTITIES.register("replicator", () -> new BlockEntityType<>(ReplicatorBlockEntity::new, ModBlocks.REPLICATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomcraft.block.entity.EnergyCellBlockEntity>> ENERGY_CELL =
            BLOCK_ENTITIES.register("energy_cell", () -> new BlockEntityType<>(com.atomcraft.block.entity.EnergyCellBlockEntity::new,
                    ModBlocks.BASIC_ENERGY_CELL.get(), ModBlocks.ADVANCED_ENERGY_CELL.get(), ModBlocks.ELITE_ENERGY_CELL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomcraft.block.entity.EnergyCellBlockEntity>> ENERGY_MATRIX =
            BLOCK_ENTITIES.register("energy_matrix", () -> new BlockEntityType<>(com.atomcraft.block.entity.EnergyCellBlockEntity::new,
                    ModBlocks.ENERGY_MATRIX.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomcraft.block.entity.MatrixPortBlockEntity>> MATRIX_PORT =
            BLOCK_ENTITIES.register("matrix_port", () -> new BlockEntityType<>(com.atomcraft.block.entity.MatrixPortBlockEntity::new,
                    ModBlocks.MATRIX_PORT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReplicatorBlockEntity>> ADVANCED_REPLICATOR =
            BLOCK_ENTITIES.register("advanced_replicator", () -> new BlockEntityType<>(ReplicatorBlockEntity::new, ModBlocks.ADVANCED_REPLICATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReplicatorBlockEntity>> QUANTUM_REPLICATOR =
            BLOCK_ENTITIES.register("quantum_replicator", () -> new BlockEntityType<>(ReplicatorBlockEntity::new, ModBlocks.QUANTUM_REPLICATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DriveBlockEntity>> DRIVE =
            BLOCK_ENTITIES.register("drive", () -> new BlockEntityType<>(DriveBlockEntity::new, ModBlocks.DRIVE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITIES.register("combustion_generator", () -> new BlockEntityType<>(CombustionGeneratorBlockEntity::new, ModBlocks.COMBUSTION_GENERATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarbonGeneratorBlockEntity>> CARBON_GENERATOR =
            BLOCK_ENTITIES.register("carbon_generator", () -> new BlockEntityType<>(CarbonGeneratorBlockEntity::new, ModBlocks.CARBON_GENERATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionReactorBlockEntity>> FUSION_REACTOR =
            BLOCK_ENTITIES.register("fusion_reactor", () -> new BlockEntityType<>(FusionReactorBlockEntity::new, ModBlocks.FUSION_REACTOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerminalBlockEntity>> TERMINAL =
            BLOCK_ENTITIES.register("terminal", () -> new BlockEntityType<>(TerminalBlockEntity::new, ModBlocks.TERMINAL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.atomcraft.block.entity.AlloyForgeBlockEntity>> ALLOY_FORGE =
            BLOCK_ENTITIES.register("alloy_forge", () -> new BlockEntityType<>(com.atomcraft.block.entity.AlloyForgeBlockEntity::new, ModBlocks.ALLOY_FORGE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SynthesizerBlockEntity>> SYNTHESIZER =
            BLOCK_ENTITIES.register("synthesizer", () -> new BlockEntityType<>(SynthesizerBlockEntity::new, ModBlocks.SYNTHESIZER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorPortBlockEntity>> REACTOR_PORT =
            BLOCK_ENTITIES.register("reactor_port", () -> new BlockEntityType<>(ReactorPortBlockEntity::new, ModBlocks.REACTOR_PORT.get()));

    private ModBlockEntities() {}
}
