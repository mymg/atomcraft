package com.atomcraft.block.entity;

import com.atomcraft.storage.ElementNetwork;
import com.atomcraft.storage.FilteredItemHandler;
import com.atomcraft.upgrade.UpgradeChipItem;
import com.atomcraft.upgrade.UpgradeType;
import com.atomcraft.upgrade.Upgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Base das máquinas: dono (quem colocou o bloco), acesso à rede de elementos
 * (com cache) e tick no servidor.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {

    protected @Nullable UUID owner;
    protected String ownerName = "";
    protected MachineStatus status = MachineStatus.IDLE;
    /** Botão de ligar/desligar da interface. */
    private boolean enabled = true;
    private final FilteredItemHandler upgrades = new FilteredItemHandler(Upgrades.SLOTS, Upgrades.MAX_PER_SLOT,
            (slot, resource) -> resource.getItem() instanceof UpgradeChipItem chip && supportsUpgrade(chip.type()),
            this::setChanged);

    private @Nullable ElementNetwork network;
    private long networkScanTime = Long.MIN_VALUE;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void serverTick();

    /** Chamado pelo ticker do bloco: respeita o botão de ligar/desligar. */
    public final void tick() {
        if (enabled) {
            serverTick();
        } else {
            status = MachineStatus.DISABLED;
            disabledTick();
        }
    }

    /** Tick com a máquina desligada (geradores continuam entregando a energia guardada). */
    protected void disabledTick() {}

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
    }

    /** Se a máquina tem botão de ligar/desligar. */
    public boolean hasPowerControl() {
        return true;
    }

    /** Quais chips de upgrade a máquina aceita. */
    public boolean supportsUpgrade(UpgradeType type) {
        return false;
    }

    public FilteredItemHandler upgrades() {
        return upgrades;
    }

    /** Quantidade de chips instalados de um tipo (até {@link Upgrades#MAX_PER_TYPE}). */
    public int upgradeCount(UpgradeType type) {
        if (!supportsUpgrade(type)) return 0;
        int count = 0;
        for (int i = 0; i < upgrades.size(); i++) {
            if (upgrades.getResource(i).getItem() instanceof UpgradeChipItem chip && chip.type() == type) {
                count += upgrades.getAmountAsInt(i);
            }
        }
        return Math.min(Upgrades.MAX_PER_TYPE, count);
    }

    public MachineStatus status() {
        return status;
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getName().getString();
        setChanged();
    }

    public void setOwner(UUID uuid, String name) {
        this.owner = uuid;
        this.ownerName = name;
        setChanged();
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    protected @Nullable ServerPlayer onlineOwner() {
        if (owner == null || level == null || level.getServer() == null) return null;
        return level.getServer().getPlayerList().getPlayer(owner);
    }

    /** Rede conectada a esta máquina. Refeita a cada segundo ou quando um Drive some. */
    public ElementNetwork network() {
        Level lvl = getLevel();
        if (lvl == null) return ElementNetwork.EMPTY;
        long now = lvl.getGameTime();
        if (network == null || now - networkScanTime >= 20 || !network.isStillValid()) {
            network = ElementNetwork.scan(lvl, worldPosition);
            networkScanTime = now;
        }
        return network;
    }

    protected boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    /**
     * Mantém a fração já concluída quando a duração muda no meio da operação
     * (ex.: chips de velocidade instalados durante a produção).
     */
    protected static int rescaleProgress(int progress, int oldMax, int newMax) {
        if (oldMax <= 0 || oldMax == newMax) return progress;
        return (int) Math.min(newMax, (long) progress * newMax / oldMax);
    }

    protected static int permille(int progress, int max) {
        return max <= 0 ? 0 : (int) Math.min(1000, (long) progress * 1000 / max);
    }

    protected static void dropContents(Level level, BlockPos pos, FilteredItemHandler handler) {
        for (int i = 0; i < handler.size(); i++) {
            ItemStack stack = handler.getStack(i);
            if (!stack.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) dropContents(level, pos, upgrades);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
        enabled = input.getBooleanOr("enabled", true);
        upgrades.deserialize(input.childOrEmpty("upgrades"));
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putString("owner_name", ownerName);
        output.putBoolean("enabled", enabled);
        upgrades.serialize(output.child("upgrades"));
    }
}
