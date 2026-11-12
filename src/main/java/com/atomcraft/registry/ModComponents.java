package com.atomcraft.registry;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.storage.DiskPartition;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, AtomCraft.MOD_ID);

    /** Elementos guardados num disco. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Composition>> DISK_CONTENTS =
            COMPONENTS.registerComponentType("disk_contents",
                    builder -> builder.persistent(Composition.CODEC).networkSynchronized(Composition.STREAM_CODEC));

    /** Partição de um disco (quais elementos aceita e quanto de cada). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiskPartition>> DISK_PARTITION =
            COMPONENTS.registerComponentType("disk_partition",
                    builder -> builder.persistent(DiskPartition.CODEC).networkSynchronized(DiskPartition.STREAM_CODEC));

    private ModComponents() {}
}
