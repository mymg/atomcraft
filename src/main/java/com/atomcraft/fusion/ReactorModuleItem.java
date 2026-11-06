package com.atomcraft.fusion;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Módulo instalado na aba de módulos do controlador do Reator de Fusão. */
public class ReactorModuleItem extends Item {

    private final ModuleType type;
    private final int tier;

    public ReactorModuleItem(Properties properties, ModuleType type, int tier) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    public ModuleType type() {
        return type;
    }

    public int tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(type.translationKey() + ".desc", type.effect(tier)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.atomcraft.module_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
