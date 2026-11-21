package com.atomcraft.wireless;

import com.atomcraft.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Cartão de Rede: agachado + clique direito vincula o cartão a você. Coloque-o num slot do Transmissor
 * Sem Fio e o transmissor passa a alimentar suas ferramentas e equipamentos atômicos onde você estiver,
 * dentro do alcance do nível do cartão.
 */
public class NetworkCardItem extends Item {

    private final CardTier tier;

    public NetworkCardItem(CardTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CardTier tier() {
        return tier;
    }

    public static @Nullable CardOwner owner(ItemStack stack) {
        return stack.get(ModComponents.CARD_OWNER.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            player.getItemInHand(hand).set(ModComponents.CARD_OWNER.get(), new CardOwner(player.getUUID(), player.getName().getString()));
            player.sendOverlayMessage(Component.translatable("message.atomcraft.card_bound").withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModComponents.CARD_OWNER.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(tier.rangeText().copy().withStyle(ChatFormatting.DARK_AQUA));
        CardOwner owner = owner(stack);
        if (owner == null) {
            tooltip.accept(Component.translatable("tooltip.atomcraft.card_unbound").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.atomcraft.card_owner", owner.ownerName()).withStyle(ChatFormatting.AQUA));
        }
        tooltip.accept(Component.translatable("tooltip.atomcraft.card_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
