package org.csu.model.impl;

import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;
import org.csu.model.base.Position;
import org.csu.model.api.Movable;

/**
 * 玩家：由输入驱动移动的角色，可以推动箱子。
 */
public class Player implements Movable {

    private Position position;

    public Player(Position position) {
        this.position = position;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public char symbol() {
        return '@';
    }

    /**
     * 玩家能否走到目标格：不越界、不是墙、且该格没有箱子。
     */
    @Override
    public boolean canMove(Direction direction, GameMap map) {
        Position target = position.move(direction);
        return map.isInBounds(target)
                && !map.isWall(target)
                && map.getBoxAt(target) == null;
    }

    @Override
    public void move(Direction direction, GameMap map) {
        position = position.move(direction);
    }

    @Override
    public String toString() {
        return "玩家" + position;
    }
}
