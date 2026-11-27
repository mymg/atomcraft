package com.atomcraft.fluid;

import com.atomcraft.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import java.util.function.Consumer;

/**
 * Item do Tanque de Fluidos. Guarda o fluido (componente {@code fluid_content}) e tem dois modos,
 * trocados com agachado + clique no ar:
 * <ul>
 *   <li><b>Bloco</b>: coloca o tanque no mundo, com o fluido dentro;</li>
 *   <li><b>Balde</b>: pega fontes de fluido do mundo e coloca de volta, 1 balde por clique, e enche ou
 *   esvazia blocos com fluido (outros tanques, Atomizador...).</li>
 * </ul>
 */
public class TankItem extends BlockItem {

    public static final int BUCKET = 1000;

    public TankItem(TankBlock block, Properties properties) {
        super(block, properties);
    }

    public FluidTier tier() {
        return ((TankBlock) getBlock()).tier();
    }

    public static boolean bucketMode(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModComponents.BUCKET_MODE.get()));
    }

    public static FluidStack contents(ItemStack stack) {
        SimpleFluidContent content = stack.get(ModComponents.FLUID_CONTENT.get());
        return content == null ? FluidStack.EMPTY : content.copy();
    }

    public static void setContents(ItemStack stack, FluidStack fluid) {
        if (fluid.isEmpty()) {
            stack.remove(ModComponents.FLUID_CONTENT.get());
        } else {
            stack.set(ModComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(fluid));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (!bucketMode(stack)) {
            if (player != null && player.isSecondaryUseActive() && context.getLevel().getBlockState(context.getClickedPos()).isAir()) {
                return InteractionResult.PASS;
            }
            return super.useOn(context);
        }
        // Modo balde: blocos com fluido (outros tanques, máquinas) enchem ou esvaziam o item
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player != null && level.getCapability(Capabilities.Fluid.BLOCK, pos, context.getClickedFace()) != null) {
            if (FluidUtil.interactWithFluidHandler(player, context.getHand(), level, pos, context.getClickedFace())) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            boolean bucket = !bucketMode(stack);
            stack.set(ModComponents.BUCKET_MODE.get(), bucket);
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable(bucket ? "message.atomcraft.tank_mode_bucket" : "message.atomcraft.tank_mode_block")
                        .withStyle(ChatFormatting.AQUA));
            }
            level.playSound(player, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3f, bucket ? 1.4f : 1.0f);
            return InteractionResult.SUCCESS;
        }
        if (!bucketMode(stack)) return InteractionResult.PASS;

        FluidStack contents = contents(stack);
        boolean canPickUp = contents.isEmpty() || contents.getAmount() + BUCKET <= tier().tankCapacity();
        BlockHitResult hit = getPlayerPOVHitResult(level, player, canPickUp ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();

        // Pegar uma fonte de fluido
        FluidState fluidState = level.getFluidState(pos);
        if (canPickUp && fluidState.isSource()) {
            Fluid fluid = fluidState.getType();
            if (!contents.isEmpty() && !contents.is(holder -> holder.value().isSame(fluid))) return InteractionResult.FAIL;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BucketPickup pickup)) return InteractionResult.FAIL;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            ItemStack bucket = pickup.pickupBlock(player, level, pos, state);
            Fluid picked = bucket.getItem() instanceof BucketItem bucketItem ? bucketItem.getContent() : fluid;
            if (bucket.isEmpty()) return InteractionResult.FAIL;
            pickup.getPickupSound().ifPresent(sound -> player.playSound(sound, 1.0f, 1.0f));
            int amount = contents.isEmpty() ? BUCKET : contents.getAmount() + BUCKET;
            setContents(stack, new FluidStack(picked, amount));
            return InteractionResult.SUCCESS;
        }

        // Colocar 1 balde no mundo
        if (contents.getAmount() >= BUCKET) {
            BlockPos target = level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(hit.getDirection());
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (FluidUtil.tryPlaceFluid(FluidResource.of(contents), player, level, hand, target)) {
                FluidStack left = contents.copyWithAmount(contents.getAmount() - BUCKET);
                setContents(stack, left);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !contents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * contents(stack).getAmount() / tier().tankCapacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3F76E4;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        FluidStack contents = contents(stack);
        if (contents.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.atomcraft.tank_empty", String.format("%,d", tier().tankCapacity()))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.atomcraft.tank_contents", contents.getHoverName(),
                    String.format("%,d", contents.getAmount()), String.format("%,d", tier().tankCapacity())).withStyle(ChatFormatting.AQUA));
        }
        tooltip.accept(Component.translatable(bucketMode(stack) ? "tooltip.atomcraft.tank_mode_bucket" : "tooltip.atomcraft.tank_mode_block")
                .withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.atomcraft.tank_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
