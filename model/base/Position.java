package org.csu.model.base;

/**
 * 地图坐标，不可变对象。
 *
 * @param x 列坐标（从 0 开始）
 * @param y 行坐标（从 0 开始）
 */
public record Position(int x, int y) {

    /**
     * 沿指定方向移动一步，返回新坐标（不改变当前对象）。
     *
     * @param direction 移动方向
     * @return 移动后的新坐标
     */
    public Position move(Direction direction) {
        return new Position(x + direction.getDx(), y + direction.getDy());
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
