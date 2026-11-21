package com.atomcraft.client;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.client.screen.TechGui;
import com.atomcraft.equipment.EquipmentEvents;
import com.atomcraft.equipment.EquipmentModule;
import com.atomcraft.equipment.JetpackItem;
import com.atomcraft.equipment.ModularItem;
import com.atomcraft.equipment.Worn;
import com.atomcraft.network.JetpackPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Lado do cliente dos equipamentos: voo do Jetpack (o movimento do jogador é calculado no cliente),
 * tecla de flutuação e o painel do Anel Analisador (composição do bloco ou item mirado).
 */
public final class EquipmentClient {

    public static final KeyMapping HOVER_KEY = new KeyMapping("key.atomcraft.jetpack_hover", 72 /* H */, KeyMapping.Category.MOVEMENT);

    private static final double SCAN_RANGE = 16.0;

    private static int lastState;
    private static boolean analyzer;

    private EquipmentClient() {}

    static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(HOVER_KEY));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(AtomCraft.id("analyzer_hud"), EquipmentClient::renderHud));
        NeoForge.EVENT_BUS.addListener(EquipmentClient::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            lastState = EquipmentEvents.JET_OFF;
            analyzer = false;
            return;
        }
        while (HOVER_KEY.consumeClick()) ClientPacketDistributor.sendToServer(new JetpackPayload(0, true));
        analyzer = player.tickCount % 5 == 0 ? Worn.hasAnalyzer(player) : analyzer;

        int state = EquipmentEvents.JET_OFF;
        ItemStack jetpack = Worn.jetpack(player);
        if (!jetpack.isEmpty() && JetpackItem.fuel(jetpack) > 0 && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isPassenger() && !player.isSpectator()) {
            Vec3 motion = player.getDeltaMovement();
            int thrusters = ModularItem.level(jetpack, EquipmentModule.THRUSTER);
            if (mc.options.keyJump.isDown() && mc.gui.screen() == null) {
                state = EquipmentEvents.JET_THRUST;
                // Acelera até a velocidade máxima de subida, sem nunca frear quem já sobe mais rápido (ex.: o pulo)
                double max = 0.4 + 0.12 * thrusters;
                double y = motion.y < max ? Math.min(motion.y + 0.14 + 0.03 * thrusters, max) : motion.y;
                player.setDeltaMovement(motion.x, y, motion.z);
                player.resetFallDistance();
                exhaust(player);
            }
        }
        if (state != lastState) {
            lastState = state;
            ClientPacketDistributor.sendToServer(new JetpackPayload(state, false));
        }
    }

    private static void exhaust(LocalPlayer player) {
        double yaw = Math.toRadians(player.yBodyRot);
        double x = player.getX() + Math.sin(yaw) * 0.35, z = player.getZ() - Math.cos(yaw) * 0.35;
        for (int side = -1; side <= 1; side += 2) {
            double ox = Math.cos(yaw) * 0.15 * side, oz = Math.sin(yaw) * 0.15 * side;
            player.level().addParticle(ParticleTypes.CLOUD, x + ox, player.getY() + 0.7, z + oz, 0, -0.15, 0);
        }
    }

    // ---------------------------------------------------------------- Anel Analisador

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!analyzer || mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        Item item = null;
        // Itens no chão ao alcance normal; blocos até 16 blocos de distância (o anel "escaneia" de longe).
        if (mc.hitResult instanceof EntityHitResult entity && entity.getEntity() instanceof ItemEntity dropped) {
            item = dropped.getItem().getItem();
        } else if (mc.player.pick(SCAN_RANGE, 1.0f, false) instanceof BlockHitResult block
                && block.getType() == HitResult.Type.BLOCK) {
            item = mc.level.getBlockState(block.getBlockPos()).getBlock().asItem();
        }
        if (item == null || item == Items.AIR) return;
        AtomicValue value = AtomicValues.get(item, true);
        Font font = mc.font;
        String name = new ItemStack(item).getHoverName().getString();
        String formula = value == null ? Component.translatable("hud.atomcraft.no_composition").getString()
                : ClientState.formula(value.composition());
        formula = font.plainSubstrByWidth(formula, 220);
        int width = Math.max(font.width(name), font.width(formula)) + 10;
        int x = mc.getWindow().getGuiScaledWidth() / 2 + 12, y = mc.getWindow().getGuiScaledHeight() / 2 + 10;
        TechGui.plainGlass(graphics, x, y, width, 24);
        graphics.text(font, name, x + 5, y + 3, TechGui.TEXT_LIGHT, false);
        graphics.text(font, formula, x + 5, y + 13, value == null ? TechGui.TEXT_DIM : TechGui.TEXT_TEAL, false);
    }
}
