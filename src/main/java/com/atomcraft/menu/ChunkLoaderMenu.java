package com.atomcraft.menu;

import com.atomcraft.block.entity.ChunkLoaderBlockEntity;
import com.atomcraft.registry.ModBlocks;
import com.atomcraft.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;

/** Carregador de Chunks: grade das chunks em volta, energia e custo. */
public class ChunkLoaderMenu extends BaseMenu {

    public static final int WIDTH = 176, HEIGHT = 124;

    private final ContainerData data;

    public static ChunkLoaderMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new ChunkLoaderMenu(id, inventory, buf.readBlockPos(), new SimpleContainerData(ChunkLoaderBlockEntity.DATA_COUNT));
    }

    public ChunkLoaderMenu(int id, Inventory inventory, ChunkLoaderBlockEntity loader) {
        this(id, inventory, loader.getBlockPos(), loader.data());
    }

    private ChunkLoaderMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        super(ModMenus.CHUNK_LOADER.get(), id, inventory, pos);
        this.data = data;
        addMachineControlsWithoutUpgrades();
        addDataSlots(data);
    }

    @Override
    protected Block block() {
        return ModBlocks.CHUNK_LOADER.get();
    }

    @Override
    protected boolean syncsNetwork() {
        return false;
    }

    public int energy() {
        return combine(data.get(ChunkLoaderBlockEntity.DATA_ENERGY_LO), data.get(ChunkLoaderBlockEntity.DATA_ENERGY_HI));
    }

    public int capacity() {
        return combine(data.get(ChunkLoaderBlockEntity.DATA_CAPACITY_LO), data.get(ChunkLoaderBlockEntity.DATA_CAPACITY_HI));
    }

    public int mask() {
        return combine(data.get(ChunkLoaderBlockEntity.DATA_MASK_LO), data.get(ChunkLoaderBlockEntity.DATA_MASK_HI));
    }

    public boolean selected(int cell) {
        return (mask() & (1 << cell)) != 0;
    }

    public int selectedCount() {
        return Integer.bitCount(mask());
    }

    public int loaded() {
        return data.get(ChunkLoaderBlockEntity.DATA_LOADED);
    }

    public int radius() {
        return data.get(ChunkLoaderBlockEntity.DATA_RADIUS);
    }

    public int maxChunks() {
        return data.get(ChunkLoaderBlockEntity.DATA_MAX);
    }

    public int costPerChunk() {
        return data.get(ChunkLoaderBlockEntity.DATA_COST);
    }
}
