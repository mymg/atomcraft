package com.atomcraft.block;

import com.atomcraft.AtomCraft;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Item das máquinas de 2 blocos (Analisador, Atomizador, Replicador): usa o mesmo modelo
 * animado (GeckoLib) do bloco, com a animação "idle". O tamanho em cada lugar (inventário,
 * mão, chão, moldura) vem do bloco "display" de {@code models/item/<máquina>.json},
 * reduzido porque o modelo tem 2 blocos de altura.
 */
public class TallMachineItem extends BlockItem implements GeoItem {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final String model;

    public TallMachineItem(Block block, Properties properties, String model) {
        super(block, properties);
        this.model = model;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private @Nullable GeoItemRenderer<TallMachineItem> renderer;

            @Override
            public @Nullable GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new GeoItemRenderer<>(new DefaultedBlockGeoModel<TallMachineItem>(AtomCraft.id(model))) {
                    // Vidro semitransparente, como no bloco.
                    @Override
                    public @Nullable RenderType getRenderType(GeoRenderState renderState, Identifier texture) {
                        return RenderTypes.entityTranslucent(texture);
                    }
                };
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<TallMachineItem>("main", 0, test -> test.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
