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
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/**
 * Atomizador: clique com um item na mão para decompô-lo em elementos.
 * Agachado (Shift) atomiza a pilha inteira.
 */
public class AtomizerBlock extends Block {

    public AtomizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level instanceof ServerLevel serverLevel)) return ItemInteractionResult.SUCCESS;

        Optional<Composition> composition = CompositionManager.get(stack.getItem());
        if (composition.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.atomcraft.no_composition", stack.getHoverName()), true);
            return ItemInteractionResult.FAIL;
        }

        int count = player.isShiftKeyDown() ? stack.getCount() : 1;
        ElementStorage.get(serverLevel.getServer()).add(composition.get(), count);
        player.displayClientMessage(Component.translatable("message.atomcraft.atomized",
                count, stack.getHoverName(), composition.get().formula()), true);
        stack.consume(count, player);

        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
        return ItemInteractionResult.SUCCESS;
    }
}
