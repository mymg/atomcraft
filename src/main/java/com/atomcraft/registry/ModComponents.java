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

    /** Energia guardada numa Célula de Energia (ou Matriz) quebrada. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_ENERGY =
            COMPONENTS.registerComponentType("stored_energy",
                    builder -> builder.persistent(com.mojang.serialization.Codec.INT)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    /** Dono do Cartão de Rede. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.atomcraft.wireless.CardOwner>> CARD_OWNER =
            COMPONENTS.registerComponentType("card_owner",
                    builder -> builder.persistent(com.atomcraft.wireless.CardOwner.CODEC)
                            .networkSynchronized(com.atomcraft.wireless.CardOwner.STREAM_CODEC));

    /** Desintegrador Atômico: transforma os blocos quebrados em elementos (true) ou só minera (false). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DISINTEGRATE =
            COMPONENTS.registerComponentType("disintegrate",
                    builder -> builder.persistent(com.mojang.serialization.Codec.BOOL)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    /** Módulos instalados num equipamento (Bancada de Módulos). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> INSTALLED_MODULES =
            COMPONENTS.registerComponentType("installed_modules",
                    builder -> builder.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
                            .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC));

    /** Hidrogênio no tanque do Jetpack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HYDROGEN =
            COMPONENTS.registerComponentType("hydrogen",
                    builder -> builder.persistent(com.mojang.serialization.Codec.INT)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    /** Jetpack: modo de flutuação ligado. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HOVER =
            COMPONENTS.registerComponentType("hover",
                    builder -> builder.persistent(com.mojang.serialization.Codec.BOOL)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    /** Acessório ligado/desligado (Anel Magnético). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ACTIVE =
            COMPONENTS.registerComponentType("active",
                    builder -> builder.persistent(com.mojang.serialization.Codec.BOOL)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    private ModComponents() {}
}
