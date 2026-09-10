package org.csu.model.impl;

import org.csu.model.base.Position;
import org.csu.model.api.Element;

/**
 * 目标点：箱子需要被推到其上的位置，本身可通行。
 */
public class Goal implements Element {

    private Position position;

    public Goal(Position position) {
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
        return '.';
    }

    @Override
    public String toString() {
        return "目标点" + position;
    }
}
