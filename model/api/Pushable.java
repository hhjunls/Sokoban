package org.csu.model.api;

import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;

/**
 * 可被推动元素的接口：玩家能够推动的对象（如箱子）。
 */
public interface Pushable extends Movable {

    /**
     * 判断能否沿指定方向被推动一格。
     *
     * @param direction 推动方向
     * @param map       当前地图
     * @return true 表示可以被推动
     */
    boolean canBePushed(Direction direction, GameMap map);

    /**
     * 沿指定方向被推动一格（调用前应先用 {@link #canBePushed} 校验）。
     *
     * @param direction 推动方向
     * @param map       当前地图
     */
    void push(Direction direction, GameMap map);
}
