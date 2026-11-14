package com.atomcraft.block;

import com.atomcraft.replication.QuantumPart;
import net.minecraft.world.level.block.Block;

/** Peça da estrutura do Replicador Quântico (carcaça, vidro ou núcleo). */
public class QuantumPartBlock extends Block {

    private final QuantumPart part;

    public QuantumPartBlock(QuantumPart part, Properties properties) {
        super(properties);
        this.part = part;
    }

    public QuantumPart part() {
        return part;
    }
}
