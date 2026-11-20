package com.atomcraft.energy;

import com.atomcraft.block.EnergyMatrixBlock;
import com.atomcraft.block.MatrixPartBlock;
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
 * Formato fixo da Matriz de Energia: 3 de largura × 5 de altura × 3 de profundidade, atrás do controlador.
 * <p>
 * Coordenadas locais: x 0..2 (esquerda → direita, olhando o controlador de frente), y 0..4 (de baixo
 * para cima), z 0..2 (da frente para o fundo). O controlador fica no meio da face da frente da camada
 * de baixo (1, 0, 0).
 * <pre>
 * 'C' carcaça   'P' carcaça ou Interface da Matriz   'G' vidro (ou carcaça)   'X' núcleo   'K' controlador
 * </pre>
 * Base e topo: carcaça, com Interfaces possíveis só no meio de cada face (onde o modelo tem os
 * conectores). Nas 3 camadas do meio: carcaça nas arestas, vidro no centro das faces e um núcleo no
 * centro (3 núcleos empilhados).
 */
public final class MatrixStructure {

    public static final int WIDTH = 3, HEIGHT = 5, CORES = 3, CONTROLLER_Y = 0;
    private static final String[] BASE = {"CKC", "PCP", "CPC"};
    private static final String[] RING = {"CGC", "GXG", "CGC"};
    private static final String[] TOP = {"CPC", "PCP", "CPC"};
    private static final String[][] LAYERS = {BASE, RING, RING, RING, TOP};

    private MatrixStructure() {}

    public record Slot(BlockPos pos, MatrixPart part) {}

    /** {@code members}: posições ocupadas pelas peças certas (sem o controlador). */
    public record Result(boolean formed, List<Slot> missing, List<BlockPos> ports, List<BlockPos> members) {
        public Map<MatrixPart, Integer> missingCounts() {
            Map<MatrixPart, Integer> counts = new EnumMap<>(MatrixPart.class);
            for (Slot slot : missing) counts.merge(slot.part(), 1, Integer::sum);
            return counts;
        }
    }

    public static BlockPos toWorld(BlockPos controller, Direction facing, int x, int y, int z) {
        Direction right = facing.getCounterClockWise();
        return controller.relative(right, x - 1).relative(Direction.UP, y - CONTROLLER_Y).relative(facing.getOpposite(), z);
    }

    private static @Nullable MatrixPart expected(char c) {
        return switch (c) {
            case 'C', 'P' -> MatrixPart.CASING;
            case 'G' -> MatrixPart.GLASS;
            case 'X' -> MatrixPart.CORE;
            case 'K' -> MatrixPart.CONTROLLER;
            default -> null;
        };
    }

    public static @Nullable MatrixPart partOf(BlockState state) {
        if (state.getBlock() instanceof MatrixPartBlock part) return part.part();
        if (state.getBlock() instanceof EnergyMatrixBlock) return MatrixPart.CONTROLLER;
        return null;
    }

    private static boolean accepts(char c, @Nullable MatrixPart actual) {
        if (actual == null) return false;
        return switch (c) {
            case 'C' -> actual == MatrixPart.CASING;
            case 'P' -> actual == MatrixPart.CASING || actual == MatrixPart.PORT;
            case 'G' -> actual == MatrixPart.GLASS || actual == MatrixPart.CASING;
            default -> actual == expected(c);
        };
    }

    /** Posições onde pode ir uma Interface da Matriz (meio das faces da base e do topo). */
    public static List<BlockPos> portSlots(BlockPos controller, Direction facing) {
        List<BlockPos> slots = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < WIDTH; z++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (LAYERS[y][z].charAt(x) == 'P') slots.add(toWorld(controller, facing, x, y, z));
                }
            }
        }
        return slots;
    }

    /** Caixa da estrutura inteira (para o render). */
    public static net.minecraft.world.phys.AABB bounds(BlockPos controller, Direction facing) {
        BlockPos a = toWorld(controller, facing, 0, 0, 0), b = toWorld(controller, facing, WIDTH - 1, HEIGHT - 1, WIDTH - 1);
        return new net.minecraft.world.phys.AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    public static List<Slot> blueprint(BlockPos controller, Direction facing) {
        List<Slot> slots = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < WIDTH; z++) {
                for (int x = 0; x < WIDTH; x++) {
                    MatrixPart part = expected(LAYERS[y][z].charAt(x));
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
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < WIDTH; z++) {
                for (int x = 0; x < WIDTH; x++) {
                    char c = LAYERS[y][z].charAt(x);
                    MatrixPart want = expected(c);
                    if (want == null) continue;
                    BlockPos pos = toWorld(controller, facing, x, y, z);
                    MatrixPart actual = partOf(level.getBlockState(pos));
                    if (!accepts(c, actual)) {
                        missing.add(new Slot(pos, want));
                    } else {
                        if (actual == MatrixPart.PORT) ports.add(pos);
                        if (actual != MatrixPart.CONTROLLER) members.add(pos);
                    }
                }
            }
        }
        return new Result(missing.isEmpty(), missing, ports, members);
    }
}
