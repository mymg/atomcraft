package com.atomcraft.block.entity;

import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.NetworkLink;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/** Porta Quântica: repassa energia, itens e a rede para o controlador do Replicador Quântico. */
public class QuantumPortBlockEntity extends BlockEntity implements NetworkLink {

    private @Nullable BlockPos controllerPos;

    public QuantumPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUANTUM_PORT.get(), pos, state);
    }

    public void setController(@Nullable BlockPos pos) {
        if (pos == null ? controllerPos == null : pos.equals(controllerPos)) return;
        controllerPos = pos == null ? null : pos.immutable();
        setChanged();
    }

    /** Replicador Quântico formado desta porta, ou null. */
    public @Nullable ReplicatorBlockEntity controller() {
        if (level == null || controllerPos == null) return null;
        if (level.getBlockEntity(controllerPos) instanceof ReplicatorBlockEntity replicator && replicator.isFormed()
                && replicator.ports().contains(worldPosition)) {
            return replicator;
        }
        return null;
    }

    @Override
    public Collection<BlockPos> linkedNodes() {
        ReplicatorBlockEntity replicator = controller();
        return replicator == null ? List.of() : List.of(replicator.getBlockPos());
    }

    public @Nullable EnergyHandler energy() {
        ReplicatorBlockEntity replicator = controller();
        return replicator == null ? null : replicator.energy();
    }

    public @Nullable ResourceHandler<ItemResource> items() {
        ReplicatorBlockEntity replicator = controller();
        return replicator == null ? null : replicator.automation();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        controllerPos = input.read("controller", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (controllerPos != null) output.store("controller", BlockPos.CODEC, controllerPos);
    }
}
