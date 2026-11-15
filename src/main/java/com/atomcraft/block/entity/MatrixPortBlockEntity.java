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

/** Porta da Matriz de Energia: expõe a energia do controlador (só entrada ou só saída) para os vizinhos. */
public class MatrixPortBlockEntity extends BlockEntity {

    private @Nullable BlockPos controllerPos;

    public MatrixPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATRIX_PORT.get(), pos, state);
    }

    public boolean isOutput() {
        return getBlockState().hasProperty(MatrixPortBlock.OUTPUT) && getBlockState().getValue(MatrixPortBlock.OUTPUT);
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

    /** Capability de energia: entrada ou saída conforme o modo da porta. */
    public @Nullable EnergyHandler energy() {
        EnergyCellBlockEntity matrix = controller();
        if (matrix == null || !matrix.isEnabled()) return null;
        boolean output = isOutput();
        return new SidedEnergyHandler(matrix.energy(), !output, output);
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
