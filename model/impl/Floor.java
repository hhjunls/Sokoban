package org.csu.model.impl;

import org.csu.model.base.Position;
import org.csu.model.api.Element;

/**
 * 地板：可通行的基础地形。
 */
public class Floor implements Element {

    private Position position;

    public Floor(Position position) {
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
        return false;
    }

    @Override
    public boolean isWalkable() {
        return true;
    }

    @Override
    public char symbol() {
        return '-';
    }

    @Override
    public String toString() {
        return "地板" + position;
    }
}
