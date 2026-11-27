package com.atomcraft.tools;

import com.atomcraft.AtomCraft;
import com.atomcraft.Config;
import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.atomcraft.equipment.Battery;
import com.atomcraft.equipment.EquipmentKind;
import com.atomcraft.equipment.EquipmentModule;
import com.atomcraft.equipment.ModularItem;
import com.atomcraft.equipment.Powered;
import com.atomcraft.wireless.Wireless;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Katana Nano Desgastada (fim de jogo): 12 de dano e 1,8 de velocidade de ataque enquanto tem energia;
 * sem energia, o dano cai para 1. Cada golpe gasta energia. Aprimoramentos: Afiação (+2 de dano por nível)
 * e o Aprimoramento de Encantamento.
 */
public class NanoKatanaItem extends Item implements ModularItem, Powered {

    public static final int CAPACITY = com.atomcraft.equipment.NanoArmorItem.CAPACITY;
    public static final double DAMAGE = 11, SPEED = -2.2, SHARPNESS_PER_LEVEL = 2;
    private static final Identifier SHARPNESS_ID = AtomCraft.id("katana_sharpness");

    public NanoKatanaItem(Properties properties) {
        super(properties);
    }

    /** 12 de dano (1 da mão + 11) e 1,8 de velocidade (4 − 2,2), como uma espada. */
    public static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, DAMAGE, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, SPEED, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public EquipmentKind kind() {
        return EquipmentKind.KATANA;
    }

    @Override
    public int capacity() {
        return CAPACITY;
    }

    public static boolean charged(ItemStack stack) {
        return Battery.energy(stack) >= Config.KATANA_ENERGY.get();
    }

    /** Sem energia o dano cai para 1 (só a mão); com Afiação, +2 de dano por nível. */
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof NanoKatanaItem)) return;
        if (!charged(stack)) {
            event.removeModifier(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE_ID);
            return;
        }
        int sharpness = ModularItem.level(stack, EquipmentModule.SHARPNESS);
        if (sharpness > 0) {
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(SHARPNESS_ID, sharpness * SHARPNESS_PER_LEVEL,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
    }

    /** Cada golpe gasta energia. */
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof NanoKatanaItem && charged(stack)) {
            Battery.use(stack, Config.KATANA_ENERGY.get(), CAPACITY);
        }
    }

    /** Recarrega pelo Transmissor Sem Fio vinculado, como o Desintegrador. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player) || level.getGameTime() % 10 != 0 || Battery.energy(stack) >= CAPACITY) return;
        WirelessTransmitterBlockEntity transmitter = Wireless.find(player);
        if (transmitter != null) Battery.charge(stack, CAPACITY, transmitter, 20_000);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Battery.barWidth(stack, CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE04040;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.atomcraft.stored_energy",
                String.format("%,d", Battery.energy(stack)), String.format("%,d", CAPACITY)).withStyle(ChatFormatting.GRAY));
        if (!charged(stack)) tooltip.accept(Component.translatable("tooltip.atomcraft.katana_empty").withStyle(ChatFormatting.RED));
        ModularItem.appendModules(stack, kind(), tooltip);
        tooltip.accept(Component.translatable("tooltip.atomcraft.weathered_nano").withStyle(ChatFormatting.DARK_AQUA));
    }
}
