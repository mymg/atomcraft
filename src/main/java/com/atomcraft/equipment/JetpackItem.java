package com.atomcraft.equipment;

import com.atomcraft.Config;
import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Jetpack a Hidrogênio: vestido no peito (ou nas costas, com o Curios). Segure pular para subir.
 * O tanque enche sozinho com hidrogênio dos Drives, pelo Transmissor Sem Fio vinculado ao Cartão de Rede.
 * Módulos: Propulsor (mais força) e Voo (voo como no modo criativo, gastando hidrogênio; a tecla H
 * liga/desliga).
 */
public class JetpackItem extends Item implements ModularItem, com.geckolib.animatable.GeoItem {

    private final com.geckolib.animatable.instance.AnimatableInstanceCache geoCache = com.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /** Modelo GeckoLib {@code hydrogen_jetpack} (Blockbench, tipo Armor). */
    @Override
    public void createGeoRenderer(Consumer<com.geckolib.animatable.client.GeoRenderProvider> consumer) {
        consumer.accept(com.atomcraft.client.render.GeoArmors.provider("hydrogen_jetpack"));
    }

    @Override
    public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    public static final Composition HYDROGEN = new Composition(Map.of(ChemicalElement.HYDROGEN, 1L));

    public JetpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentKind kind() {
        return EquipmentKind.JETPACK;
    }

    public static int capacity() {
        return Config.JETPACK_TANK.get();
    }

    public static int fuel(ItemStack stack) {
        Integer value = stack.get(ModComponents.HYDROGEN.get());
        return value == null ? 0 : value;
    }

    public static void setFuel(ItemStack stack, int fuel) {
        stack.set(ModComponents.HYDROGEN.get(), Math.max(0, Math.min(capacity(), fuel)));
    }

    public static boolean hover(ItemStack stack) {
        return !Boolean.FALSE.equals(stack.get(ModComponents.HOVER.get())) && ModularItem.level(stack, EquipmentModule.HOVER) > 0;
    }

    /** Hidrogênio gasto por tick subindo. */
    public static int thrustCost(ItemStack stack) {
        return Config.JETPACK_USE.get() * (1 + ModularItem.level(stack, EquipmentModule.THRUSTER));
    }

    /** Enche o tanque com hidrogênio da rede do transmissor (até {@code max}). Devolve quanto entrou. */
    public static int refuel(ItemStack stack, WirelessTransmitterBlockEntity transmitter, int max) {
        var network = transmitter.network();
        long amount = Math.min(Math.min(capacity() - fuel(stack), max), network.maxExtractable(HYDROGEN));
        if (amount <= 0 || !network.extract(HYDROGEN, amount)) return 0;
        setFuel(stack, fuel(stack) + (int) amount);
        return (int) amount;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * fuel(stack) / capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE8F0FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.atomcraft.hydrogen",
                String.format("%,d", fuel(stack)), String.format("%,d", capacity())).withStyle(ChatFormatting.GRAY));
        if (ModularItem.level(stack, EquipmentModule.HOVER) > 0) {
            tooltip.accept(Component.translatable(hover(stack) ? "tooltip.atomcraft.hover_on" : "tooltip.atomcraft.hover_off")
                    .withStyle(hover(stack) ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        }
        ModularItem.appendModules(stack, EquipmentKind.JETPACK, tooltip);
        tooltip.accept(Component.translatable("tooltip.atomcraft.jetpack_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
