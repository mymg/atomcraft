package com.atomcraft.tools;

import com.atomcraft.Config;
import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.registry.ModComponents;
import com.atomcraft.wireless.Wireless;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Desintegrador Atômico: quebra qualquer bloco de picareta, machado, pá ou enxada rapidamente,
 * gastando energia da bateria interna. Com um Cartão de Rede vinculado no inventário, os blocos
 * quebrados viram elementos direto nos Drives da rede (se couberem), e a bateria recarrega sozinha
 * pelo Transmissor Sem Fio. Agachado + clique alterna entre desintegrar e soltar os itens normalmente.
 */
public class AtomicDisintegratorItem extends Item {

    public static final int CAPACITY = 100_000, CHARGE_PER_TICK = 2_000;
    private static final float SPEED = 14f;

    public AtomicDisintegratorItem(Properties properties) {
        super(properties);
    }

    public static int energy(ItemStack stack) {
        Integer value = stack.get(ModComponents.STORED_ENERGY.get());
        return value == null ? 0 : value;
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.set(ModComponents.STORED_ENERGY.get(), Math.max(0, Math.min(CAPACITY, energy)));
    }

    /** Desintegra (elementos na rede) ou só minera (itens normais). */
    public static boolean atomizes(ItemStack stack) {
        return !Boolean.FALSE.equals(stack.get(ModComponents.DISINTEGRATE.get()));
    }

    private static boolean powered(ItemStack stack) {
        return energy(stack) >= Config.DISINTEGRATOR_ENERGY.get();
    }

    private static boolean mineable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL) || state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return powered(stack) && mineable(state) ? SPEED : 1f;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return powered(stack) && mineable(state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0f && powered(stack)) {
            setEnergy(stack, energy(stack) - Config.DISINTEGRATOR_ENERGY.get());
        }
        return true;
    }

    /** Recarrega a bateria pelo Transmissor Sem Fio vinculado. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player) || level.getGameTime() % 10 != 0 || energy(stack) >= CAPACITY) return;
        WirelessTransmitterBlockEntity transmitter = Wireless.find(player);
        if (transmitter != null) charge(stack, transmitter, CHARGE_PER_TICK * 10);
    }

    /** Passa até {@code max} FE do transmissor para a bateria. Devolve quanto passou. */
    public static int charge(ItemStack stack, WirelessTransmitterBlockEntity transmitter, int max) {
        int room = CAPACITY - energy(stack);
        int amount = Math.min(Math.min(room, max), transmitter.energy().getAmountAsInt());
        if (amount <= 0) return 0;
        transmitter.energy().set(transmitter.energy().getAmountAsInt() - amount);
        setEnergy(stack, energy(stack) + amount);
        return amount;
    }

    /**
     * Tenta guardar o item como elementos na rede do transmissor. Falso (nada muda) se o item não
     * tem composição conhecida ou se não couber.
     */
    public static boolean atomize(WirelessTransmitterBlockEntity transmitter, ItemStack drop) {
        AtomicValue value = AtomicValues.get(drop.getItem(), false);
        if (value == null || drop.isEmpty()) return false;
        return transmitter.network().insert(value.composition(), drop.getCount());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            boolean atomize = !atomizes(stack);
            stack.set(ModComponents.DISINTEGRATE.get(), atomize);
            player.sendSystemMessage(Component.translatable(atomize ? "message.atomcraft.disintegrate_on" : "message.atomcraft.disintegrate_off")
                    .withStyle(atomize ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * energy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3FD0FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.atomcraft.stored_energy",
                String.format("%,d", energy(stack)), String.format("%,d", CAPACITY)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(atomizes(stack) ? "tooltip.atomcraft.disintegrate_on" : "tooltip.atomcraft.disintegrate_off")
                .withStyle(atomizes(stack) ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.atomcraft.disintegrator_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
