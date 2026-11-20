package com.atomcraft.client.render;

import com.atomcraft.AtomCraft;
import com.atomcraft.block.EnergyMatrixBlock;
import com.atomcraft.block.MachineBlock;
import com.atomcraft.block.entity.EnergyCellBlockEntity;
import com.atomcraft.energy.MatrixStructure;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Matriz de Energia formada (GeckoLib) e, montando, os blocos fantasma da prévia.
 * <p>
 * Arquivos: {@code geckolib/models/block/energy_matrix.geo.json}, {@code textures/block/energy_matrix.png}
 * e {@code textures/block/energy_matrix_off.png} (a mesma textura com a bobina apagada, usada quando a
 * matriz está sem energia). O ponto (0, 0, 0) do modelo é o centro da base; a frente fica para o norte.
 */
public class EnergyMatrixRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<EnergyCellBlockEntity, R> {

    private static final DataTicket<Boolean> FORMED = DataTicket.create("atomcraft_matrix_formed", Boolean.class);
    private static final DataTicket<Boolean> CHARGED = DataTicket.create("atomcraft_matrix_charged", Boolean.class);
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final DataTicket<List<Holograms.Entry>> GHOSTS = DataTicket.create("atomcraft_matrix_ghosts", (Class) List.class);
    private static final DataTicket<Long> TIME = DataTicket.create("atomcraft_matrix_time", Long.class);

    private static final Identifier TEXTURE_ON = AtomCraft.id("textures/block/energy_matrix.png");
    private static final Identifier TEXTURE_OFF = AtomCraft.id("textures/block/energy_matrix_off.png");

    private final ItemModelResolver itemModelResolver;

    public EnergyMatrixRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new DefaultedBlockGeoModel<>(AtomCraft.id("energy_matrix")) {
            @Override
            public Identifier getTextureResource(GeoRenderState renderState) {
                return Boolean.TRUE.equals(renderState.getOrDefaultGeckolibData(CHARGED, (Boolean) true)) ? TEXTURE_ON : TEXTURE_OFF;
            }
        });
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<R> renderPassInfo) {
        super.adjustRenderPose(renderPassInfo);
        // O controlador é a frente da base: a origem do modelo (centro da base) fica 1 bloco para trás.
        renderPassInfo.poseStack().translate(0, 0, 1);
    }

    /** Translúcido, para o vidro da textura aparecer como no Blockbench. */
    @Override
    public @Nullable RenderType getRenderType(R renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public void captureDefaultRenderState(EnergyCellBlockEntity animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, relatedObject, renderState, partialTick);
        BlockState state = animatable.getBlockState();
        boolean formed = state.hasProperty(EnergyMatrixBlock.FORMED) && state.getValue(EnergyMatrixBlock.FORMED);
        renderState.addGeckolibData(FORMED, formed);
        renderState.addGeckolibData(CHARGED, state.hasProperty(EnergyMatrixBlock.CHARGED) && state.getValue(EnergyMatrixBlock.CHARGED));
        if (!formed && animatable.getLevel() != null) {
            renderState.addGeckolibData(GHOSTS, Holograms.capture(itemModelResolver, animatable.getLevel(), animatable.getBlockPos()));
            renderState.addGeckolibData(TIME, animatable.getLevel().getGameTime());
        }
    }

    @Override
    public void submit(R renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (Boolean.TRUE.equals(renderState.getOrDefaultGeckolibData(FORMED, false))) {
            super.submit(renderState, poseStack, collector, camera);
            return;
        }
        List<Holograms.Entry> ghosts = renderState.getOrDefaultGeckolibData(GHOSTS, (List<Holograms.Entry>) null);
        if (ghosts != null) Holograms.submit(ghosts, poseStack, collector, renderState.getOrDefaultGeckolibData(TIME, (Long) 0L), 0);
    }

    @Override
    public AABB getRenderBoundingBox(EnergyCellBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        return state.hasProperty(MachineBlock.FACING) ? MatrixStructure.bounds(blockEntity.getBlockPos(), state.getValue(MachineBlock.FACING))
                : new AABB(blockEntity.getBlockPos());
    }
}
