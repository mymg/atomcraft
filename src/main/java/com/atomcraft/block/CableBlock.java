package com.atomcraft.block;

import com.atomcraft.storage.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cabo Atômico: liga máquinas e Drives numa mesma rede de elementos. */
public class CableBlock extends Block implements NetworkNode {

    public static final Map<Direction, BooleanProperty> PROPERTIES = new EnumMap<>(Map.of(
            Direction.NORTH, BlockStateProperties.NORTH,
            Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.EAST, BlockStateProperties.EAST,
            Direction.WEST, BlockStateProperties.WEST,
            Direction.UP, BlockStateProperties.UP,
            Direction.DOWN, BlockStateProperties.DOWN));

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.NORTH, Block.box(6, 6, 0, 10, 10, 5),
            Direction.SOUTH, Block.box(6, 6, 11, 10, 10, 16),
            Direction.WEST, Block.box(0, 6, 6, 5, 10, 10),
            Direction.EAST, Block.box(11, 6, 6, 16, 10, 10),
            Direction.DOWN, Block.box(6, 0, 6, 10, 5, 10),
            Direction.UP, Block.box(6, 11, 6, 10, 16, 10)));
    private final Map<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();

    public CableBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : PROPERTIES.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PROPERTIES.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockState neighbor = context.getLevel().getBlockState(context.getClickedPos().relative(direction));
            state = state.setValue(PROPERTIES.get(direction), connects(neighbor));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(PROPERTIES.get(direction), connects(neighborState));
    }

    private static boolean connects(BlockState state) {
        return state.getBlock() instanceof NetworkNode;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeCache.computeIfAbsent(state, s -> {
            VoxelShape shape = CORE;
            for (var entry : PROPERTIES.entrySet()) {
                if (s.getValue(entry.getValue())) shape = Shapes.or(shape, ARMS.get(entry.getKey()));
            }
            return shape;
        });
    }
}
