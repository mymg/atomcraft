package com.atomcraft.client.render;

import com.atomcraft.client.screen.FluidGauge;
import com.atomcraft.fluid.TankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Desenha o fluido dentro do Tanque de Fluidos, na altura proporcional ao que ele guarda. */
public class TankRenderer implements BlockEntityRenderer<TankBlockEntity, TankRenderer.State> {

    private static final float MIN = 2.01f / 16f, MAX = 13.99f / 16f, BOTTOM = 1.01f / 16f, TOP = 14.99f / 16f;
    private static final int FULL_BRIGHT = 0xF000F0;

    public static class State extends BlockEntityRenderState {
        public Fluid fluid = Fluids.EMPTY;
        public float fill;
        public boolean glowing;
    }

    public TankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TankBlockEntity tank, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTick, cameraPos, breakProgress);
        state.fluid = tank.tank().fluid();
        state.fill = tank.tank().capacity() <= 0 ? 0 : Math.min(1f, (float) tank.tank().amount() / tank.tank().capacity());
        state.glowing = state.fluid != Fluids.EMPTY && state.fluid.getFluidType().getLightLevel() > 0;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fluid == Fluids.EMPTY || state.fill <= 0) return;
        TextureAtlasSprite sprite = FluidGauge.sprite(state.fluid);
        if (sprite == null) return;
        int color = FluidGauge.tint(state.fluid, 1000);
        int light = state.glowing ? FULL_BRIGHT : state.lightCoords;
        float top = BOTTOM + (TOP - BOTTOM) * Math.max(0.02f, state.fill);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(sprite.atlasLocation()), (pose, consumer) -> {
            // Topo
            quad(consumer, pose, sprite, color, light, 0, 1, 0,
                    MIN, top, MIN, MIN, top, MAX, MAX, top, MAX, MAX, top, MIN, 0, 1, 0, 1);
            float v1 = (top - BOTTOM) / (TOP - BOTTOM);
            // Laterais
            quad(consumer, pose, sprite, color, light, 0, 0, -1,
                    MAX, BOTTOM, MIN, MIN, BOTTOM, MIN, MIN, top, MIN, MAX, top, MIN, 0, 1, 0, v1);
            quad(consumer, pose, sprite, color, light, 0, 0, 1,
                    MIN, BOTTOM, MAX, MAX, BOTTOM, MAX, MAX, top, MAX, MIN, top, MAX, 0, 1, 0, v1);
            quad(consumer, pose, sprite, color, light, -1, 0, 0,
                    MIN, BOTTOM, MIN, MIN, BOTTOM, MAX, MIN, top, MAX, MIN, top, MIN, 0, 1, 0, v1);
            quad(consumer, pose, sprite, color, light, 1, 0, 0,
                    MAX, BOTTOM, MAX, MAX, BOTTOM, MIN, MAX, top, MIN, MAX, top, MAX, 0, 1, 0, v1);
        });
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, int color, int light,
                             float nx, float ny, float nz,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float u0, float u1, float v0, float v1) {
        float su0 = sprite.getU(u0), su1 = sprite.getU(u1), sv0 = sprite.getV(1 - v0), sv1 = sprite.getV(1 - v1);
        vertex(consumer, pose, x1, y1, z1, su0, sv0, color, light, nx, ny, nz);
        vertex(consumer, pose, x2, y2, z2, su1, sv0, color, light, nx, ny, nz);
        vertex(consumer, pose, x3, y3, z3, su1, sv1, color, light, nx, ny, nz);
        vertex(consumer, pose, x4, y4, z4, su0, sv1, color, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int color, int light, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
