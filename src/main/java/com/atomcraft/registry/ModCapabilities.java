package com.atomcraft.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Expõe os inventários e a energia para outros mods: pipes, esteiras, hoppers e
 * cabos de energia (FE) de qualquer mod que use as capacidades do NeoForge.
 */
public final class ModCapabilities {

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ATOMIZER.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ANALYZER.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.REPLICATOR.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.REPLICATOR.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ATOMIZER.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.DRIVE.get(), (be, side) -> be.disks());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(), (be, side) -> be.automation());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.CARBON_GENERATOR.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.FUSION_REACTOR.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.SYNTHESIZER.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SYNTHESIZER.get(), (be, side) -> be.automation());
    }

    private ModCapabilities() {}
}
