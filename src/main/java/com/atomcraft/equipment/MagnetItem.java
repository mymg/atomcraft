package com.atomcraft.equipment;

import com.atomcraft.Config;
import com.atomcraft.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Anel Magnético: puxa itens e orbes de experiência próximos, no inventário ou num slot de anel do Curios.
 * Agachado + clique liga/desliga.
 */
public class MagnetItem extends Item {

    public MagnetItem(Properties properties) {
        super(properties);
    }

    public static boolean active(ItemStack stack) {
        return !Boolean.FALSE.equals(stack.get(ModComponents.ACTIVE.get()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return active(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            boolean active = !active(stack);
            stack.set(ModComponents.ACTIVE.get(), active);
            player.displayClientMessage(Component.translatable(active ? "message.atomcraft.magnet_on" : "message.atomcraft.magnet_off")
                    .withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Puxa itens e orbes no raio da config em direção ao jogador. Devolve quantos foram puxados. */
    public static int pull(Player player) {
        double range = Config.MAGNET_RANGE.get();
        AABB area = player.getBoundingBox().inflate(range);
        Vec3 target = player.position().add(0, 0.6, 0);
        int pulled = 0;
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.hasPickUpDelay())) {
            pulled += move(item, target);
        }
        for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
            pulled += move(orb, target);
        }
        return pulled;
    }

    private static int move(net.minecraft.world.entity.Entity entity, Vec3 target) {
        Vec3 offset = target.subtract(entity.position());
        double distance = offset.length();
        if (distance < 0.8) return 0;
        entity.setDeltaMovement(offset.scale(Math.min(0.5, 0.15 + distance * 0.05) / distance));
        entity.hurtMarked = true;
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(active(stack) ? "tooltip.atomcraft.magnet_on" : "tooltip.atomcraft.magnet_off",
                Config.MAGNET_RANGE.get()).withStyle(active(stack) ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.atomcraft.magnet_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
