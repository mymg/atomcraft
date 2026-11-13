package com.atomcraft.client.render;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.entity.AnalyzerBlockEntity;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Analisador animado (GeckoLib). Arquivos:
 * {@code geckolib/models/block/analyzer.geo.json}, {@code geckolib/animations/block/analyzer.animation.json}
 * (animações "idle" e "analyzing") e {@code textures/block/analyzer.png}.
 */
public class AnalyzerRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<AnalyzerBlockEntity, R> {

    public AnalyzerRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new DefaultedBlockGeoModel<>(AtomCraft.id("analyzer")));
    }

    /** O modelo passa de um bloco de altura: não pode sumir quando só a parte de cima está na tela. */
    @Override
    public AABB getRenderBoundingBox(AnalyzerBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0, 1, 0).inflate(0.5);
    }
}
