package com.atomcraft.compat;

import com.atomcraft.client.render.GeoArmors;
import com.atomcraft.equipment.JetpackItem;
import com.atomcraft.equipment.ModEquipment;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Curios no cliente (só carregada com o Curios instalado): desenha o Jetpack quando está no slot de costas. */
public final class CuriosClient {

    private CuriosClient() {}

    public static void register() {
        ICurioRenderer.register(ModEquipment.JETPACK.get(), JetpackRenderer::new);
    }

    /** Usa o mesmo modelo GeckoLib do jetpack vestido no peito, posado pelo modelo do jogador. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static final class JetpackRenderer implements ICurioRenderer {
        private GeoArmors.Renderer renderer;

        @Override
        public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(ItemStack stack, SlotContext slotContext,
                PoseStack poseStack, SubmitNodeCollector collector, int light, S renderState, RenderLayerParent<S, M> parent,
                EntityRendererProvider.Context context, float yRot, float xRot) {
            if (!(stack.getItem() instanceof JetpackItem jetpack) || !(parent.getModel() instanceof HumanoidModel<?> model)) return;
            // Jetpack vestido no peito já é desenhado pela armadura
            if (slotContext.entity().getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof JetpackItem) return;
            if (renderer == null) renderer = new GeoArmors.Renderer("hydrogen_jetpack");
            HumanoidRenderState state = new HumanoidRenderState();
            state.lightCoords = light;
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            renderer.fillRenderState(jetpack, new com.geckolib.renderer.GeoArmorRenderer.RenderData(stack, EquipmentSlot.CHEST,
                    slotContext.entity(), model), state, partialTick);
            renderer.performRenderPass(state, poseStack, collector, Minecraft.getInstance().levelRenderer.levelRenderState.cameraRenderState);
        }
    }
}
