package org.csu.model.base;

/**
 * 移动方向枚举，每个方向携带对应的坐标增量。
 */
public enum Direction {

    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    /** 水平方向增量（x 轴） */
    private final int dx;
    /** 垂直方向增量（y 轴） */
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    /**
     * 返回相反方向。
     *
     * @return 与本方向相反的方向
     */
    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    /**
     * 根据关卡文本中的传送带符号解析方向。
     *
     * @param symbol 传送带符号：'^' 上、'v' 下、'&lt;' 左、'&gt;' 右
     * @return 对应的方向
     * @throws IllegalArgumentException 符号不是合法的传送带方向
     */
    public static Direction fromSymbol(char symbol) {
        return switch (symbol) {
            case '^' -> UP;
            case 'v' -> DOWN;
            case '<' -> LEFT;
            case '>' -> RIGHT;
            default -> throw new IllegalArgumentException("非法的传送带方向符号: " + symbol);
        };
    }
}
