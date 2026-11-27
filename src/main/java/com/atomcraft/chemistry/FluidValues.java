package com.atomcraft.chemistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Composição de 1 balde (1.000 mB) de um fluido: o valor do balde cheio menos o do balde vazio. Assim
 * fluidos de outros mods funcionam sozinhos, desde que o balde deles tenha valor atômico.
 */
public final class FluidValues {

    public static final int BUCKET = 1000;

    private FluidValues() {}

    /** Composição de 1.000 mB, ou null se o fluido não tem balde com valor conhecido. */
    public static @Nullable Composition perBucket(Fluid fluid, boolean clientSide) {
        if (fluid == Fluids.EMPTY) return null;
        Fluid source = fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
        var bucket = source.getBucket();
        if (bucket == null || bucket == Items.AIR || bucket == Items.BUCKET) return null;
        AtomicValue full = AtomicValues.get(bucket, clientSide);
        if (full == null) return null;
        AtomicValue empty = AtomicValues.get(Items.BUCKET, clientSide);
        Composition result = empty == null ? full.composition() : full.composition().minusClamped(empty.composition());
        return result.isEmpty() ? null : result;
    }

    public static boolean canAtomize(Fluid fluid, boolean clientSide) {
        return perBucket(fluid, clientSide) != null;
    }

    /** Fluidos-fonte (sem as versões "correndo") que têm composição conhecida. */
    public static List<Fluid> atomizable(boolean clientSide) {
        List<Fluid> list = new ArrayList<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY) continue;
            if (fluid instanceof FlowingFluid flowing && flowing.getSource() != fluid) continue;
            if (canAtomize(fluid, clientSide)) list.add(fluid);
        }
        return list;
    }
}
