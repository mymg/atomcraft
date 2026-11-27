package com.atomcraft.compat.jade;

import com.atomcraft.AtomCraft;
import com.atomcraft.chemistry.AtomicValue;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.client.ClientState;
import com.atomcraft.equipment.Worn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade (opcional): com os Óculos Analisadores (ou o capacete com o aprimoramento Analisador), a fórmula do
 * bloco ou item mirado aparece no próprio tooltip do Jade, no lugar do painel do AtomCraft.
 */
@WailaPlugin
public class AtomCraftJadePlugin implements IWailaPlugin {

    static final Identifier UID = AtomCraft.id("composition");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BlockComposition.INSTANCE, Block.class);
        registration.registerEntityComponent(ItemComposition.INSTANCE, ItemEntity.class);
    }

    static void append(ITooltip tooltip, Player player, Item item) {
        if (player == null || item == null || item == Items.AIR || !Worn.hasAnalyzer(player)) return;
        AtomicValue value = AtomicValues.get(item, true);
        tooltip.add(value == null
                ? Component.translatable("hud.atomcraft.no_composition").withStyle(ChatFormatting.GRAY)
                : Component.literal(ClientState.formula(value.composition())).withStyle(ChatFormatting.AQUA));
    }

    enum BlockComposition implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            append(tooltip, accessor.getPlayer(), accessor.getBlock().asItem());
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    enum ItemComposition implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            Entity entity = accessor.getEntity();
            if (entity instanceof ItemEntity dropped) append(tooltip, accessor.getPlayer(), dropped.getItem().getItem());
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
