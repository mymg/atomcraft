package com.atomcraft.block.entity;

import com.atomcraft.block.MatrixPortBlock;
import com.atomcraft.energy.SidedEnergyHandler;
import com.atomcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/** Interface da Matriz de Energia: expõe a energia do controlador para os vizinhos (entrada, saída ou os dois). */
public class MatrixPortBlockEntity extends BlockEntity implements com.atomcraft.storage.NetworkLink {

    private @Nullable BlockPos controllerPos;

    public MatrixPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATRIX_PORT.get(), pos, state);
    }

    public com.atomcraft.energy.PortMode mode() {
        return getBlockState().hasProperty(MatrixPortBlock.MODE) ? getBlockState().getValue(MatrixPortBlock.MODE) : com.atomcraft.energy.PortMode.BOTH;
    }

    /** O controlador lembra cada porta quando verifica a estrutura. */
    public void setController(@Nullable BlockPos pos) {
        if (pos == null ? controllerPos == null : pos.equals(controllerPos)) return;
        controllerPos = pos == null ? null : pos.immutable();
        setChanged();
    }

    /** Controlador formado desta porta, ou null. */
    public @Nullable EnergyCellBlockEntity controller() {
        if (level == null || controllerPos == null) return null;
        if (level.getBlockEntity(controllerPos) instanceof EnergyCellBlockEntity matrix && matrix.isFormed()
                && matrix.ports().contains(worldPosition)) {
            return matrix;
        }
        return null;
    }

    @Override
    public java.util.Collection<BlockPos> linkedNodes() {
        EnergyCellBlockEntity matrix = controller();
        return matrix == null ? java.util.List.of() : java.util.List.of(matrix.getBlockPos());
    }

    /** Capability de energia: entrada ou saída conforme o modo da porta. */
    public @Nullable EnergyHandler energy() {
        EnergyCellBlockEntity matrix = controller();
        if (matrix == null || !matrix.isEnabled()) return null;
        com.atomcraft.energy.PortMode mode = mode();
        return new SidedEnergyHandler(matrix.energy(), mode.receives(), mode.sends());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        controllerPos = input.read("controller", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (controllerPos != null) output.store("controller", BlockPos.CODEC, controllerPos);
    }
}
