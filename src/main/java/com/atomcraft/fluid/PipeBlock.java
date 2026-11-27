package com.atomcraft.fluid;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cano de Fluido (4 níveis). Liga-se a canos do mesmo nível e a qualquer bloco com fluido (tanques,
 * bombas, máquinas de outros mods). Cada lado ligado a um bloco entrega fluido a ele; com agachado + mão
 * vazia no lado, o modo vira "puxar" (tira fluido do bloco) e depois "desligado".
 */
public class PipeBlock extends BaseEntityBlock {

    public static final Map<Direction, EnumProperty<PipeConnection>> PROPERTIES = new EnumMap<>(Direction.class);

    static {
        for (Direction direction : Direction.values()) {
            PROPERTIES.put(direction, EnumProperty.create(direction.getSerializedName(), PipeConnection.class));
        }
    }

    // Mesmas medidas do modelo: núcleo de 5 px, braço de 4 px com o aro de 5 px na ponta
    private static final VoxelShape CORE = Block.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.NORTH, Shapes.or(Block.box(6, 6, 0, 10, 10, 5.5), Block.box(5.5, 5.5, 0, 10.5, 10.5, 0.5)),
            Direction.SOUTH, Shapes.or(Block.box(6, 6, 10.5, 10, 10, 16), Block.box(5.5, 5.5, 15.5, 10.5, 10.5, 16)),
            Direction.WEST, Shapes.or(Block.box(0, 6, 6, 5.5, 10, 10), Block.box(0, 5.5, 5.5, 0.5, 10.5, 10.5)),
            Direction.EAST, Shapes.or(Block.box(10.5, 6, 6, 16, 10, 10), Block.box(15.5, 5.5, 5.5, 16, 10.5, 10.5)),
            Direction.DOWN, Shapes.or(Block.box(6, 0, 6, 10, 5.5, 10), Block.box(5.5, 0, 5.5, 10.5, 0.5, 10.5)),
            Direction.UP, Shapes.or(Block.box(6, 10.5, 6, 10, 16, 10), Block.box(5.5, 15.5, 5.5, 10.5, 16, 10.5))));
    private final Map<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();

    private final FluidTier tier;

    public PipeBlock(FluidTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        BlockState state = stateDefinition.any();
        for (EnumProperty<PipeConnection> property : PROPERTIES.values()) state = state.setValue(property, PipeConnection.NONE);
        registerDefaultState(state);
    }

    public FluidTier tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new PipeBlock(tier, properties));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PROPERTIES.values().forEach(builder::add);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static PipeConnection side(BlockState state, Direction direction) {
        return state.getValue(PROPERTIES.get(direction));
    }

    // ----- Conexões -----

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    private BlockState connections(LevelReader level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTIES.get(direction), connectionFor(level, pos, direction, side(state, direction)));
        }
        return state;
    }

    /** Como fica o lado {@code direction}, mantendo o que o jogador escolheu (puxar / desligado). */
    private PipeConnection connectionFor(LevelReader level, BlockPos pos, Direction direction, PipeConnection current) {
        if (current == PipeConnection.DISABLED) return PipeConnection.DISABLED;
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof PipeBlock pipe) {
            if (pipe.tier != tier) return PipeConnection.NONE;
            return side(neighbor, direction.getOpposite()) == PipeConnection.DISABLED ? PipeConnection.NONE : PipeConnection.NORMAL;
        }
        if (level instanceof Level world && world.getCapability(Capabilities.Fluid.BLOCK, neighborPos, direction.getOpposite()) != null) {
            return current == PipeConnection.PULL ? PipeConnection.PULL : PipeConnection.NORMAL;
        }
        return PipeConnection.NONE;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(PROPERTIES.get(direction), connectionFor(level, pos, direction, side(state, direction)));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   net.minecraft.world.level.redstone.@Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide()) return;
        BlockState updated = connections(level, pos, state);
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_ALL);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide()) return;
        PipeNetwork.invalidateAround(level, pos);
        if (oldState.getBlock() != this) {
            // Colocado por código (estrutura, comando): calcula as conexões agora
            BlockState updated = connections(level, pos, state);
            if (updated != state) level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    // ----- Interação: agachado + mão vazia troca o modo do lado clicado -----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction direction = clickedSide(pos, hit);
        if (!player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.atomcraft.pipe_info",
                        Component.translatable("pipe_mode.atomcraft." + side(state, direction).getSerializedName()),
                        Component.translatable("direction.atomcraft." + direction.getSerializedName()),
                        String.format("%,d", tier.pipeRate())).withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        PipeConnection current = side(state, direction);
        boolean pipeNeighbor = level.getBlockState(pos.relative(direction)).getBlock() instanceof PipeBlock;
        PipeConnection next = switch (current) {
            case NORMAL -> pipeNeighbor ? PipeConnection.DISABLED : PipeConnection.PULL;
            case PULL, NONE -> PipeConnection.DISABLED;
            case DISABLED -> PipeConnection.NORMAL;
        };
        BlockState updated = state.setValue(PROPERTIES.get(direction), next);
        if (next == PipeConnection.NORMAL) {
            updated = updated.setValue(PROPERTIES.get(direction), connectionFor(level, pos, direction, PipeConnection.NORMAL));
        }
        level.setBlock(pos, updated, Block.UPDATE_ALL);
        // O cano vizinho também precisa reconectar (ou desconectar)
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof PipeBlock neighborPipe) {
            BlockState refreshed = neighborPipe.connections(level, neighborPos, neighbor);
            if (refreshed != neighbor) level.setBlock(neighborPos, refreshed, Block.UPDATE_ALL);
        }
        player.sendOverlayMessage(Component.translatable("message.atomcraft.pipe_mode",
                Component.translatable("direction.atomcraft." + direction.getSerializedName()),
                Component.translatable("pipe_mode.atomcraft." + side(updated, direction).getSerializedName())).withStyle(ChatFormatting.AQUA));
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    /** Lado do cano que o jogador mirou: o braço sob a mira, ou a face clicada do núcleo. */
    public static Direction clickedSide(BlockPos pos, BlockHitResult hit) {
        Vec3 rel = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        double ax = Math.abs(rel.x), ay = Math.abs(rel.y), az = Math.abs(rel.z);
        double max = Math.max(ax, Math.max(ay, az));
        if (max < 0.16) return hit.getDirection();
        if (max == ax) return rel.x > 0 ? Direction.EAST : Direction.WEST;
        if (max == ay) return rel.y > 0 ? Direction.UP : Direction.DOWN;
        return rel.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeCache.computeIfAbsent(state, s -> {
            VoxelShape shape = CORE;
            for (Direction direction : Direction.values()) {
                if (side(s, direction).hasArm()) shape = Shapes.or(shape, ARMS.get(direction));
            }
            return shape;
        });
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof PipeBlockEntity pipe) pipe.serverTick();
        };
    }
}
