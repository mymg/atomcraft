package com.atomcraft.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Chip de upgrade, instalado na aba de upgrades das máquinas. */
public class UpgradeChipItem extends Item {

    private final UpgradeType type;

    public UpgradeChipItem(Properties properties, UpgradeType type) {
        super(properties);
        this.type = type;
    }

    public UpgradeType type() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(type.translationKey() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.atomcraft.upgrade_max", Upgrades.MAX_PER_TYPE).withStyle(ChatFormatting.DARK_GRAY));
    }
}
