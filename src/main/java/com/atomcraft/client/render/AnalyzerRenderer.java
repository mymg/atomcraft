package com.atomcraft.client.render;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Analisador animado (GeckoLib). Arquivos:
 * {@code geckolib/models/block/analyzer.geo.json}, {@code geckolib/animations/block/analyzer.animation.json}
 * (animações "idle" e "analyzing") e {@code textures/block/analyzer.png}.
 * <p>
 * Enquanto analisa, o item da vez gira dentro da câmara, em cima da plataforma
 * (y 18 px do modelo), no centro do bloco.
 */
public class AnalyzerRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<AnalyzerBlockEntity, R> {

    /** Altura (em pixels) do centro do item: a plataforma da câmara fica em y 18. */
    private static final float ITEM_Y = 21.5f;
    private static final float ITEM_SCALE = 0.55f;

    private static final DataTicket<ItemStackRenderState> ITEM = DataTicket.create("atomcraft_analyzer_item", ItemStackRenderState.class);
    private static final DataTicket<Float> SPIN = DataTicket.create("atomcraft_analyzer_spin", Float.class);

    private final ItemModelResolver itemModelResolver;

    public AnalyzerRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new DefaultedBlockGeoModel<>(AtomCraft.id("analyzer")));
        this.itemModelResolver = context.itemModelResolver();
    }

    /** Translúcido, para o vidro semitransparente da textura aparecer como no Blockbench. */
    @Override
    public @Nullable RenderType getRenderType(R renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public void captureDefaultRenderState(AnalyzerBlockEntity analyzer, @Nullable Void relatedObject, R renderState, float partialTick) {
        super.captureDefaultRenderState(analyzer, relatedObject, renderState, partialTick);
        Item item = analyzer.current();
        if (item == null || analyzer.getLevel() == null) return;
        ItemStackRenderState itemState = new ItemStackRenderState();
        itemModelResolver.updateForTopItem(itemState, new ItemStack(item), ItemDisplayContext.FIXED, analyzer.getLevel(), null,
                (int) analyzer.getBlockPos().asLong());
        if (itemState.isEmpty()) return;
        renderState.addGeckolibData(ITEM, itemState);
        renderState.addGeckolibData(SPIN, ((analyzer.getLevel().getGameTime() % 360) + partialTick) * 3f);
    }

    @Override
    public void submit(R renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(renderState, poseStack, collector, camera);
        ItemStackRenderState item = renderState.getOrDefaultGeckolibData(ITEM, null);
        if (item == null) return;
        float spin = renderState.getOrDefaultGeckolibData(SPIN, 0f);
        poseStack.pushPose();
        poseStack.translate(0.5, ITEM_Y / 16f + Math.sin(Math.toRadians(spin * 2)) * 0.02, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        item.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** O modelo ocupa 2 blocos de altura: não pode sumir quando só a parte de cima está na tela. */
    @Override
    public AABB getRenderBoundingBox(AnalyzerBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0, 1.25, 0).inflate(0.25);
    }
}
