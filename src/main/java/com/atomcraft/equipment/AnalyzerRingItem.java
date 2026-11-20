package com.atomcraft.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Anel Analisador: no inventário ou num slot de anel do Curios, mostra a composição do bloco ou item mirado. */
public class AnalyzerRingItem extends Item {

    public AnalyzerRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.atomcraft.analyzer_ring").withStyle(ChatFormatting.GRAY));
    }
}
