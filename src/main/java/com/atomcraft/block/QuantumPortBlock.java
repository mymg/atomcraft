package com.atomcraft.block;

import com.atomcraft.block.entity.QuantumPortBlockEntity;
import com.atomcraft.replication.QuantumPart;
import com.atomcraft.storage.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Porta Quântica: fica no lugar de uma carcaça ou vidro do Replicador Quântico e liga o
 * multibloco à rede (Cabo Atômico), à energia e a canos de itens (saída dos itens replicados).
 * Clique abre a interface do replicador.
 */
public class QuantumPortBlock extends QuantumPartBlock implements EntityBlock, NetworkNode {

    public QuantumPortBlock(Properties properties) {
        super(QuantumPart.PORT, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumPortBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof QuantumPortBlockEntity port
                && port.controller() != null) {
            serverPlayer.openMenu(port.controller());
        }
        return InteractionResult.SUCCESS;
    }
}
