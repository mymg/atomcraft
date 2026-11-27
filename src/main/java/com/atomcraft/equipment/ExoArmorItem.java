package com.atomcraft.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Peça do Exoesqueleto: armadura com bateria e módulos (visão noturna, respirador, blindagem, escudo,
 * velocidade, subida de degraus, salto e amortecedor). A bateria recarrega pelo Transmissor Sem Fio
 * com um Cartão de Rede vinculado; os efeitos ficam em {@link EquipmentEvents}.
 */
public class ExoArmorItem extends Item implements ModularItem, Powered {

    public static final int CAPACITY = 250_000;

    private final EquipmentKind kind;
    private final int capacity;

    public ExoArmorItem(EquipmentKind kind, Properties properties) {
        this(kind, CAPACITY, properties);
    }

    protected ExoArmorItem(EquipmentKind kind, int capacity, Properties properties) {
        super(properties);
        this.kind = kind;
        this.capacity = capacity;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public EquipmentKind kind() {
        return kind;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Battery.barWidth(stack, capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3FD0FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.atomcraft.stored_energy",
                String.format("%,d", Battery.energy(stack)), String.format("%,d", capacity)).withStyle(ChatFormatting.GRAY));
        ModularItem.appendModules(stack, kind, tooltip);
    }
}
