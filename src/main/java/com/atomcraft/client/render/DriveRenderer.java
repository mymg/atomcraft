package com.atomcraft.client.render;

import com.atomcraft.block.MachineBlock;
import com.atomcraft.block.entity.DriveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Desenha os discos inseridos nos slots do Drive, usando o próprio modelo do disco
 * (cada nível com a sua textura), como as células do Applied Energistics.
 * <p>
 * As posições seguem os elementos "drive" do modelo {@code models/block/drive.json}:
 * duas colunas (x 9..15 e 1..7) e cinco linhas (y 13, 10, 7, 4, 1), com o disco
 * de z 1 a 10. O modelo do disco ({@code models/item/disk.json}) fica em x 5..11,
 * y 0..2, z 0..9 com a etiqueta para o norte.
 */
public class DriveRenderer implements BlockEntityRenderer<DriveBlockEntity, DriveRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState[] disks = new ItemStackRenderState[DriveBlockEntity.SLOTS];
        public Direction facing = Direction.NORTH;

        public State() {
            for (int i = 0; i < disks.length; i++) disks[i] = new ItemStackRenderState();
        }
    }

    private final ItemModelResolver itemModelResolver;

    public DriveRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DriveBlockEntity drive, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(drive, state, partialTick, cameraPos, breakProgress);
        state.facing = drive.getBlockState().hasProperty(MachineBlock.FACING) ? drive.getBlockState().getValue(MachineBlock.FACING) : Direction.NORTH;
        for (int i = 0; i < DriveBlockEntity.SLOTS; i++) {
            ItemStack disk = drive.getDisk(i);
            state.disks[i].clear();
            if (!disk.isEmpty()) {
                itemModelResolver.updateForTopItem(state.disks[i], disk, ItemDisplayContext.NONE, drive.getLevel(), null, i);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < DriveBlockEntity.SLOTS; i++) {
            if (state.disks[i].isEmpty()) continue;
            int column = i % 2, row = i / 2;
            // Deslocamento (em pixels) do modelo do disco até o slot.
            float dx = column == 0 ? 4 : -4, dy = 13 - 3 * row, dz = 1;
            poseStack.pushPose();
            poseStack.translate(0.5, 0, 0.5);
            switch (state.facing) {
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YN.rotationDegrees(90));
                default -> {}
            }
            poseStack.translate(-0.5, 0, -0.5);
            poseStack.translate(dx / 16f + 0.5f, dy / 16f + 0.5f, dz / 16f + 0.5f);
            state.disks[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
