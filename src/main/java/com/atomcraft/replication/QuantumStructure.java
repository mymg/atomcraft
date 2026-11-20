package com.atomcraft.replication;

import com.atomcraft.block.QuantumPartBlock;
import com.atomcraft.block.QuantumReplicatorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Formato fixo do Replicador Quântico: um cubo 3×3×3 atrás do controlador.
 * <p>
 * Coordenadas locais: x 0..2 (esquerda → direita, olhando o controlador de frente),
 * y 0..2 (de baixo para cima) e z 0..2 (da frente para o fundo). O controlador fica
 * no meio da face da frente da camada de baixo (1, 0, 0), virado para fora.
 * <pre>
 * 'C' carcaça   'P' carcaça ou Porta Quântica   'G' vidro quântico (ou carcaça)   'X' núcleo   'K' controlador
 * </pre>
 * Camada de baixo: carcaça, com Portas Quânticas possíveis só no meio das outras três faces (onde o
 * modelo tem os conectores). Camada do meio: carcaça nos cantos, vidro no meio das faces e o núcleo no
 * centro. Camada de cima: só carcaça.
 */
public final class QuantumStructure {

    public static final int SIZE = 3;
    private static final String[] BASE = {"CKC", "PCP", "CPC"};
    private static final String[] MIDDLE = {"CGC", "GXG", "CGC"};
    private static final String[] TOP = {"CCC", "CCC", "CCC"};
    private static final String[][] LAYERS = {BASE, MIDDLE, TOP};

    private QuantumStructure() {}

    public record Slot(BlockPos pos, QuantumPart part) {}

    /** {@code members}: posições ocupadas pelas peças certas (sem o controlador). */
    public record Result(boolean formed, List<Slot> missing, List<BlockPos> ports, List<BlockPos> members) {
        public Map<QuantumPart, Integer> missingCounts() {
            Map<QuantumPart, Integer> counts = new EnumMap<>(QuantumPart.class);
            for (Slot slot : missing) counts.merge(slot.part(), 1, Integer::sum);
            return counts;
        }
    }

    /** {@code facing} é para onde o controlador olha (para fora da estrutura). */
    public static BlockPos toWorld(BlockPos controller, Direction facing, int x, int y, int z) {
        Direction right = facing.getCounterClockWise();
        return controller.relative(right, x - 1).relative(Direction.UP, y).relative(facing.getOpposite(), z);
    }

    private static @Nullable QuantumPart expected(char c) {
        return switch (c) {
            case 'C', 'P' -> QuantumPart.CASING;
            case 'G' -> QuantumPart.GLASS;
            case 'X' -> QuantumPart.CORE;
            case 'K' -> QuantumPart.CONTROLLER;
            default -> null;
        };
    }

    public static @Nullable QuantumPart partOf(BlockState state) {
        if (state.getBlock() instanceof QuantumPartBlock part) return part.part();
        if (state.getBlock() instanceof QuantumReplicatorBlock) return QuantumPart.CONTROLLER;
        return null;
    }

    private static boolean accepts(char c, @Nullable QuantumPart actual) {
        if (actual == null) return false;
        // Portas Quânticas só no meio das faces da camada de baixo
        if (actual == QuantumPart.PORT) return c == 'P';
        if (c == 'G') return actual == QuantumPart.GLASS || actual == QuantumPart.CASING;
        return actual == expected(c);
    }

    /** Posições onde pode ir uma Porta Quântica (meio das faces da camada de baixo). */
    public static List<BlockPos> portSlots(BlockPos controller, Direction facing) {
        List<BlockPos> slots = new ArrayList<>();
        for (int z = 0; z < SIZE; z++) {
            for (int x = 0; x < SIZE; x++) {
                if (LAYERS[0][z].charAt(x) == 'P') slots.add(toWorld(controller, facing, x, 0, z));
            }
        }
        return slots;
    }

    /** Caixa da estrutura inteira (para o render). */
    public static net.minecraft.world.phys.AABB bounds(BlockPos controller, Direction facing) {
        BlockPos a = toWorld(controller, facing, 0, 0, 0), b = toWorld(controller, facing, SIZE - 1, SIZE - 1, SIZE - 1);
        return new net.minecraft.world.phys.AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** Todas as posições com a peça "ideal" (usado pelos testes e pela vitrine). */
    public static List<Slot> blueprint(BlockPos controller, Direction facing) {
        List<Slot> slots = new ArrayList<>();
        for (int y = 0; y < SIZE; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    QuantumPart part = expected(LAYERS[y][z].charAt(x));
                    if (part != null) slots.add(new Slot(toWorld(controller, facing, x, y, z), part));
                }
            }
        }
        return slots;
    }

    public static Result check(BlockGetter level, BlockPos controller, Direction facing) {
        List<Slot> missing = new ArrayList<>();
        List<BlockPos> ports = new ArrayList<>();
        List<BlockPos> members = new ArrayList<>();
        for (int y = 0; y < SIZE; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    char c = LAYERS[y][z].charAt(x);
                    QuantumPart want = expected(c);
                    if (want == null) continue;
                    BlockPos pos = toWorld(controller, facing, x, y, z);
                    QuantumPart actual = partOf(level.getBlockState(pos));
                    if (!accepts(c, actual)) {
                        missing.add(new Slot(pos, want));
                    } else {
                        if (actual == QuantumPart.PORT) ports.add(pos);
                        if (actual != QuantumPart.CONTROLLER) members.add(pos);
                    }
                }
            }
        }
        return new Result(missing.isEmpty(), missing, ports, members);
    }
}
