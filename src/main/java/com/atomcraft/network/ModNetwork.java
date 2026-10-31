package com.atomcraft.network;

import com.atomcraft.block.entity.ReplicatorBlockEntity;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.client.ClientState;
import com.atomcraft.research.ResearchData;
import net.minecraft.server.level.ServerPlayer;
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
                (payload, context) -> ClientState.setResearch(payload.progress()));
        registrar.playToClient(NetworkContentsPayload.TYPE, NetworkContentsPayload.STREAM_CODEC,
                (payload, context) -> ClientState.setNetwork(payload.totals(), payload.capacity(), payload.used()));
        registrar.playToServer(ReplicatePayload.TYPE, ReplicatePayload.STREAM_CODEC, ModNetwork::handleReplicate);
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
        PacketDistributor.sendToPlayer(player, new ResearchSyncPayload(research.snapshot(player.getUUID())));
    }

    private ModNetwork() {}
}
