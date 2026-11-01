package com.atomcraft.network;

import com.atomcraft.block.entity.MachineBlockEntity;
import com.atomcraft.block.entity.ReplicatorBlockEntity;
import com.atomcraft.block.entity.SynthesizerBlockEntity;
import com.atomcraft.menu.BaseMenu;
import net.minecraft.core.BlockPos;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.client.ClientState;
import com.atomcraft.research.ResearchData;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ValuesSyncPayload.TYPE, ValuesSyncPayload.STREAM_CODEC,
                (payload, context) -> AtomicValues.setClientValues(payload.values()));
        registrar.playToClient(ResearchSyncPayload.TYPE, ResearchSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientState.setResearch(payload.progress(), payload.discovered()));
        registrar.playToClient(NetworkContentsPayload.TYPE, NetworkContentsPayload.STREAM_CODEC,
                (payload, context) -> ClientState.setNetwork(payload.totals(), payload.capacity(), payload.used()));
        registrar.playToServer(ReplicatePayload.TYPE, ReplicatePayload.STREAM_CODEC, ModNetwork::handleReplicate);
        registrar.playToServer(MachineTogglePayload.TYPE, MachineTogglePayload.STREAM_CODEC, ModNetwork::handleToggle);
        registrar.playToServer(SynthesizePayload.TYPE, SynthesizePayload.STREAM_CODEC, ModNetwork::handleSynthesize);
        registrar.playToClient(MachineInfoPayload.TYPE, MachineInfoPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof BaseMenu menu && menu.containerId == payload.containerId()) {
                menu.setOwnerName(payload.ownerName());
            }
        });
    }

    private static boolean inReach(ServerPlayer player, BlockPos pos) {
        return player.level().isLoaded(pos)
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    private static void handleToggle(MachineTogglePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !inReach(player, payload.pos())) return;
        if (player.level().getBlockEntity(payload.pos()) instanceof MachineBlockEntity machine && machine.hasPowerControl()) {
            machine.setEnabled(!machine.isEnabled());
        }
    }

    private static void handleSynthesize(SynthesizePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !inReach(player, payload.pos())) return;
        if (player.level().getBlockEntity(payload.pos()) instanceof SynthesizerBlockEntity synthesizer) {
            synthesizer.select(payload.item());
        }
    }

    private static void handleReplicate(ReplicatePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        var level = player.level();
        if (!level.isLoaded(payload.pos())) return;
        if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) > 64) return;
        if (level.getBlockEntity(payload.pos()) instanceof ReplicatorBlockEntity replicator) {
            if (payload.amount() <= 0) replicator.cancelJob();
            else replicator.startJob(player, payload.item(), payload.amount());
        }
    }

    /** Envia valores atômicos e pesquisa ao jogador (ao entrar e após /reload). */
    public static void syncAll(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ValuesSyncPayload(AtomicValues.serverValues()));
        syncResearch(player);
    }

    public static void syncResearch(ServerPlayer player) {
        var research = ResearchData.get(player.level().getServer());
        PacketDistributor.sendToPlayer(player, new ResearchSyncPayload(research.snapshot(player.getUUID()),
                List.copyOf(research.discovered(player.getUUID()))));
    }

    private ModNetwork() {}
}
