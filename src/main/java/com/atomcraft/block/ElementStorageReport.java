package com.atomcraft.block;

import com.atomcraft.storage.ElementStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Mostra no chat o conteúdo do estoque de elementos. */
public final class ElementStorageReport {

    private ElementStorageReport() {}

    public static void send(Player player, ElementStorage storage) {
        if (storage.getAll().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.atomcraft.storage_empty"));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.atomcraft.storage_header").withStyle(ChatFormatting.AQUA));
        storage.getAll().forEach((element, amount) -> player.sendSystemMessage(
                Component.literal(" " + element.symbol() + " ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable(element.translationKey()).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(": " + amount).withStyle(ChatFormatting.WHITE))));
    }
}
