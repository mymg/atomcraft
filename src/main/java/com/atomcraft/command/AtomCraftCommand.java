package com.atomcraft.command;

import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.network.ModNetwork;
import com.atomcraft.research.ResearchData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * /atomcraft valor                     → valor atômico do item na mão
 * /atomcraft pesquisa desbloquear      → pesquisa o item na mão (operadores)
 * /atomcraft pesquisa tudo             → pesquisa todos os itens (operadores)
 */
public final class AtomCraftCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("atomcraft")
                .then(Commands.literal("valor").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack held = player.getMainHandItem();
                    AtomicValue value = AtomicValues.get(held.getItem(), false);
                    if (value == null) {
                        ctx.getSource().sendFailure(Component.translatable("message.atomcraft.no_value", held.getHoverName()));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.translatable("message.atomcraft.value",
                            held.getHoverName(),
                            value.composition().formula(),
                            Component.translatable(value.rarity().translationKey()).withStyle(value.rarity().color()),
                            value.energyCost(),
                            String.format("%.1f", value.replicateTicks() / 20.0),
                            value.researchRequired(held.getItem())), false);
                    return 1;
                }))
                .then(Commands.literal("pesquisa")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("desbloquear").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ItemStack held = player.getMainHandItem();
                            if (AtomicValues.get(held.getItem(), false) == null) {
                                ctx.getSource().sendFailure(Component.translatable("message.atomcraft.no_value", held.getHoverName()));
                                return 0;
                            }
                            ResearchData.get(ctx.getSource().getServer()).unlock(player.getUUID(), held.getItem());
                            ModNetwork.syncResearch(player);
                            ctx.getSource().sendSuccess(() -> Component.translatable("message.atomcraft.research_complete",
                                    held.getHoverName()).withStyle(ChatFormatting.GREEN), false);
                            return 1;
                        }))
                        .then(Commands.literal("tudo").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ResearchData research = ResearchData.get(ctx.getSource().getServer());
                            AtomicValues.serverValues().keySet().forEach(item -> research.unlock(player.getUUID(), item));
                            ModNetwork.syncResearch(player);
                            int count = AtomicValues.serverValues().size();
                            ctx.getSource().sendSuccess(() -> Component.translatable("message.atomcraft.research_all", count), false);
                            return count;
                        }))));
    }

    private AtomCraftCommand() {}
}
