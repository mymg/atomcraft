package com.atomcraft.block;

import com.atomcraft.chemistry.Composition;
import com.atomcraft.chemistry.CompositionManager;
import com.atomcraft.storage.ElementStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Optional;

/**
 * Reconstrutor: clique com um item na mão (usado como "molde", não é consumido)
 * para criar uma cópia a partir dos elementos guardados.
 * Agachado (Shift) cria até uma pilha. Mão vazia mostra o estoque.
 */
public class AssemblerBlock extends Block {

    public AssemblerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack template, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (template.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level instanceof ServerLevel serverLevel)) return ItemInteractionResult.SUCCESS;

        Optional<Composition> composition = CompositionManager.get(template.getItem());
        if (composition.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.atomcraft.no_composition", template.getHoverName()), true);
            return ItemInteractionResult.FAIL;
        }

        ElementStorage storage = ElementStorage.get(serverLevel.getServer());
        int wanted = player.isShiftKeyDown() ? template.getMaxStackSize() : 1;
        int amount = (int) Math.min(wanted, storage.maxCraftable(composition.get()));
        if (amount <= 0) {
            player.displayClientMessage(Component.translatable("message.atomcraft.not_enough",
                    template.getHoverName(), composition.get().formula()), true);
            return ItemInteractionResult.FAIL;
        }

        storage.consume(composition.get(), amount);
        ItemHandlerHelper.giveItemToPlayer(player, template.copyWithCount(amount));
        player.displayClientMessage(Component.translatable("message.atomcraft.assembled", amount, template.getHoverName()), true);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.4f);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            ElementStorageReport.send(player, ElementStorage.get(serverLevel.getServer()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
