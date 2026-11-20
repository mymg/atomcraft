package com.atomcraft.block.entity;

import com.atomcraft.block.EnergyCellBlock;
import com.atomcraft.block.EnergyMatrixBlock;
import com.atomcraft.block.MachineBlock;
import com.atomcraft.energy.CellSide;
import com.atomcraft.energy.CellTier;
import com.atomcraft.energy.EnergyBuffer;
import com.atomcraft.energy.MatrixStructure;
import com.atomcraft.energy.SideMode;
import com.atomcraft.energy.SidedEnergyHandler;
import com.atomcraft.menu.EnergyCellMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.registry.ModComponents;
import com.atomcraft.storage.EnergyReceiver;
import com.atomcraft.upgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Célula de Energia (Básica, Avançada, Elite) e controlador da Matriz de Energia.
 * <p>
 * Células: cada lado (relativo à frente) pode receber, enviar, os dois ou nada; ligadas ao Cabo
 * Atômico, recebem dos geradores da rede e alimentam as máquinas da rede.
 * Matriz: a energia entra e sai pelas Portas da Matriz (modo de cada porta) e pela rede.
 * Ao quebrar, a energia fica guardada no item.
 */
public class EnergyCellBlockEntity extends MachineBlockEntity implements EnergyReceiver, EnergyBuffer, com.atomcraft.storage.NetworkLink,
        com.geckolib.animatable.GeoBlockEntity {

    /** A Matriz formada é desenhada pelo GeckoLib (modelo {@code energy_matrix}, sem animações). */
    private final com.geckolib.animatable.instance.AnimatableInstanceCache geoCache = com.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }


    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAPACITY_LO = 2, DATA_CAPACITY_HI = 3,
            DATA_IN_LO = 4, DATA_IN_HI = 5, DATA_OUT_LO = 6, DATA_OUT_HI = 7, DATA_TIER = 8, DATA_FORMED = 9,
            DATA_SIDES = 10, DATA_PORTS_IN = 16, DATA_PORTS_OUT = 17, DATA_COUNT = 18;
    private static final int STATS_WINDOW = 20;

    private final CellTier tier;
    private final SimpleEnergyHandler energy;
    private final SideMode[] sides = new SideMode[CellSide.values().length];
    private final List<BlockPos> ports = new ArrayList<>();
    private boolean formed;
    private int structureTimer;
    private long inWindow, outWindow;
    private int ticksInWindow;
    private int avgIn, avgOut;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int stored = energy.getAmountAsInt(), capacity = energy.getCapacityAsInt();
            if (index >= DATA_SIDES && index < DATA_SIDES + 6) return sides[index - DATA_SIDES].ordinal();
            return switch (index) {
                case DATA_ENERGY_LO -> stored & 0xFFFF;
                case DATA_ENERGY_HI -> (stored >>> 16) & 0xFFFF;
                case DATA_CAPACITY_LO -> capacity & 0xFFFF;
                case DATA_CAPACITY_HI -> (capacity >>> 16) & 0xFFFF;
                case DATA_IN_LO -> avgIn & 0xFFFF;
                case DATA_IN_HI -> (avgIn >>> 16) & 0xFFFF;
                case DATA_OUT_LO -> avgOut & 0xFFFF;
                case DATA_OUT_HI -> (avgOut >>> 16) & 0xFFFF;
                case DATA_TIER -> tier.ordinal();
                case DATA_FORMED -> isFormed() ? 1 : 0;
                case DATA_PORTS_IN -> (int) ports.stream().filter(p -> portMode(p).receives()).count();
                case DATA_PORTS_OUT -> (int) ports.stream().filter(p -> portMode(p).sends()).count();
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

    public EnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(typeFor(state), pos, state);
        this.tier = tierOf(state);
        int rate = tier.rate();
        this.energy = new SimpleEnergyHandler(tier.capacity(), rate, rate) {
            @Override
            protected void onEnergyChanged(int previousAmount) {
                long delta = (long) energy - previousAmount;
                if (delta > 0) inWindow += delta;
                else outWindow -= delta;
                setChanged();
            }
        };
        Arrays.fill(sides, SideMode.INPUT);
        sides[CellSide.FRONT.ordinal()] = SideMode.OUTPUT;
    }

    private static CellTier tierOf(BlockState state) {
        if (state.getBlock() instanceof EnergyCellBlock cell) return cell.tier();
        return CellTier.MATRIX;
    }

    private static BlockEntityType<EnergyCellBlockEntity> typeFor(BlockState state) {
        return state.getBlock() instanceof EnergyMatrixBlock ? ModBlockEntities.ENERGY_MATRIX.get() : ModBlockEntities.ENERGY_CELL.get();
    }

    public CellTier tier() {
        return tier;
    }

    @Override
    public SimpleEnergyHandler energy() {
        return energy;
    }

    public ContainerData data() {
        return data;
    }

    public SideMode side(CellSide side) {
        return sides[side.ordinal()];
    }

    public void setSide(CellSide side, SideMode mode) {
        if (tier == CellTier.MATRIX || sides[side.ordinal()] == mode) return;
        sides[side.ordinal()] = mode;
        setChanged();
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    public boolean isFormed() {
        return tier != CellTier.MATRIX || formed;
    }

    /** Portas da Matriz encontradas na última verificação da estrutura. */
    public List<BlockPos> ports() {
        return ports;
    }

    @Override
    public java.util.Collection<BlockPos> linkedNodes() {
        return tier == CellTier.MATRIX && formed ? List.copyOf(ports) : List.of();
    }

    private com.atomcraft.energy.PortMode portMode(BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof MatrixPortBlockEntity port ? port.mode() : com.atomcraft.energy.PortMode.BOTH;
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return false;
    }

    private Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    /** Capability de energia de um lado: limitada ao modo do lado (null = lado desligado). */
    public @Nullable EnergyHandler handlerFor(@Nullable Direction direction) {
        if (tier == CellTier.MATRIX) return isFormed() ? energy : null;
        if (direction == null) return energy;
        SideMode mode = side(CellSide.fromDirection(facing(), direction));
        if (mode == SideMode.NONE) return null;
        return new SidedEnergyHandler(energy, mode.receives(), mode.sends() && isEnabled());
    }

    /** Verifica a estrutura da Matriz, liga as portas a este controlador e acende a frente. */
    public boolean checkStructure() {
        if (level == null || tier != CellTier.MATRIX) return true;
        MatrixStructure.Result result = MatrixStructure.check(level, worldPosition, facing());
        boolean changed = result.formed() != formed || !ports.equals(result.ports());
        // Formada, as peças somem e o controlador desenha o modelo grande; as interfaces continuam visíveis.
        if (formed && !result.formed()) disassemble();
        formed = result.formed();
        if (formed) com.atomcraft.block.StructurePartBlock.setFormed(level, result.members(), true);
        ports.clear();
        ports.addAll(result.ports());
        for (BlockPos pos : ports) {
            if (level.getBlockEntity(pos) instanceof MatrixPortBlockEntity port) port.setController(worldPosition);
        }
        setStateFlag(EnergyMatrixBlock.FORMED, formed);
        setStateFlag(EnergyMatrixBlock.CHARGED, energy.getAmountAsInt() > 0);
        if (changed) {
            invalidateNetwork();
            level.invalidateCapabilities(worldPosition);
            for (BlockPos pos : result.ports()) level.invalidateCapabilities(pos);
        }
        return formed;
    }

    /** Mostra de novo todas as peças do projeto (ao desformar ou quebrar o controlador). */
    public void disassemble() {
        if (level == null || level.isClientSide() || tier != CellTier.MATRIX) return;
        java.util.List<BlockPos> all = new java.util.ArrayList<>();
        for (var slot : MatrixStructure.blueprint(worldPosition, facing())) all.add(slot.pos());
        com.atomcraft.block.StructurePartBlock.setFormed(level, all, false);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        disassemble();
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (tier == CellTier.MATRIX && structureTimer++ % 20 == 0) checkStructure();
        if (tier == CellTier.MATRIX && structureTimer % 10 == 0) setStateFlag(EnergyMatrixBlock.CHARGED, energy.getAmountAsInt() > 0);
        if (!isFormed()) {
            status = MachineStatus.INCOMPLETE;
            return;
        }
        int sent = 0;
        if (tier == CellTier.MATRIX) {
            for (BlockPos pos : ports) {
                if (!portMode(pos).pushes()) continue;
                int budget = tier.rate();
                for (Direction direction : Direction.values()) {
                    BlockPos target = pos.relative(direction);
                    if (MatrixStructure.partOf(level.getBlockState(target)) != null) continue;
                    EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, target, direction.getOpposite());
                    if (handler == null) continue;
                    int moved = push(handler, budget);
                    budget -= moved;
                    sent += moved;
                    if (budget <= 0) break;
                }
            }
            sent += pushToNetwork(tier.rate());
        } else {
            int budget = tier.rate();
            for (CellSide side : CellSide.values()) {
                if (budget <= 0) break;
                if (!side(side).sends()) continue;
                Direction direction = side.toDirection(facing());
                EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, worldPosition.relative(direction), direction.getOpposite());
                if (handler == null) continue;
                int moved = push(handler, budget);
                budget -= moved;
                sent += moved;
            }
            if (budget > 0) sent += pushToNetwork(budget);
        }
        status = sent > 0 || avgIn > 0 ? MachineStatus.WORKING : MachineStatus.IDLE;
    }

    /** Alimenta as máquinas da rede (não outras células). */
    private int pushToNetwork(int budget) {
        int sent = 0;
        for (EnergyReceiver receiver : network().receivers()) {
            if (budget - sent <= 0) break;
            if (receiver == this || receiver instanceof EnergyBuffer || receiver.isRemoved()) continue;
            sent += push(receiver.energy(), budget - sent);
        }
        return sent;
    }

    private int push(EnergyHandler target, int max) {
        int amount = Math.min(max, energy.getAmountAsInt());
        if (amount <= 0) return 0;
        int inserted;
        try (Transaction transaction = Transaction.openRoot()) {
            inserted = target.insert(amount, transaction);
            transaction.commit();
        }
        if (inserted > 0) energy.set(energy.getAmountAsInt() - inserted);
        return inserted;
    }

    @Override
    protected void afterTick() {
        super.afterTick();
        if (++ticksInWindow >= STATS_WINDOW) {
            avgIn = (int) Math.min(Integer.MAX_VALUE, inWindow / ticksInWindow);
            avgOut = (int) Math.min(Integer.MAX_VALUE, outWindow / ticksInWindow);
            inWindow = outWindow = 0;
            ticksInWindow = 0;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(switch (tier) {
            case BASIC -> "block.atomcraft.basic_energy_cell";
            case ADVANCED -> "block.atomcraft.advanced_energy_cell";
            case ELITE -> "block.atomcraft.elite_energy_cell";
            case MATRIX -> "block.atomcraft.energy_matrix";
        });
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EnergyCellMenu(id, inventory, this);
    }

    // ----- Energia guardada no item -----

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModComponents.STORED_ENERGY.get(), energy.getAmountAsInt());
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer stored = components.get(ModComponents.STORED_ENERGY.get());
        if (stored != null) energy.set(Math.min(stored, energy.getCapacityAsInt()));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
        for (CellSide side : CellSide.values()) {
            sides[side.ordinal()] = SideMode.byId(input.getIntOr("side_" + side.name().toLowerCase(), sides[side.ordinal()].ordinal()));
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        for (CellSide side : CellSide.values()) output.putInt("side_" + side.name().toLowerCase(), sides[side.ordinal()].ordinal());
    }
}
