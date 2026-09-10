package org.csu.model.api;

import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;

/**
 * 可移动元素的接口：能够在地图上自主移动的对象（如玩家）。
 */
public interface Movable extends Element {

    /**
     * 判断能否沿指定方向移动一步。
     *
     * @param direction 移动方向
     * @param map       当前地图
     * @return true 表示可以移动
     */
    boolean canMove(Direction direction, GameMap map);

    /**
     * 沿指定方向移动一步（调用前应先用 {@link #canMove} 校验）。
     *
     * @param direction 移动方向
     * @param map       当前地图
     */
    void move(Direction direction, GameMap map);
}
