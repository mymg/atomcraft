package com.atomcraft.client;

import com.atomcraft.client.screen.PartitionScreen;
import com.atomcraft.storage.DiskPartition;
import com.atomcraft.client.render.DriveRenderer;
import com.atomcraft.client.render.AnalyzerRenderer;
import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.chemistry.Fuel;
import com.atomcraft.client.screen.AnalyzerScreen;
import com.atomcraft.client.screen.AtomizerScreen;
import com.atomcraft.client.screen.DriveScreen;
import com.atomcraft.client.screen.GeneratorScreen;
import com.atomcraft.client.screen.SynthesizerScreen;
import com.atomcraft.client.screen.TerminalScreen;
import com.atomcraft.client.screen.GuiUtil;
import com.atomcraft.client.screen.ReplicatorScreen;
import com.atomcraft.registry.ModMenus;
import com.atomcraft.storage.ElementDiskItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.atomcraft.client.render.FusionReactorRenderer;
import com.atomcraft.registry.ModBlockEntities;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@Mod(value = AtomCraft.MOD_ID, dist = Dist.CLIENT)
public class AtomCraftClient {

    public AtomCraftClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(AtomCraftClient::registerScreens);
        modBus.addListener(AtomCraftClient::registerRenderers);
        FusionPreview.register();
        ElementDiskItem.openPartitionScreen = hand -> Minecraft.getInstance().gui.setScreen(new PartitionScreen(hand));
        NeoForge.EVENT_BUS.addListener(AtomCraftClient::onTooltip);
        ClientShowcase.register();
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FUSION_REACTOR.get(), FusionReactorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ANALYZER.get(), AnalyzerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DRIVE.get(), DriveRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ATOMIZER.get(), AtomizerScreen::new);
        event.register(ModMenus.ANALYZER.get(), AnalyzerScreen::new);
        event.register(ModMenus.REPLICATOR.get(), ReplicatorScreen::new);
        event.register(ModMenus.DRIVE.get(), DriveScreen::new);
        event.register(ModMenus.COMBUSTION_GENERATOR.get(), GeneratorScreen::new);
        event.register(ModMenus.CARBON_GENERATOR.get(), GeneratorScreen::new);
        event.register(ModMenus.FUSION_REACTOR.get(), GeneratorScreen::new);
        event.register(ModMenus.TERMINAL.get(), TerminalScreen::new);
        event.register(ModMenus.SYNTHESIZER.get(), SynthesizerScreen::new);
    }

    /** Mostra a fórmula, a raridade e o progresso de pesquisa no tooltip de cada item. */
    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (stack.getItem() instanceof ElementDiskItem disk) {
            Composition contents = ElementDiskItem.contents(stack);
            tooltip.add(Component.translatable("tooltip.atomcraft.disk_usage",
                    GuiUtil.formatAmount(contents.totalAtoms()), GuiUtil.formatAmount(disk.capacity())).withStyle(ChatFormatting.GRAY));
            if (!contents.isEmpty()) {
                tooltip.add(Component.literal(GuiUtil.ellipsize(ClientState.formula(contents), 48)).withStyle(ChatFormatting.DARK_AQUA));
            }
            DiskPartition partition = ElementDiskItem.partition(stack);
            if (!partition.isEmpty()) {
                StringBuilder text = new StringBuilder();
                partition.limits().forEach((element, limit) -> {
                    if (!text.isEmpty()) text.append(", ");
                    text.append(ClientState.symbol(element));
                    if (limit > 0) text.append(" ≤").append(GuiUtil.formatAmount(limit));
                });
                tooltip.add(Component.translatable("tooltip.atomcraft.partition", GuiUtil.ellipsize(text.toString(), 44))
                        .withStyle(ChatFormatting.GOLD));
            }
            tooltip.add(Component.translatable("tooltip.atomcraft.partition_hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        AtomicValue value = AtomicValues.get(stack.getItem(), true);
        if (value == null) return;
        tooltip.add(Component.literal("⚛ " + GuiUtil.ellipsize(ClientState.formula(value.composition()), 40))
                .withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(value.rarity().translationKey()).withStyle(value.rarity().color())));
        int required = value.researchRequired(stack.getItem());
        int progress = Math.min(required, ClientState.researchProgress(stack.getItem()));
        if (progress >= required) {
            tooltip.add(Component.translatable("tooltip.atomcraft.researched").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.atomcraft.research_progress", progress, required).withStyle(ChatFormatting.GRAY));
        }
        // Com o Gerador de Combustão aberto, mostra quanta energia o item rende.
        if (Minecraft.getInstance().gui.screen() instanceof GeneratorScreen) {
            long fuel = Fuel.energy(stack.getItem(), true);
            if (fuel > 0) {
                tooltip.add(Component.translatable("tooltip.atomcraft.fuel_value", GuiUtil.formatAmount(fuel)).withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
