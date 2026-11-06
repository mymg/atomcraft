package com.atomcraft.client.render;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.FusionReactorBlock;
import com.atomcraft.block.MachineBlock;
import com.atomcraft.block.entity.FusionReactorBlockEntity;
import com.atomcraft.fusion.FusionStructure;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Desenha o modelo grande do Reator de Fusão (GeckoLib) quando a estrutura está formada.
 * <p>
 * Arquivos (editáveis no Blockbench, formato "GeckoLib Animated Model"):
 * <ul>
 *   <li>{@code assets/atomcraft/geckolib/models/block/fusion_reactor_formed.geo.json}</li>
 *   <li>{@code assets/atomcraft/geckolib/animations/block/fusion_reactor_formed.animation.json} (animação "plasma")</li>
 *   <li>{@code assets/atomcraft/textures/block/fusion_reactor_formed.png} (+ {@code _glowmask.png} para partes que brilham)</li>
 * </ul>
 * O ponto (0, 0, 0) do modelo é o centro da base da estrutura; a frente (lado do
 * controlador) fica para o norte (-Z) no Blockbench.
 */
public class FusionReactorRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<FusionReactorBlockEntity, R> {

    public FusionReactorRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new DefaultedBlockGeoModel<>(AtomCraft.id("fusion_reactor_formed")));
        withRenderLayer(AutoGlowingGeoLayer::new);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<R> renderPassInfo) {
        super.adjustRenderPose(renderPassInfo);
        // O controlador fica no meio da parede da frente, 2 blocos acima da base:
        // leva a origem do modelo para o centro da base da estrutura.
        PoseStack poseStack = renderPassInfo.poseStack();
        poseStack.translate(0, -FusionStructure.CONTROLLER_Y, FusionStructure.SIZE / 2);
    }

    @Override
    public void submit(R renderState, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraRenderState) {
        if (renderState.blockState.hasProperty(FusionReactorBlock.FORMED) && renderState.blockState.getValue(FusionReactorBlock.FORMED)) {
            super.submit(renderState, poseStack, renderTasks, cameraRenderState);
        }
    }

    @Override
    public AABB getRenderBoundingBox(FusionReactorBlockEntity blockEntity) {
        return FusionStructure.bounds(blockEntity.getBlockPos(), blockEntity.getBlockState().getValue(MachineBlock.FACING));
    }
}
