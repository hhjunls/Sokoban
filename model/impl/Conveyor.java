package org.csu.model.impl;

import org.csu.model.base.Direction;
import org.csu.model.base.Position;

/**
 * 传送带：可通行的特殊地板。
 * <p>
 * 每次玩家成功移动后，站在其上的箱子会沿 {@link #getDirection() 传送方向}
 * 自动移动一格（玩家不受传送带影响）；箱子被传送到另一条传送带上时
 * 会继续传导，直到静止。
 */
public class Conveyor extends Floor {

    /** 传送方向 */
    private final Direction direction;

    public Conveyor(Position position, Direction direction) {
        super(position);
        this.direction = direction;
    }

    /**
     * 返回传送方向。
     *
     * @return 箱子被传送的方向
     */
    public Direction getDirection() {
        return direction;
    }

    @Override
    public char symbol() {
        return switch (direction) {
            case UP -> '^';
            case DOWN -> 'v';
            case LEFT -> '<';
            case RIGHT -> '>';
        };
    }

    @Override
    public String toString() {
        return "传送带" + getPosition() + "(" + direction + ")";
    }
}
