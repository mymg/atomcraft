package com.atomcraft.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Módulo de equipamento: instalado na Bancada de Módulos. A pilha no slot é o nível do módulo. */
public class EquipmentModuleItem extends Item {

    private final EquipmentModule module;

    public EquipmentModuleItem(EquipmentModule module, Properties properties) {
        super(properties.stacksTo(module.maxLevel()));
        this.module = module;
    }

    public EquipmentModule module() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(module.description().copy().withStyle(ChatFormatting.GRAY));
        Component kinds = module == EquipmentModule.ENCHANTMENT ? Component.translatable("tooltip.atomcraft.module_for_all")
                : java.util.Arrays.stream(EquipmentKind.values()).filter(module::fits).map(EquipmentKind::label)
                .reduce((a, b) -> a.copy().append(", ").append(b)).orElse(Component.empty());
        tooltip.accept(Component.translatable("tooltip.atomcraft.module_for", kinds).withStyle(ChatFormatting.DARK_AQUA));
        if (module == EquipmentModule.ENCHANTMENT) {
            tooltip.accept(Component.translatable("tooltip.atomcraft.enchant_augment_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (module.maxLevel() > 1) {
            tooltip.accept(Component.translatable("tooltip.atomcraft.module_max", module.maxLevel()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (module.energyPerTick() > 0) {
            tooltip.accept(Component.translatable("tooltip.atomcraft.module_energy", module.energyPerTick()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
