package com.atomcraft.client.render;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.entity.MachineBlockEntity;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Máquinas de 2 blocos com modelo animado (GeckoLib). Para a máquina {@code X}:
 * {@code geckolib/models/block/X.geo.json}, {@code geckolib/animations/block/X.animation.json}
 * (animações "idle" e "working"; o Analisador usa "analyzing") e {@code textures/block/X.png}.
 */
public class TallMachineRenderer<T extends MachineBlockEntity & GeoBlockEntity, R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<T, R> {

    public TallMachineRenderer(BlockEntityRendererProvider.Context context, String model) {
        super(context, new DefaultedBlockGeoModel<>(AtomCraft.id(model)));
    }

    /** Translúcido, para as partes semitransparentes da textura (vidro) aparecerem como no Blockbench. */
    @Override
    public @Nullable RenderType getRenderType(R renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    /** O modelo ocupa 2 blocos de altura: não pode sumir quando só a parte de cima está na tela. */
    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0, 1.25, 0).inflate(0.25);
    }
}
