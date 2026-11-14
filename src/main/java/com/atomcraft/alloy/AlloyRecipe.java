package com.atomcraft.alloy;

import com.atomcraft.chemistry.Composition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * Receita da Forja de Ligas: elementos da rede + energia → item. Arquivo em
 * {@code data/<namespace>/atomcraft/alloys/<nome>.json}:
 * <pre>
 * {
 *   "result": "atomcraft:steel_ingot",
 *   "count": 1,
 *   "elements": { "Fe": 8, "C": 1 },
 *   "energy": 4000,
 *   "ticks": 100,
 *   "order": 10
 * }
 * </pre>
 * Para não criar nem destruir matéria, os elementos devem bater com a composição do
 * resultado (a do aço é Fe8 C1). {@code order} define a posição na lista da máquina.
 */
public record AlloyRecipe(Item result, int count, Composition elements, long energy, int ticks, int order) {

    public static final Codec<AlloyRecipe> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(AlloyRecipe::result),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(AlloyRecipe::count),
            Composition.CODEC.fieldOf("elements").forGetter(AlloyRecipe::elements),
            Codec.LONG.optionalFieldOf("energy", 4000L).forGetter(AlloyRecipe::energy),
            Codec.intRange(1, 72_000).optionalFieldOf("ticks", 100).forGetter(AlloyRecipe::ticks),
            Codec.INT.optionalFieldOf("order", 1000).forGetter(AlloyRecipe::order)
    ).apply(i, AlloyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), AlloyRecipe::result,
            ByteBufCodecs.VAR_INT, AlloyRecipe::count,
            Composition.STREAM_CODEC, AlloyRecipe::elements,
            ByteBufCodecs.VAR_LONG, AlloyRecipe::energy,
            ByteBufCodecs.VAR_INT, AlloyRecipe::ticks,
            ByteBufCodecs.VAR_INT, AlloyRecipe::order,
            AlloyRecipe::new);
}
