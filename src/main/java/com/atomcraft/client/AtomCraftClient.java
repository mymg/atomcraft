package com.atomcraft.client;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.client.screen.AnalyzerScreen;
import com.atomcraft.client.screen.AtomizerScreen;
import com.atomcraft.client.screen.DriveScreen;
import com.atomcraft.client.screen.GuiUtil;
import com.atomcraft.client.screen.ReplicatorScreen;
import com.atomcraft.registry.ModMenus;
import com.atomcraft.storage.ElementDiskItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
        NeoForge.EVENT_BUS.addListener(AtomCraftClient::onTooltip);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ATOMIZER.get(), AtomizerScreen::new);
        event.register(ModMenus.ANALYZER.get(), AnalyzerScreen::new);
        event.register(ModMenus.REPLICATOR.get(), ReplicatorScreen::new);
        event.register(ModMenus.DRIVE.get(), DriveScreen::new);
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
                tooltip.add(Component.literal(GuiUtil.ellipsize(contents.formula(), 48)).withStyle(ChatFormatting.DARK_AQUA));
            }
            return;
        }

        AtomicValue value = AtomicValues.get(stack.getItem(), true);
        if (value == null) return;
        tooltip.add(Component.literal("⚛ " + GuiUtil.ellipsize(value.composition().formula(), 40))
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
    }
}
