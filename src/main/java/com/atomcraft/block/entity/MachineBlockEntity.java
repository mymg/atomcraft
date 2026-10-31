package com.atomcraft.block.entity;

import com.atomcraft.storage.ElementNetwork;
import com.atomcraft.storage.FilteredItemHandler;
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

    private @Nullable ElementNetwork network;
    private long networkScanTime = Long.MIN_VALUE;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void serverTick();

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getName().getString();
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
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putString("owner_name", ownerName);
    }
}
