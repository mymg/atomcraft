package com.atomcraft.block;

import com.atomcraft.energy.CellTier;
import com.atomcraft.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Item das Células de Energia e da Matriz: mostra a energia guardada (barra e tooltip). */
public class EnergyCellItem extends BlockItem {

    public EnergyCellItem(Block block, Properties properties) {
        super(block, properties);
    }

    public CellTier tier() {
        return getBlock() instanceof EnergyCellBlock cell ? cell.tier() : CellTier.MATRIX;
    }

    public static int stored(ItemStack stack) {
        Integer value = stack.get(ModComponents.STORED_ENERGY.get());
        return value == null ? 0 : value;
    }

    /** Célula cheia (para a aba do modo criativo). */
    public static ItemStack full(Item item) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof EnergyCellItem cell) stack.set(ModComponents.STORED_ENERGY.get(), cell.tier().capacity());
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stored(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * stored(stack) / Math.max(1, tier().capacity()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE0302C;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.atomcraft.stored_energy",
                String.format("%,d", stored(stack)), String.format("%,d", tier().capacity())).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.atomcraft.energy_rate", String.format("%,d", tier().rate()))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
