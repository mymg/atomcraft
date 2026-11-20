package com.atomcraft.client.render;

import com.atomcraft.block.MachineBlock;
import com.atomcraft.block.entity.ReplicatorBlockEntity;
import com.atomcraft.replication.QuantumStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Replicador Quântico: blocos fantasma da prévia da estrutura (o modelo formado entra quando existir). */
public class QuantumReplicatorRenderer implements BlockEntityRenderer<ReplicatorBlockEntity, QuantumReplicatorRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public List<Holograms.Entry> ghosts = List.of();
        public long time;
        public float partialTick;
    }

    private final ItemModelResolver itemModelResolver;

    public QuantumReplicatorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ReplicatorBlockEntity replicator, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(replicator, state, partialTick, cameraPos, breakProgress);
        state.ghosts = replicator.getLevel() == null ? List.of() : Holograms.capture(itemModelResolver, replicator.getLevel(), replicator.getBlockPos());
        state.time = replicator.getLevel() == null ? 0 : replicator.getLevel().getGameTime();
        state.partialTick = partialTick;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Holograms.submit(state.ghosts, poseStack, collector, state.time, state.partialTick);
    }

    @Override
    public AABB getRenderBoundingBox(ReplicatorBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        return state.hasProperty(MachineBlock.FACING) ? QuantumStructure.bounds(blockEntity.getBlockPos(), state.getValue(MachineBlock.FACING))
                : new AABB(blockEntity.getBlockPos());
    }
}
