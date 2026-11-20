package com.atomcraft.client.render;

import com.atomcraft.client.FusionPreview;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Blocos fantasma da prévia das estruturas: em cada lugar onde falta uma peça, um contorno do bloco e
 * o próprio bloco em miniatura, pulsando. Lugares que precisam ficar livres (canal de plasma do reator)
 * aparecem só com o contorno vermelho.
 */
public final class Holograms {

    /** Peça a desenhar, com a posição relativa ao controlador. */
    public record Entry(int dx, int dy, int dz, ItemStackRenderState item) {}

    private static final VoxelShape BOX = Shapes.box(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
    private static final int FULL_BRIGHT = 0xF000F0;

    private Holograms() {}

    public static List<Entry> capture(ItemModelResolver resolver, Level level, BlockPos controller) {
        List<FusionPreview.Ghost> ghosts = FusionPreview.ghosts(controller);
        if (ghosts.isEmpty()) return List.of();
        List<Entry> entries = new ArrayList<>(ghosts.size());
        int seed = 0;
        for (FusionPreview.Ghost ghost : ghosts) {
            ItemStackRenderState state = new ItemStackRenderState();
            if (!ghost.stack().isEmpty()) resolver.updateForTopItem(state, ghost.stack(), ItemDisplayContext.NONE, level, null, seed++);
            BlockPos d = ghost.pos().subtract(controller);
            entries.add(new Entry(d.getX(), d.getY(), d.getZ(), state));
        }
        return entries;
    }

    public static void submit(List<Entry> entries, PoseStack poseStack, SubmitNodeCollector collector, long gameTime, float partialTick) {
        if (entries.isEmpty()) return;
        float time = gameTime + partialTick;
        float scale = 0.55f + 0.06f * (float) Math.sin(time * 0.15);
        for (Entry entry : entries) {
            poseStack.pushPose();
            poseStack.translate(entry.dx() + 0.5, entry.dy() + 0.5, entry.dz() + 0.5);
            boolean empty = entry.item().isEmpty();
            collector.submitShapeOutline(poseStack, BOX, RenderTypes.LINES, empty ? 0xFFFF4040 : 0xFF5FE8FF, 2.0f, false);
            if (!empty) {
                poseStack.scale(scale, scale, scale);
                entry.item().submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        }
    }
}
