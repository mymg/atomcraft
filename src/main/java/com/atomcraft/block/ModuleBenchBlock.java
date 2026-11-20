package com.atomcraft.block;

import com.atomcraft.menu.ModuleBenchMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Bancada de Módulos: instala e remove módulos dos equipamentos (exoesqueleto, jetpack, desintegrador). */
public class ModuleBenchBlock extends Block {

    public static final MapCodec<ModuleBenchBlock> CODEC = simpleCodec(ModuleBenchBlock::new);

    public ModuleBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) open(serverPlayer, pos);
        return InteractionResult.SUCCESS;
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.atomcraft.module_bench");
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new ModuleBenchMenu(id, inventory, pos);
            }

            @Override
            public void writeClientSideData(AbstractContainerMenu menu, net.minecraft.network.RegistryFriendlyByteBuf buffer) {
                buffer.writeBlockPos(pos);
            }
        });
    }
}
