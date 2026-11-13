package com.atomcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Máquina de 2 blocos de altura com modelo animado (GeckoLib): Analisador, Atomizador e
 * Replicador. Funciona como uma porta: a metade de baixo guarda a máquina (block entity),
 * desenha o modelo inteiro e é a única que conecta em cabos, tubos e afins; a de cima só
 * ocupa o espaço e repassa os cliques para a de baixo.
 */
public abstract class TallMachineBlock extends MachineBlock {

    /** Trabalhando (troca a animação de "idle" para a de trabalho). */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    protected TallMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE, HALF);
    }

    public static boolean isUpper(BlockState state) {
        return state.hasProperty(HALF) && state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    /** Só coloca se houver espaço livre em cima. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        Level level = context.getLevel();
        if (above.getY() > level.getMaxY() || !level.getBlockState(above).canBeReplaced(context)) return null;
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER).setValue(ACTIVE, false), Block.UPDATE_ALL);
    }

    /** Coloca as duas metades (usado pela vitrine e pelos testes). */
    public static void place(Level level, BlockPos pos, BlockState lower) {
        level.setBlock(pos, lower.setValue(HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
        level.setBlock(pos.above(), lower.setValue(HALF, DoubleBlockHalf.UPPER).setValue(ACTIVE, false), Block.UPDATE_ALL);
    }

    /** A metade de cima some se a de baixo sair. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (isUpper(state) && direction == Direction.DOWN
                && !(neighborState.is(this) && !isUpper(neighborState))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** Quebrar a metade de cima quebra a máquina (que solta o item e o conteúdo). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && isUpper(state)) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this) && !isUpper(lower)) {
                if (player.isCreative()) {
                    level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                } else {
                    level.destroyBlock(below, true, player);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (isUpper(state)) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            return lower.is(this) ? super.useWithoutItem(lower, level, below, player, hit) : InteractionResult.PASS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    /** O modelo animado é desenhado pelo GeckoLib. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Só a metade de baixo conecta na rede de elementos. */
    @Override
    public boolean isNetworkNode(BlockState state) {
        return !isUpper(state);
    }

    /** A block entity fica só na metade de baixo. */
    protected abstract BlockEntity createMachine(BlockPos pos, BlockState state);

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isUpper(state) ? null : createMachine(pos, state);
    }
}
