package org.csu.model.api;

import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;

/**
 * 游戏逻辑接口：封装推箱子的核心玩法规则。
 * <p>
 * 职责包括：玩家移动、推箱、<b>箱子联动</b>、<b>传送带</b>结算、
 * 胜负判定与关卡重置。控制器层只需依赖本接口即可驱动整个游戏。
 */
public interface GameModel {

    /**
     * 玩家向指定方向移动一步。
     * <ul>
     *   <li>前方是地板/目标点/传送带：直接走过去；</li>
     *   <li>前方是箱子：尝试推动（接触链上的联动箱必须全部可推动时
     *       才会一起移动，否则原地不动）。</li>
     * </ul>
     * 注意：传送带独立于玩家移动运转，由控制器定时调用
     * {@link #stepConveyors()} 结算，本方法不触发传送。
     *
     * @param direction 移动方向
     * @return true 表示本次移动成功（位置发生了变化）
     */
    boolean movePlayer(Direction direction);

    /**
     * 结算传送带一次：所有位于传送带上的箱子沿各自传送方向移动一格。
     * 由控制器按固定时间间隔调用（与玩家是否移动无关），
     * 每次结算后视图逐格刷新。
     *
     * @return true 表示至少有一个箱子被传送
     */
    boolean stepConveyors();

    /**
     * 重置关卡：恢复玩家和所有箱子的初始位置，步数清零。
     */
    void reset();

    /**
     * 判断是否胜利：所有目标点都已被箱子覆盖。
     *
     * @return true 表示胜利
     */
    boolean isWin();

    /**
     * 返回当前步数（玩家有效移动的次数）。
     *
     * @return 已走步数
     */
    int getMoveCount();

    /**
     * 获取游戏地图，地图中可查询地形、玩家、箱子与目标点。
     *
     * @return 当前地图
     */
    GameMap getMap();
}
