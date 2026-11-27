package com.atomcraft.client.render;

import com.atomcraft.AtomCraft;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Armaduras desenhadas com modelos do GeckoLib (Blockbench, tipo "Armor"). Arquivos de cada modelo:
 * {@code geckolib/models/item/<nome>.geo.json} e {@code textures/item/armor/<nome>.png}.
 * <p>
 * Bones usados (padrão do GeckoLib): armorHead, armorBody, armorRightArm, armorLeftArm, armorRightLeg,
 * armorLeftLeg, armorRightBoot e armorLeftBoot. Extra: {@code armorWaist} (cintura das calças), que acompanha
 * o tronco mas só aparece com as calças vestidas.
 */
public final class GeoArmors {

    private GeoArmors() {}

    /** Provider para {@code GeoItem#createGeoRenderer}: um renderizador por item, criado na primeira vez. */
    public static <T extends Item & GeoItem> GeoRenderProvider provider(String model) {
        return new GeoRenderProvider() {
            private @Nullable Renderer<T, ?> renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(@Nullable ItemStack stack, EquipmentSlot slot) {
                if (renderer == null) renderer = new Renderer<>(model);
                return renderer;
            }
        };
    }

    public static class Renderer<T extends Item & GeoItem, R extends HumanoidRenderState & GeoRenderState> extends GeoArmorRenderer<T, R> {

        public static final String WAIST = "armorWaist";

        public Renderer(String model) {
            super(new DefaultedItemGeoModel<T>(AtomCraft.id(model)).withAltTexture(AtomCraft.id("armor/" + model)));
        }

        /** As calças também mostram a cintura (bone armorWaist), presa ao tronco. */
        @Override
        public List<ArmorSegment> getSegmentsForSlot(R renderState, EquipmentSlot slot) {
            List<ArmorSegment> segments = super.getSegmentsForSlot(renderState, slot);
            if (slot != EquipmentSlot.LEGS || segments.contains(ArmorSegment.CHEST)) return segments;
            List<ArmorSegment> withWaist = new ArrayList<>(segments);
            withWaist.add(ArmorSegment.CHEST);
            return withWaist;
        }

        @Override
        public String getBoneNameForSegment(R renderState, ArmorSegment segment) {
            if (segment == ArmorSegment.CHEST && renderState.getGeckolibData(CURRENT_SLOT) == EquipmentSlot.LEGS) return WAIST;
            return super.getBoneNameForSegment(renderState, segment);
        }
    }
}
