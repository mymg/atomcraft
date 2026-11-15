package com.atomcraft.block;

import com.atomcraft.energy.MatrixPart;
import net.minecraft.world.level.block.Block;

/** Peça da Matriz de Energia (carcaça, vidro, núcleo ou porta). */
public class MatrixPartBlock extends Block {

    private final MatrixPart part;

    public MatrixPartBlock(MatrixPart part, Properties properties) {
        super(properties);
        this.part = part;
    }

    public MatrixPart part() {
        return part;
    }
}
