package com.atomcraft.wireless;

import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Cartão de Rede: agachado + clique num Transmissor Sem Fio vincula o cartão ao transmissor e a
 * você. Com o cartão no inventário, suas ferramentas e equipamentos atômicos usam a rede à
 * distância. Agachado + clique no ar desfaz o vínculo.
 */
public class NetworkCardItem extends Item {

    public NetworkCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof WirelessTransmitterBlockEntity)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            context.getItemInHand().set(ModComponents.WIRELESS_LINK.get(), new WirelessLink(level.dimension(),
                    context.getClickedPos().immutable(), player.getUUID(), player.getName().getString()));
            player.sendSystemMessage(Component.translatable("message.atomcraft.card_linked",
                    context.getClickedPos().toShortString()).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(ModComponents.WIRELESS_LINK.get())) {
            if (!level.isClientSide()) {
                stack.remove(ModComponents.WIRELESS_LINK.get());
                player.sendSystemMessage(Component.translatable("message.atomcraft.card_unlinked").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModComponents.WIRELESS_LINK.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        WirelessLink link = stack.get(ModComponents.WIRELESS_LINK.get());
        if (link == null) {
            tooltip.accept(Component.translatable("tooltip.atomcraft.card_unlinked").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.accept(Component.translatable("tooltip.atomcraft.card_linked", link.pos().toShortString(),
                link.dimension().identifier().getPath()).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.atomcraft.card_owner", link.ownerName()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
