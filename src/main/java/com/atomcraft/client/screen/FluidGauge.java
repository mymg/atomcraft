package com.atomcraft.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Medidores de fluido das interfaces: a textura do fluido (com a cor dele) preenchendo a barra. */
public final class FluidGauge {

    private FluidGauge() {}

    public static @Nullable TextureAtlasSprite sprite(Fluid fluid) {
        if (fluid == Fluids.EMPTY) return null;
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        return model == null ? null : model.stillMaterial().sprite();
    }

    public static int tint(Fluid fluid, int amount) {
        if (fluid == Fluids.EMPTY) return 0xFFFFFFFF;
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        if (model == null || model.fluidTintSource() == null) return 0xFFFFFFFF;
        return model.fluidTintSource().colorAsStack(new FluidStack(fluid, Math.max(1, amount))) | 0xFF000000;
    }

    /** Barra vertical que enche de baixo para cima. */
    public static void vertical(GuiGraphicsExtractor g, int x, int y, int w, int h, Fluid fluid, int amount, int capacity) {
        g.fill(x, y, x + w, y + h, 0xFF0B1216);
        TextureAtlasSprite sprite = sprite(fluid);
        if (sprite == null || amount <= 0 || capacity <= 0) return;
        int filled = Math.max(1, Math.min(h, Math.round((float) h * amount / capacity)));
        int color = tint(fluid, amount);
        int bottom = y + h;
        for (int dy = 0; dy < filled; dy += 16) {
            int th = Math.min(16, filled - dy);
            for (int dx = 0; dx < w; dx += 16) {
                int tw = Math.min(16, w - dx);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + dx, bottom - dy - th, tw, th, color);
            }
        }
    }

    /** Barra horizontal que enche da esquerda para a direita. */
    public static void horizontal(GuiGraphicsExtractor g, int x, int y, int w, int h, Fluid fluid, int amount, int capacity) {
        g.fill(x, y, x + w, y + h, 0xFF0B1216);
        TextureAtlasSprite sprite = sprite(fluid);
        if (sprite == null || amount <= 0 || capacity <= 0) return;
        int filled = Math.max(1, Math.min(w, Math.round((float) w * amount / capacity)));
        int color = tint(fluid, amount);
        for (int dx = 0; dx < filled; dx += 16) {
            int tw = Math.min(16, filled - dx);
            for (int dy = 0; dy < h; dy += 16) {
                int th = Math.min(16, h - dy);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + dx, y + dy, tw, th, color);
            }
        }
    }

    public static List<Component> tooltip(Fluid fluid, int amount, int capacity) {
        List<Component> lines = new ArrayList<>();
        if (fluid == Fluids.EMPTY || amount <= 0) {
            lines.add(Component.translatable("gui.atomcraft.fluid_empty"));
        } else {
            lines.add(new FluidStack(fluid, amount).getHoverName());
        }
        lines.add(Component.literal(String.format("%,d / %,d mB", Math.max(0, amount), capacity)));
        return lines;
    }
}
