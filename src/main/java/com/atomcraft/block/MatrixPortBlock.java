package com.atomcraft.block;

import com.atomcraft.block.entity.EnergyCellBlockEntity;
import com.atomcraft.block.entity.MatrixPortBlockEntity;
import com.atomcraft.energy.MatrixPart;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Porta da Matriz de Energia: por ela a energia entra (azul) ou sai (laranja) da matriz, e o
 * Cabo Atômico ligado a ela põe a matriz na rede.
 * Agachado + clique com a mão vazia alterna entre entrada e saída; clique normal abre a matriz.
 */
public class MatrixPortBlock extends MatrixPartBlock implements EntityBlock, com.atomcraft.storage.NetworkNode {

    public static final BooleanProperty OUTPUT = BooleanProperty.create("output");

    public MatrixPortBlock(Properties properties) {
        super(MatrixPart.PORT, properties);
        registerDefaultState(stateDefinition.any().setValue(OUTPUT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OUTPUT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatrixPortBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            boolean output = !state.getValue(OUTPUT);
            level.setBlock(pos, state.setValue(OUTPUT, output), Block.UPDATE_ALL);
            level.invalidateCapabilities(pos);
            player.sendSystemMessage(Component.translatable(output ? "message.atomcraft.port_output" : "message.atomcraft.port_input"));
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MatrixPortBlockEntity port
                && port.controller() instanceof EnergyCellBlockEntity matrix) {
            serverPlayer.openMenu(matrix);
        }
        return InteractionResult.SUCCESS;
    }
}
