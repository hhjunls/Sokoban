package org.csu.model.impl;

import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;
import org.csu.model.base.Position;
import org.csu.model.api.Pushable;

import java.util.HashSet;
import java.util.Set;

/**
 * 箱子：可被推动的游戏元素，支持<b>箱子联动</b>。
 * <p>
 * 联动规则：通过 {@link #linkWith(Box)} 关联的箱子（相同字母）在
 * <b>相互接触（上下左右相邻）后</b>才联动：推动其中一个箱子时，
 * 与它接触的联动箱（以及通过接触链相连的联动箱）一起移动；
 * 只有整组箱子全部可推动时，推动才会成功。
 * <p>
 * 注意：联动关系只作用于玩家推动；传送带只移动其上的单个箱子，
 * 不带动整组。
 */
public class Box implements Pushable {

    private Position position;

    /** 是否停在目标点上 */
    private boolean onGoal;

    /** 与本箱子直接联动的其他箱子 */
    private final Set<Box> linkedBoxes = new HashSet<>();

    public Box(Position position) {
        this.position = position;
    }

    // ==================== 箱子联动 ====================

    /**
     * 与另一个箱子建立双向联动关系。
     *
     * @param other 要联动的箱子
     */
    public void linkWith(Box other) {
        if (other == this) {
            return;
        }
        linkedBoxes.add(other);
        other.linkedBoxes.add(this);
    }

    /**
     * 断开与另一个箱子的联动关系。
     *
     * @param other 要解除联动的箱子
     */
    public void unlink(Box other) {
        linkedBoxes.remove(other);
        other.linkedBoxes.remove(this);
    }

    /**
     * 返回与本箱子直接联动的所有箱子。
     *
     * @return 直接联动的箱子集合（不可修改视图）
     */
    public Set<Box> getLinkedBoxes() {
        return Set.copyOf(linkedBoxes);
    }

    /**
     * 深度优先收集与被推箱子接触的联动组（包括自身）：
     * 只有相互接触（上下左右相邻）的联动箱才会被带动，
     * 接触链之外的联动箱保持原地不动。
     *
     * @return 本次推动会一起移动的所有箱子
     */
    public Set<Box> collectLinkedGroup() {
        Set<Box> group = new HashSet<>();
        collectLinkedGroup(group);
        return group;
    }

    private void collectLinkedGroup(Set<Box> visited) {
        if (!visited.add(this)) {
            return;
        }
        for (Box box : linkedBoxes) {
            if (isTouching(box)) {
                box.collectLinkedGroup(visited);
            }
        }
    }

    /**
     * 两个箱子是否相互接触（上下左右相邻一格）。
     */
    private boolean isTouching(Box other) {
        return Math.abs(position.x() - other.position.x())
                + Math.abs(position.y() - other.position.y()) == 1;
    }

    // ==================== 位置与状态 ====================

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * 箱子当前是否停在目标点上。
     */
    public boolean isOnGoal() {
        return onGoal;
    }

    public void setOnGoal(boolean onGoal) {
        this.onGoal = onGoal;
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
        return '$';
    }

    // ==================== 移动规则 ====================

    /**
     * 单个箱子能否被推动：目标格不越界、不是墙、没有玩家、没有其他箱子。
     * 联动组的整体校验由游戏模型负责。
     */
    @Override
    public boolean canBePushed(Direction direction, GameMap map) {
        Position target = position.move(direction);
        return map.isInBounds(target)
                && !map.isWall(target)
                && !target.equals(map.getPlayer().getPosition())
                && map.getBoxAt(target) == null;
    }

    /**
     * 推动单个箱子一格，并同步更新是否停在目标点上。
     * 联动组的整体推动由游戏模型负责。
     */
    @Override
    public void push(Direction direction, GameMap map) {
        position = position.move(direction);
        onGoal = map.isGoal(position);
    }

    /** 箱子自身不会主动移动，可移动性等价于可被推动性。 */
    @Override
    public boolean canMove(Direction direction, GameMap map) {
        return canBePushed(direction, map);
    }

    @Override
    public void move(Direction direction, GameMap map) {
        push(direction, map);
    }

    @Override
    public String toString() {
        return "箱子" + position + (onGoal ? "(在目标点)" : "");
    }
}
