package com.atomcraft.equipment;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Armadura Nano Desgastada: a armadura mais forte do mod, achada só em baús das Cidades Ancestrais.
 * Funciona como o Exoesqueleto (bateria 8× maior, mesmos aprimoramentos) e cada peça tem um bônus próprio
 * enquanto tem energia: capacete = visão noturna, peitoral = +5 corações, calças = velocidade (andando,
 * nadando e voando), botas = sem dano de queda e de ambiente. Modelo GeckoLib {@code weathered_nano_armor}.
 */
public class NanoArmorItem extends ExoArmorItem implements GeoItem {

    public static final int CAPACITY = ExoArmorItem.CAPACITY * 8;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NanoArmorItem(EquipmentKind kind, Properties properties) {
        super(kind, CAPACITY, properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(com.atomcraft.client.render.GeoArmors.provider("weathered_nano_armor"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE04040;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.atomcraft.nano_bonus." + kind().id()).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.atomcraft.weathered_nano").withStyle(ChatFormatting.DARK_AQUA));
    }
}
