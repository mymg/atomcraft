package com.atomcraft.synthesis;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Schematic de uma tecnologia perdida, encontrada em baús do mundo. Enquanto
 * estiver na biblioteca de um Sintetizador, a máquina sabe fabricar o item.
 */
public class SchematicItem extends Item {

    private final Supplier<? extends Item> result;

    public SchematicItem(Properties properties, Supplier<? extends Item> result) {
        super(properties);
        this.result = result;
    }

    public Item result() {
        return result.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.atomcraft.schematic", new ItemStack(result()).getHoverName())
                .withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.atomcraft.schematic_hint").withStyle(ChatFormatting.GRAY));
    }
}
