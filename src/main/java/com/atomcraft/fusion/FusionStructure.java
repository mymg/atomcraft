package com.atomcraft.fusion;

import com.atomcraft.block.FusionReactorBlock;
import com.atomcraft.block.ReactorPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Formato fixo do Reator de Fusão: um tokamak 7×7×5.
 * <p>
 * Coordenadas locais: x 0..6 (esquerda → direita, olhando o controlador de frente),
 * y 0..4 (de baixo para cima) e z 0..6 (da frente, onde fica o controlador, para o fundo).
 * O controlador fica no centro da parede da frente, na camada do meio (3, 2, 0), virado para fora.
 * <pre>
 * '-' qualquer coisa   '.' vazio (canal de plasma)   'C' carcaça (ou porta)
 * 'V' vidro (ou carcaça/porta)   'B' bobina magnética   'S' solenoide central   'K' controlador
 * </pre>
 */
public final class FusionStructure {

    public static final int SIZE = 7, HEIGHT = 5, CONTROLLER_X = 3, CONTROLLER_Y = 2;

    private static final String[] CAP = {
            "-CCCCC-",
            "CCCCCCC",
            "CCCCCCC",
            "CCCSCCC",
            "CCCCCCC",
            "CCCCCCC",
            "-CCCCC-"};
    private static final String[] RING = {
            "-CCCCC-",
            "C.....C",
            "C.BBB.C",
            "C.BSB.C",
            "C.BBB.C",
            "C.....C",
            "-CCCCC-"};
    private static final String[] CENTER = {
            "-CVKVC-",
            "C.....C",
            "V.BBB.V",
            "C.BSB.C",
            "V.BBB.V",
            "C.....C",
            "-CVCVC-"};
    /** Camadas de baixo (y = 0) para cima. */
    private static final String[][] LAYERS = {CAP, RING, CENTER, RING, CAP};

    private FusionStructure() {}

    /** Uma posição da estrutura com a peça esperada nela. */
    public record Slot(BlockPos pos, ReactorPart part) {}

    /**
     * Resultado da verificação.
     *
     * @param missing posições com a peça errada ou faltando (com a peça esperada)
     * @param members todas as posições ocupadas por peças (sem o canal vazio)
     * @param ports   posições das Portas do Reator
     */
    public record Result(boolean formed, List<Slot> missing, List<BlockPos> members, List<BlockPos> ports) {

        /** Quantas peças faltam de cada tipo. */
        public Map<ReactorPart, Integer> missingCounts() {
            Map<ReactorPart, Integer> counts = new EnumMap<>(ReactorPart.class);
            for (Slot slot : missing) counts.merge(slot.part(), 1, Integer::sum);
            return counts;
        }
    }

    /** Converte coordenadas locais em posição no mundo. {@code facing} é para onde o controlador olha (para fora). */
    public static BlockPos toWorld(BlockPos controller, Direction facing, int x, int y, int z) {
        Direction right = facing.getCounterClockWise();
        return controller.relative(right, x - CONTROLLER_X).relative(Direction.UP, y - CONTROLLER_Y).relative(facing.getOpposite(), z);
    }

    /** Centro da base da estrutura (para o modelo grande). */
    public static BlockPos baseCenter(BlockPos controller, Direction facing) {
        return toWorld(controller, facing, CONTROLLER_X, 0, SIZE / 2);
    }

    /** Caixa que envolve toda a estrutura (para a renderização não sumir quando o controlador sai da tela). */
    public static AABB bounds(BlockPos controller, Direction facing) {
        BlockPos a = toWorld(controller, facing, 0, 0, 0);
        BlockPos b = toWorld(controller, facing, SIZE - 1, HEIGHT - 1, SIZE - 1);
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    public static @Nullable ReactorPart partOf(BlockState state) {
        if (state.getBlock() instanceof ReactorPartBlock part) return part.part();
        if (state.getBlock() instanceof FusionReactorBlock) return ReactorPart.CONTROLLER;
        if (state.isAir()) return ReactorPart.AIR;
        return null;
    }

    private static @Nullable ReactorPart expected(char c) {
        return switch (c) {
            case 'C' -> ReactorPart.CASING;
            case 'V' -> ReactorPart.GLASS;
            case 'B' -> ReactorPart.COIL;
            case 'S' -> ReactorPart.SOLENOID;
            case 'K' -> ReactorPart.CONTROLLER;
            case '.' -> ReactorPart.AIR;
            default -> null;
        };
    }

    private static boolean accepts(char c, @Nullable ReactorPart actual) {
        if (actual == null) return false;
        return switch (c) {
            case 'C' -> actual == ReactorPart.CASING || actual == ReactorPart.PORT;
            case 'V' -> actual == ReactorPart.GLASS || actual == ReactorPart.CASING || actual == ReactorPart.PORT;
            default -> actual == expected(c);
        };
    }

    /** Todas as posições da estrutura com a peça "ideal" de cada uma (usado na construção automática dos testes). */
    public static List<Slot> blueprint(BlockPos controller, Direction facing) {
        List<Slot> slots = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    ReactorPart part = expected(LAYERS[y][z].charAt(x));
                    if (part != null) slots.add(new Slot(toWorld(controller, facing, x, y, z), part));
                }
            }
        }
        return slots;
    }

    public static Result check(BlockGetter level, BlockPos controller, Direction facing) {
        List<Slot> missing = new ArrayList<>();
        List<BlockPos> members = new ArrayList<>();
        List<BlockPos> ports = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    char c = LAYERS[y][z].charAt(x);
                    ReactorPart want = expected(c);
                    if (want == null) continue;
                    BlockPos pos = toWorld(controller, facing, x, y, z);
                    ReactorPart actual = partOf(level.getBlockState(pos));
                    if (!accepts(c, actual)) {
                        missing.add(new Slot(pos, want));
                        continue;
                    }
                    if (actual == ReactorPart.AIR) continue;
                    if (actual == ReactorPart.PORT) ports.add(pos);
                    if (actual != ReactorPart.CONTROLLER) members.add(pos);
                }
            }
        }
        return new Result(missing.isEmpty(), missing, members, ports);
    }
}
