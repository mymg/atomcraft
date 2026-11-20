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
 * Interface da Matriz de Energia: por ela a energia entra (azul), sai (laranja) ou os dois (vermelho), e o
 * Cabo Atômico ligado a ela põe a matriz na rede. Só pode ficar no meio das faces da base e do topo.
 * Agachado + clique com a mão vazia troca o modo; clique normal abre a matriz.
 */
public class MatrixPortBlock extends MatrixPartBlock implements EntityBlock, com.atomcraft.storage.NetworkNode {

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<com.atomcraft.energy.PortMode> MODE =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("mode", com.atomcraft.energy.PortMode.class);

    public MatrixPortBlock(Properties properties) {
        super(MatrixPart.PORT, properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, com.atomcraft.energy.PortMode.BOTH).setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MODE);
    }

    /** A interface continua visível com a matriz formada: mostra onde ligar cabos e o modo (cor). */
    @Override
    protected boolean hiddenWhenFormed(BlockState state) {
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatrixPortBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            com.atomcraft.energy.PortMode mode = state.getValue(MODE).next();
            level.setBlock(pos, state.setValue(MODE, mode), Block.UPDATE_ALL);
            level.invalidateCapabilities(pos);
            player.sendOverlayMessage(Component.translatable("message.atomcraft.port_mode", mode.label().copy().withColor(mode.color())));
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MatrixPortBlockEntity port
                && port.controller() instanceof EnergyCellBlockEntity matrix) {
            serverPlayer.openMenu(matrix);
        }
        return InteractionResult.SUCCESS;
    }
}
