package com.atomcraft.storage;

import com.atomcraft.block.entity.DriveBlockEntity;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Uma rede de elementos: todos os blocos {@link NetworkNode} conectados a partir
 * de uma posição. O estoque da rede é a soma dos discos de todos os Drives.
 * <p>
 * Inserir e retirar são operações "tudo ou nada": ou a composição inteira cabe
 * (ou está disponível), ou nada é alterado.
 */
public final class ElementNetwork {

    public static final int MAX_NODES = 4096;
    public static final ElementNetwork EMPTY = new ElementNetwork(List.of());

    private final List<DriveBlockEntity> drives;

    private ElementNetwork(List<DriveBlockEntity> drives) {
        this.drives = drives;
    }

    /** Busca em largura pelos blocos da rede conectados a {@code origin}. */
    public static ElementNetwork scan(Level level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<DriveBlockEntity> drives = new ArrayList<>();
        BlockPos start = origin.immutable();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (level.getBlockEntity(pos) instanceof DriveBlockEntity drive) drives.add(drive);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.contains(next) || visited.size() >= MAX_NODES || !level.isLoaded(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof NetworkNode) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return new ElementNetwork(drives);
    }

    public boolean hasStorage() {
        for (DriveBlockEntity drive : drives) {
            if (!drive.isRemoved() && drive.hasDisks()) return true;
        }
        return false;
    }

    /** Falso se algum Drive da rede foi removido desde a busca. */
    public boolean isStillValid() {
        for (DriveBlockEntity drive : drives) {
            if (drive.isRemoved()) return false;
        }
        return true;
    }

    public long capacity() {
        long total = 0;
        for (DiskRef disk : disks()) total += disk.capacity;
        return total;
    }

    public long used() {
        long total = 0;
        for (DiskRef disk : disks()) total += disk.used();
        return total;
    }

    public Composition totals() {
        EnumMap<ChemicalElement, Long> totals = new EnumMap<>(ChemicalElement.class);
        for (DiskRef disk : disks()) disk.contents.forEach((element, amount) -> totals.merge(element, amount, Long::sum));
        return new Composition(totals);
    }

    public boolean canExtract(Composition composition, long times) {
        Composition totals = totals();
        for (var entry : composition.elements().entrySet()) {
            if (totals.get(entry.getKey()) < entry.getValue() * times) return false;
        }
        return true;
    }

    /** Quantas unidades da composição podem ser retiradas com o estoque atual. */
    public long maxExtractable(Composition composition) {
        if (composition.isEmpty()) return 0;
        Composition totals = totals();
        long max = Long.MAX_VALUE;
        for (var entry : composition.elements().entrySet()) {
            max = Math.min(max, totals.get(entry.getKey()) / entry.getValue());
        }
        return max;
    }

    public boolean canInsert(Composition composition, long times) {
        long free = 0;
        for (DiskRef disk : disks()) free += disk.free();
        return free >= composition.totalAtoms() * times;
    }

    public boolean insert(Composition composition, long times) {
        List<DiskRef> disks = disks();
        long free = 0;
        for (DiskRef disk : disks) free += disk.free();
        if (free < composition.totalAtoms() * times) return false;

        for (var entry : composition.elements().entrySet()) {
            long remaining = entry.getValue() * times;
            // Preferência para discos que já têm esse elemento, para mantê-los organizados.
            for (int round = 0; round < 2 && remaining > 0; round++) {
                for (DiskRef disk : disks) {
                    if (remaining <= 0) break;
                    boolean hasElement = disk.contents.containsKey(entry.getKey());
                    if ((round == 0) != hasElement) continue;
                    long moved = Math.min(remaining, disk.free());
                    if (moved <= 0) continue;
                    disk.contents.merge(entry.getKey(), moved, Long::sum);
                    disk.dirty = true;
                    remaining -= moved;
                }
            }
        }
        commit(disks);
        return true;
    }

    public boolean extract(Composition composition, long times) {
        if (!canExtract(composition, times)) return false;
        List<DiskRef> disks = disks();
        for (var entry : composition.elements().entrySet()) {
            long remaining = entry.getValue() * times;
            for (DiskRef disk : disks) {
                if (remaining <= 0) break;
                long available = disk.contents.getOrDefault(entry.getKey(), 0L);
                long moved = Math.min(remaining, available);
                if (moved <= 0) continue;
                if (available - moved == 0) disk.contents.remove(entry.getKey());
                else disk.contents.put(entry.getKey(), available - moved);
                disk.dirty = true;
                remaining -= moved;
            }
        }
        commit(disks);
        return true;
    }

    private List<DiskRef> disks() {
        List<DiskRef> result = new ArrayList<>();
        for (DriveBlockEntity drive : drives) {
            if (drive.isRemoved()) continue;
            for (int slot = 0; slot < DriveBlockEntity.SLOTS; slot++) {
                ItemStack stack = drive.getDisk(slot);
                if (stack.getItem() instanceof ElementDiskItem disk) {
                    result.add(new DiskRef(drive, slot, disk.capacity(), ElementDiskItem.contents(stack)));
                }
            }
        }
        return result;
    }

    private static void commit(List<DiskRef> disks) {
        for (DiskRef disk : disks) {
            if (disk.dirty) disk.drive.setDiskContents(disk.slot, new Composition(disk.contents));
        }
    }

    private static final class DiskRef {
        final DriveBlockEntity drive;
        final int slot;
        final long capacity;
        final Map<ChemicalElement, Long> contents;
        boolean dirty;

        DiskRef(DriveBlockEntity drive, int slot, long capacity, Composition contents) {
            this.drive = drive;
            this.slot = slot;
            this.capacity = capacity;
            this.contents = new EnumMap<>(ChemicalElement.class);
            this.contents.putAll(contents.elements());
        }

        long used() {
            long total = 0;
            for (long amount : contents.values()) total += amount;
            return total;
        }

        long free() {
            return Math.max(0, capacity - used());
        }
    }
}
