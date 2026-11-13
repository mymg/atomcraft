package com.atomcraft.block.entity;

import com.atomcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/** Porta do Reator: repassa a energia do controlador para cabos de outros mods. */
public class ReactorPortBlockEntity extends BlockEntity {

    private @Nullable BlockPos controller;

    public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_PORT.get(), pos, state);
    }

    public void setController(@Nullable BlockPos controller) {
        this.controller = controller;
        setChanged();
    }

    /** Energia do reator, ou null se a porta não faz parte de um reator formado. */
    public @Nullable EnergyHandler energy() {
        if (controller == null || level == null) return null;
        return level.getBlockEntity(controller) instanceof FusionReactorBlockEntity reactor && reactor.isFormed()
                ? reactor.energy() : null;
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        long packed = input.getLongOr("controller", Long.MIN_VALUE);
        controller = packed == Long.MIN_VALUE ? null : BlockPos.of(packed);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (controller != null) output.putLong("controller", controller.asLong());
    }
}
