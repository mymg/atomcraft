package com.atomcraft.registry;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.atomcraft.block.entity.AtomizerBlockEntity;
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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DriveBlockEntity>> DRIVE =
            BLOCK_ENTITIES.register("drive", () -> new BlockEntityType<>(DriveBlockEntity::new, ModBlocks.DRIVE.get()));

    private ModBlockEntities() {}
}
