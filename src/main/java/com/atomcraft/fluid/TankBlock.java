package com.atomcraft.fluid;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

/** Tanque de Fluidos (4 níveis). Clique com balde/tanque para encher ou esvaziar; sem item, mostra o conteúdo. */
public class TankBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    private final FluidTier tier;

    public TankBlock(FluidTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public FluidTier tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new TankBlock(tier, properties));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        // Tanque em modo bloco na mão: deixa colocar outro tanque em cima
        if (stack.getItem() instanceof TankItem && !TankItem.bucketMode(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) return InteractionResult.SUCCESS;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            player.sendOverlayMessage(describe(tank.tank()));
        }
        return InteractionResult.SUCCESS;
    }

    public static Component describe(FluidTank tank) {
        if (tank.isEmpty()) {
            return Component.translatable("message.atomcraft.tank_empty", String.format("%,d", tank.capacity())).withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("message.atomcraft.tank_contents", tank.resource().getHoverName(),
                String.format("%,d", tank.amount()), String.format("%,d", tank.capacity())).withStyle(ChatFormatting.AQUA);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof TankBlockEntity tank) tank.serverTick();
        };
    }
}
