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
 * no centro da face da frente (1, 1, 0), virado para fora.
 * <pre>
 * 'C' carcaça quântica   'G' vidro quântico (ou carcaça)   'X' núcleo quântico   'K' controlador
 * </pre>
 * Portas Quânticas podem ocupar qualquer posição de carcaça ou vidro.
 * Camada de baixo e de cima: só carcaça. Camada do meio: carcaça nos cantos, vidro nas
 * laterais e no fundo, núcleo no centro e o controlador na frente.
 */
public final class QuantumStructure {

    public static final int SIZE = 3;
    private static final String[] SOLID = {"CCC", "CCC", "CCC"};
    private static final String[] MIDDLE = {"CKC", "GXG", "CGC"};
    private static final String[][] LAYERS = {SOLID, MIDDLE, SOLID};

    private QuantumStructure() {}

    public record Slot(BlockPos pos, QuantumPart part) {}

    public record Result(boolean formed, List<Slot> missing, List<BlockPos> ports) {
        public Map<QuantumPart, Integer> missingCounts() {
            Map<QuantumPart, Integer> counts = new EnumMap<>(QuantumPart.class);
            for (Slot slot : missing) counts.merge(slot.part(), 1, Integer::sum);
            return counts;
        }
    }

    /** {@code facing} é para onde o controlador olha (para fora da estrutura). */
    public static BlockPos toWorld(BlockPos controller, Direction facing, int x, int y, int z) {
        Direction right = facing.getCounterClockWise();
        return controller.relative(right, x - 1).relative(Direction.UP, y - 1).relative(facing.getOpposite(), z);
    }

    private static @Nullable QuantumPart expected(char c) {
        return switch (c) {
            case 'C' -> QuantumPart.CASING;
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
        // Portas Quânticas podem ficar no lugar de qualquer carcaça ou vidro
        if (actual == QuantumPart.PORT) return c == 'C' || c == 'G';
        if (c == 'G') return actual == QuantumPart.GLASS || actual == QuantumPart.CASING;
        return actual == expected(c);
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
        for (int y = 0; y < SIZE; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    char c = LAYERS[y][z].charAt(x);
                    QuantumPart want = expected(c);
                    if (want == null) continue;
                    BlockPos pos = toWorld(controller, facing, x, y, z);
                    QuantumPart actual = partOf(level.getBlockState(pos));
                    if (!accepts(c, actual)) missing.add(new Slot(pos, want));
                    else if (actual == QuantumPart.PORT) ports.add(pos);
                }
            }
        }
        return new Result(missing.isEmpty(), missing, ports);
    }
}
