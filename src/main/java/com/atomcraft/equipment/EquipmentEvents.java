package com.atomcraft.equipment;

import com.atomcraft.AtomCraft;
import com.atomcraft.Config;
import com.atomcraft.block.entity.WirelessTransmitterBlockEntity;
import com.atomcraft.registry.ModComponents;
import com.atomcraft.wireless.Wireless;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lado do servidor dos equipamentos: efeitos dos módulos do Exoesqueleto, escudo de energia,
 * consumo do Jetpack, Anel Magnético e recarga sem fio (energia e hidrogênio).
 */
public final class EquipmentEvents {

    /** Estado do jetpack enviado pelo cliente: 0 parado, 1 subindo, 2 flutuando. */
    public static final int JET_OFF = 0, JET_THRUST = 1, JET_HOVER = 2;

    private static final Map<UUID, Integer> JET_STATE = new HashMap<>();
    private static final Map<UUID, Integer> JET_PENDING = new HashMap<>();
    /** Jogadores que receberam o voo do módulo de Voo do jetpack (para tirar quando acabar). */
    private static final java.util.Set<UUID> FLIGHT = new java.util.HashSet<>();

    private static final Identifier SPEED = AtomCraft.id("exo_speed");
    private static final Identifier STEP = AtomCraft.id("exo_step");
    private static final Identifier JUMP = AtomCraft.id("exo_jump");
    private static final Identifier FALL = AtomCraft.id("exo_fall");
    private static final Identifier CHEST_ARMOR = AtomCraft.id("exo_plating_chest");
    private static final Identifier CHEST_TOUGHNESS = AtomCraft.id("exo_toughness_chest");
    private static final Identifier LEGS_ARMOR = AtomCraft.id("exo_plating_legs");
    private static final Identifier LEGS_TOUGHNESS = AtomCraft.id("exo_toughness_legs");

    private EquipmentEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EquipmentEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(EquipmentEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(EquipmentEvents::onLogout);
    }

    public static void setJetState(Player player, int state) {
        JET_STATE.put(player.getUUID(), state);
    }

    public static int jetState(Player player) {
        return JET_STATE.getOrDefault(player.getUUID(), JET_OFF);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        JET_STATE.remove(event.getEntity().getUUID());
        JET_PENDING.remove(event.getEntity().getUUID());
        FLIGHT.remove(event.getEntity().getUUID());
        LAST_POSITION.remove(event.getEntity().getUUID());
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        long time = player.level().getGameTime();
        tickJetpack(player, time);
        if (time % 2 == 0) tickMagnet(player);
        if (time % 10 == 0) tickExo(player, 10);
        if (time % 20 == 0) recharge(player);
    }

    // ---------------------------------------------------------------- Jetpack

    private static void tickJetpack(ServerPlayer player, long time) {
        int state = jetState(player);
        ItemStack jetpack = Worn.jetpack(player);
        updateFlight(player, jetpack);
        if (jetpack.isEmpty()) return;
        boolean flying = FLIGHT.contains(player.getUUID()) && player.getAbilities().flying;
        if ((state == JET_THRUST || flying) && JetpackItem.fuel(jetpack) > 0) {
            int cost = state == JET_THRUST && !flying ? JetpackItem.thrustCost(jetpack) : Config.JETPACK_USE.get();
            JET_PENDING.merge(player.getUUID(), cost, Integer::sum);
            player.resetFallDistance();
        }
        if (time % 10 == 0) {
            Integer pending = JET_PENDING.remove(player.getUUID());
            if (pending != null) JetpackItem.setFuel(jetpack, JetpackItem.fuel(jetpack) - pending);
        }
    }

    /**
     * Módulo de Voo: com o jetpack vestido, hidrogênio no tanque e o voo ligado, o jogador voa como no
     * modo criativo (dois toques no pulo). Sem isso, o voo some (exceto no criativo/espectador).
     */
    public static void updateFlight(ServerPlayer player, ItemStack jetpack) {
        boolean allowed = !jetpack.isEmpty() && JetpackItem.hover(jetpack) && JetpackItem.fuel(jetpack) > 0;
        var abilities = player.getAbilities();
        if (allowed) {
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                FLIGHT.add(player.getUUID());
                player.onUpdateAbilities();
            }
        } else if (FLIGHT.remove(player.getUUID()) && !player.isCreative() && !player.isSpectator()) {
            abilities.mayfly = false;
            abilities.flying = false;
            player.onUpdateAbilities();
        }
    }

    /** Tecla de voo (H): liga/desliga o voo do jetpack vestido. */
    public static void toggleHover(ServerPlayer player) {
        ItemStack jetpack = Worn.jetpack(player);
        if (jetpack.isEmpty()) return;
        if (ModularItem.level(jetpack, EquipmentModule.HOVER) <= 0) {
            player.sendOverlayMessage(Component.translatable("message.atomcraft.hover_missing").withStyle(ChatFormatting.GRAY));
            return;
        }
        boolean hover = Boolean.FALSE.equals(jetpack.get(ModComponents.HOVER.get()));
        jetpack.set(ModComponents.HOVER.get(), hover);
        player.sendOverlayMessage(Component.translatable(hover ? "message.atomcraft.hover_on" : "message.atomcraft.hover_off")
                .withStyle(hover ? ChatFormatting.AQUA : ChatFormatting.GRAY));
    }

    // ---------------------------------------------------------------- Ímã

    private static void tickMagnet(ServerPlayer player) {
        ItemStack magnet = Worn.find(player, stack -> stack.getItem() instanceof MagnetItem && MagnetItem.active(stack));
        if (magnet != null && !player.isShiftKeyDown()) MagnetItem.pull(player);
    }

    // ---------------------------------------------------------------- Exoesqueleto

    /** Aplica os módulos do exoesqueleto vestido (chamado a cada {@code interval} ticks). */
    public static void tickExo(Player player, int interval) {
        ItemStack head = Worn.exo(player, EquipmentSlot.HEAD);
        ItemStack chest = Worn.exo(player, EquipmentSlot.CHEST);
        ItemStack legs = Worn.exo(player, EquipmentSlot.LEGS);
        ItemStack feet = Worn.exo(player, EquipmentSlot.FEET);

        // Capacete: visão noturna e respirador
        int nightVision = ModularItem.level(head, EquipmentModule.NIGHT_VISION);
        boolean underwater = player.isUnderWater();
        int respirator = underwater ? ModularItem.level(head, EquipmentModule.RESPIRATOR) : 0;
        if (powered(head, interval, nightVision, EquipmentModule.NIGHT_VISION, respirator, EquipmentModule.RESPIRATOR)) {
            if (nightVision > 0) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
            if (respirator > 0) player.setAirSupply(player.getMaxAirSupply());
        }

        // Peito e pernas: blindagem (passiva)
        int chestPlating = ModularItem.level(chest, EquipmentModule.PLATING);
        modifier(player, Attributes.ARMOR, CHEST_ARMOR, chestPlating * 2.0, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.ARMOR_TOUGHNESS, CHEST_TOUGHNESS, chestPlating * 1.0, AttributeModifier.Operation.ADD_VALUE);
        int legsPlating = ModularItem.level(legs, EquipmentModule.PLATING);
        modifier(player, Attributes.ARMOR, LEGS_ARMOR, legsPlating * 2.0, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.ARMOR_TOUGHNESS, LEGS_TOUGHNESS, legsPlating * 1.0, AttributeModifier.Operation.ADD_VALUE);

        // Pernas: velocidade e subida de degraus (só gastam energia com o jogador andando)
        boolean moving = moved(player);
        int speed = ModularItem.level(legs, EquipmentModule.SPEED);
        int step = ModularItem.level(legs, EquipmentModule.STEP_ASSIST);
        boolean legsOn = moving ? powered(legs, interval, speed, EquipmentModule.SPEED, step, EquipmentModule.STEP_ASSIST)
                : speed + step > 0 && Battery.energy(legs) > 0;
        modifier(player, Attributes.MOVEMENT_SPEED, SPEED, legsOn ? speed * 0.15 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(player, Attributes.STEP_HEIGHT, STEP, legsOn && step > 0 ? 0.5 : 0, AttributeModifier.Operation.ADD_VALUE);

        // Botas: salto e amortecedor (passivo)
        int jump = ModularItem.level(feet, EquipmentModule.JUMP);
        boolean feetOn = moving ? powered(feet, interval, jump, EquipmentModule.JUMP, 0, EquipmentModule.JUMP)
                : jump > 0 && Battery.energy(feet) > 0;
        modifier(player, Attributes.JUMP_STRENGTH, JUMP, feetOn ? jump * 0.1 : 0, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.SAFE_FALL_DISTANCE, FALL, ModularItem.level(feet, EquipmentModule.SHOCK_ABSORBER) * 4.0,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static final Map<UUID, net.minecraft.world.phys.Vec3> LAST_POSITION = new HashMap<>();

    /** Se o jogador se mexeu desde a última verificação (parado, calças e botas não gastam energia). */
    private static boolean moved(Player player) {
        net.minecraft.world.phys.Vec3 now = player.position();
        net.minecraft.world.phys.Vec3 last = LAST_POSITION.put(player.getUUID(), now);
        return last == null || last.distanceToSqr(now) > 0.01;
    }

    /** Gasta a energia dos módulos ativos da peça; falso se a bateria não der conta (os módulos param). */
    private static boolean powered(ItemStack piece, int interval, int levelA, EquipmentModule a, int levelB, EquipmentModule b) {
        if (piece.isEmpty()) return false;
        int cost = (levelA * a.energyPerTick() + levelB * b.energyPerTick()) * interval;
        if (cost == 0) return levelA + levelB > 0;
        return Battery.use(piece, cost, ExoArmorItem.CAPACITY);
    }

    private static void modifier(Player player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (amount == 0) {
            if (instance.hasModifier(id)) instance.removeModifier(id);
        } else {
            AttributeModifier current = instance.getModifier(id);
            if (current == null || current.amount() != amount) instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /** Escudo de energia do peitoral: absorve o dano gastando energia da bateria. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float remaining = absorb(player, event.getAmount());
        if (remaining <= 0) event.setCanceled(true);
        else if (remaining < event.getAmount()) event.setAmount(remaining);
    }

    /** Dano que sobra depois do escudo (gasta a energia correspondente). */
    public static float absorb(Player player, float amount) {
        ItemStack chest = Worn.exo(player, EquipmentSlot.CHEST);
        if (ModularItem.level(chest, EquipmentModule.SHIELD) <= 0 || amount <= 0) return amount;
        int perPoint = Config.SHIELD_ENERGY.get();
        int energy = Battery.energy(chest);
        int needed = (int) Math.ceil(amount * perPoint);
        if (energy >= needed) {
            Battery.set(chest, energy - needed, ExoArmorItem.CAPACITY);
            return 0;
        }
        Battery.set(chest, 0, ExoArmorItem.CAPACITY);
        return amount - (float) energy / perPoint;
    }

    // ---------------------------------------------------------------- Recarga sem fio

    /** Recarrega exoesqueleto e jetpack pelo transmissor vinculado (a cada segundo). */
    public static void recharge(Player player) {
        WirelessTransmitterBlockEntity transmitter = null;
        boolean searched = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = Worn.exo(player, slot);
            if (piece.isEmpty() || Battery.energy(piece) >= ExoArmorItem.CAPACITY) continue;
            if (!searched) {
                transmitter = Wireless.find(player);
                searched = true;
            }
            if (transmitter == null) return;
            Battery.charge(piece, ExoArmorItem.CAPACITY, transmitter, Config.EXO_CHARGE.get());
        }
        ItemStack jetpack = Worn.jetpack(player);
        if (!jetpack.isEmpty() && JetpackItem.fuel(jetpack) < JetpackItem.capacity()) {
            if (!searched) transmitter = Wireless.find(player);
            if (transmitter != null) JetpackItem.refuel(jetpack, transmitter, Config.JETPACK_REFUEL.get());
        }
    }
}
