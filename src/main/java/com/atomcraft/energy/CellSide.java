package com.atomcraft.energy;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/** Lados da célula relativos à frente do bloco (assim a configuração não muda ao girar). */
public enum CellSide {
    FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM;

    private static final CellSide[] VALUES = values();

    /** Direção no mundo deste lado, para um bloco virado para {@code facing}. */
    public Direction toDirection(Direction facing) {
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    public static CellSide fromDirection(Direction facing, Direction direction) {
        for (CellSide side : VALUES) if (side.toDirection(facing) == direction) return side;
        return FRONT;
    }

    public Component label() {
        return Component.translatable("cell_side.atomcraft." + name().toLowerCase());
    }

    public static CellSide byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : FRONT;
    }
}
