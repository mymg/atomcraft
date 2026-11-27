package com.atomcraft.fluid;

import com.atomcraft.Config;
import com.atomcraft.block.entity.MachineBlockEntity;
import com.atomcraft.block.entity.MachineStatus;
import com.atomcraft.menu.PumpMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.EnergyReceiver;
import com.atomcraft.upgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Bomba: puxa fontes de fluido de baixo dela (o lago ou poça inteira, até o alcance da configuração),
 * gastando energia por balde, e empurra o fluido para os blocos em volta (canos, tanques, Atomizador).
 * Por padrão a água não é removida (é infinita, como um poço).
 */
public class PumpBlockEntity extends MachineBlockEntity implements EnergyReceiver {

    public static final int CAPACITY = 16_000, BUCKET = 1000, PUSH_PER_TICK = 4_000;
    public static final int DATA_STATUS = 0, DATA_ENERGY_LO = 1, DATA_ENERGY_HI = 2, DATA_CAPACITY_LO = 3, DATA_CAPACITY_HI = 4,
            DATA_FLUID = 5, DATA_AMOUNT_LO = 6, DATA_AMOUNT_HI = 7, DATA_PROGRESS = 8, DATA_COUNT = 9;

    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(Config.PUMP_CAPACITY.get(), 10_000, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };
    private final FluidTank tank = new FluidTank(CAPACITY, this::setChanged);
    private final ResourceHandler<FluidResource> automation = tank.extractOnly();
    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_STATUS -> status.ordinal();
                case DATA_ENERGY_LO -> energy.getAmountAsInt() & 0xFFFF;
                case DATA_ENERGY_HI -> (energy.getAmountAsInt() >>> 16) & 0xFFFF;
                case DATA_CAPACITY_LO -> energy.getCapacityAsInt() & 0xFFFF;
                case DATA_CAPACITY_HI -> (energy.getCapacityAsInt() >>> 16) & 0xFFFF;
                case DATA_FLUID -> tank.fluidId();
                case DATA_AMOUNT_LO -> tank.amount() & 0xFFFF;
                case DATA_AMOUNT_HI -> (tank.amount() >>> 16) & 0xFFFF;
                case DATA_PROGRESS -> permille(progress, ticksPerBucket());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP.get(), pos, state);
    }

    @Override
    public SimpleEnergyHandler energy() {
        return energy;
    }

    public FluidTank tank() {
        return tank;
    }

    public ResourceHandler<FluidResource> automation() {
        return automation;
    }

    public ContainerData data() {
        return data;
    }

    private int ticksPerBucket() {
        return Math.max(1, Config.PUMP_TICKS.get() >> Math.min(3, upgradeCount(UpgradeType.SPEED)));
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return type == UpgradeType.SPEED;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        push();
        if (tank.amount() + BUCKET > tank.capacity()) {
            status = MachineStatus.OUTPUT_FULL;
            return;
        }
        BlockPos source = findSource();
        if (source == null) {
            status = MachineStatus.IDLE;
            progress = 0;
            return;
        }
        int cost = Config.PUMP_ENERGY.get();
        if (energy.getAmountAsInt() < cost) {
            status = MachineStatus.NO_ENERGY;
            return;
        }
        status = MachineStatus.WORKING;
        if (++progress < ticksPerBucket()) return;
        progress = 0;
        if (pump(source)) energy.set(energy.getAmountAsInt() - cost);
        setChanged();
    }

    /** Fonte de fluido mais próxima ligada ao bloco de baixo (mesmo fluido do tanque, se houver). */
    public @Nullable BlockPos findSource() {
        if (level == null) return null;
        BlockPos start = worldPosition.below();
        FluidState first = level.getFluidState(start);
        if (first.isEmpty()) return null;
        Fluid fluid = first.getType() instanceof net.minecraft.world.level.material.FlowingFluid flowing ? flowing.getSource() : first.getType();
        if (!tank.isEmpty() && !tank.fluid().isSame(fluid)) return null;
        int range = Config.PUMP_RANGE.get();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < 8192) {
            BlockPos pos = queue.poll();
            FluidState state = level.getFluidState(pos);
            if (state.isEmpty() || !state.getType().isSame(fluid)) continue;
            if (state.isSource() && level.getBlockState(pos).getBlock() instanceof BucketPickup) return pos;
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP && pos.getY() >= start.getY()) continue;
                BlockPos next = pos.relative(direction);
                if (next.distManhattan(start) > range || !level.isLoaded(next) || !seen.add(next)) continue;
                queue.add(next);
            }
        }
        return null;
    }

    private boolean pump(BlockPos pos) {
        if (level == null) return false;
        FluidState fluidState = level.getFluidState(pos);
        Fluid fluid = fluidState.getType();
        boolean infinite = Config.PUMP_INFINITE_WATER.get() && fluid.isSame(Fluids.WATER);
        Fluid stored;
        if (infinite) {
            stored = Fluids.WATER;
        } else {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BucketPickup pickup)) return false;
            ItemStack bucket = pickup.pickupBlock(null, level, pos, state);
            if (bucket.isEmpty()) return false;
            stored = bucket.getItem() instanceof BucketItem bucketItem ? bucketItem.getContent() : fluid;
        }
        if (stored == Fluids.EMPTY) return false;
        FluidStack current = tank.stack();
        tank.setStack(new FluidStack(stored, current.isEmpty() ? BUCKET : current.getAmount() + BUCKET));
        return true;
    }

    /** Empurra o fluido para canos, tanques e máquinas em volta (menos para baixo). */
    private void push() {
        if (level == null || tank.isEmpty()) return;
        int budget = PUSH_PER_TICK;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN || budget <= 0 || tank.isEmpty()) continue;
            ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if (target == null) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int moved = ResourceHandlerUtil.move(tank, target, resource -> true, budget, transaction);
                if (moved > 0) {
                    budget -= moved;
                    transaction.commit();
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.pump");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PumpMenu(id, inventory, this);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
        tank.deserialize(input.childOrEmpty("tank"));
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        tank.serialize(output.child("tank"));
        output.putInt("progress", progress);
    }
}
