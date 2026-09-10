package org.csu.model.api;

import org.csu.model.base.Position;

/**
 * 地图元素的统一接口。
 * <p>
 * 所有放置在地图上的对象（墙、地板、目标点、传送带、箱子、玩家）
 * 都必须实现该接口，以便游戏模型统一处理。
 */
public interface Element {

    /**
     * 返回元素当前坐标。
     *
     * @return 元素所在位置
     */
    Position getPosition();

    /**
     * 设置元素坐标。
     *
     * @param position 新坐标
     */
    void setPosition(Position position);

    /**
     * 该元素是否阻碍移动（墙、箱子、玩家返回 true）。
     *
     * @return true 表示不可穿过
     */
    boolean isSolid();

    /**
     * 玩家是否可以站立在该元素上（地板、目标点、传送带返回 true）。
     *
     * @return true 表示可站立
     */
    boolean isWalkable();

    /**
     * 关卡文本中代表该元素的符号。
     *
     * @return 元素符号
     */
    char symbol();
}
