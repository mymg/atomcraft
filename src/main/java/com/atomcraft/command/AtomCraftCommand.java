package com.atomcraft.command;

import com.atomcraft.block.ElementStorageReport;
import com.atomcraft.chemistry.CompositionManager;
import com.atomcraft.storage.ElementStorage;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * /atomcraft estoque      → lista os elementos guardados
 * /atomcraft composicao   → mostra a fórmula do item na mão
 */
public final class AtomCraftCommand {

    private AtomCraftCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("atomcraft")
                .then(Commands.literal("estoque").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ElementStorageReport.send(player, ElementStorage.get(ctx.getSource().getServer()));
                    return 1;
                }))
                .then(Commands.literal("composicao").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack held = player.getMainHandItem();
                    CompositionManager.get(held.getItem()).ifPresentOrElse(
                            c -> ctx.getSource().sendSuccess(() -> Component.translatable(
                                    "message.atomcraft.composition", held.getHoverName(), c.formula()), false),
                            () -> ctx.getSource().sendFailure(Component.translatable(
                                    "message.atomcraft.no_composition", held.getHoverName())));
                    return 1;
                })));
    }
}
