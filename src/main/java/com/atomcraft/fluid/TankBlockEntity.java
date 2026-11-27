package com.atomcraft.fluid;

import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Tanque de Fluidos: guarda um fluido, aceita baldes, canos e bombas, e despeja no tanque de baixo
 * (como no Mekanism). Quebrado, o fluido fica no item.
 */
public class TankBlockEntity extends BlockEntity {

    private final FluidTier tier;
    private final FluidTank tank;
    private boolean dirtyClient;
    private int lastSentAmount = -1;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state);
        this.tier = state.getBlock() instanceof TankBlock block ? block.tier() : FluidTier.BASIC;
        this.tank = new FluidTank(tier.tankCapacity(), this::onTankChanged);
    }

    public FluidTier tier() {
        return tier;
    }

    public FluidTank tank() {
        return tank;
    }

    private void onTankChanged() {
        setChanged();
        dirtyClient = true;
    }

    public void serverTick() {
        if (level == null) return;
        // Despeja no tanque de baixo
        if (!tank.isEmpty() && level.getBlockEntity(worldPosition.below()) instanceof TankBlockEntity below) {
            try (Transaction transaction = Transaction.openRoot()) {
                int moved = ResourceHandlerUtil.move(tank, below.tank(), resource -> true, tank.amount(), transaction);
                if (moved > 0) transaction.commit();
            }
        }
        // Atualiza o cliente (nível do fluido desenhado no bloco) sem mandar pacote todo tick
        if (dirtyClient && (level.getGameTime() % 5 == 0 || tank.isEmpty() || lastSentAmount <= 0)) {
            dirtyClient = false;
            if (tank.amount() != lastSentAmount) {
                lastSentAmount = tank.amount();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty()) components.set(ModComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(tank.stack()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        SimpleFluidContent content = components.get(ModComponents.FLUID_CONTENT.get());
        if (content != null) tank.setStack(content.copy());
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }
}
