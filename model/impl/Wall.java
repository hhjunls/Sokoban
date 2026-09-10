package org.csu.model.impl;

import org.csu.model.base.Position;
import org.csu.model.api.Element;

/**
 * 墙：不可通行、不可推动的地形元素。
 */
public class Wall implements Element {

    private Position position;

    public Wall(Position position) {
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
        return '#';
    }

    @Override
    public String toString() {
        return "墙" + position;
    }
}
