package org.csu.model.impl;

import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;
import org.csu.model.base.Position;
import org.csu.model.api.GameModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 推箱子游戏模型实现类，实现 {@link GameModel} 接口。
 * <p>
 * 核心规则：
 * <ul>
 *   <li><b>玩家移动</b>：目标格是地板/目标点/传送带可直接走；
 *       目标格是箱子则尝试推动，否则原地不动。</li>
 *   <li><b>箱子联动</b>：相同字母的箱子相互接触（上下左右相邻）后才联动，
 *       推动时接触链上的箱子一起移动，只有整组全部可推动时推动才会成功
 *       （先校验后移动，原子操作）。</li>
 *   <li><b>传送带</b>：独立于玩家移动运转，由控制器按固定时间间隔
 *       调用 {@link #stepConveyors()} 结算，箱子每次移动一格
 *       （一格一格移动，不连续传导）。</li>
 *   <li><b>胜利条件</b>：所有目标点都被箱子覆盖。</li>
 * </ul>
 */
public class SokobanGameModel implements GameModel {

    /** 当前地图 */
    private final GameMap map;
    /** 已走步数 */
    private int moveCount;
    /** 玩家初始位置，用于重置 */
    private Position playerStart;
    /** 箱子初始位置，用于重置 */
    private final Map<Box, Position> boxStarts = new HashMap<>();

    public SokobanGameModel(GameMap map) {
        this.map = map;
        snapshot();
    }

    /**
     * 便捷构造：直接从关卡文本行构建游戏。
     *
     * @param levelLines 关卡文本，符号约定见 {@link GameMap#parse(String...)}
     */
    public SokobanGameModel(String... levelLines) {
        this(GameMap.parse(levelLines));
    }

    /** 记录初始状态，供 {@link #reset()} 恢复。 */
    private void snapshot() {
        playerStart = map.getPlayer().getPosition();
        boxStarts.clear();
        for (Box box : map.getBoxes()) {
            boxStarts.put(box, box.getPosition());
        }
    }

    @Override
    public boolean movePlayer(Direction direction) {
        Player player = map.getPlayer();
        Position target = player.getPosition().move(direction);
        if (!map.isInBounds(target) || map.isWall(target)) {
            return false;
        }

        Box boxAtTarget = map.getBoxAt(target);
        if (boxAtTarget != null) {
            // 尝试推动整个联动箱子组
            Set<Box> group = boxAtTarget.collectLinkedGroup();
            if (!canPushGroup(group, direction)) {
                return false;
            }
            pushGroup(group, direction);
        }

        player.move(direction, map);
        moveCount++;
        return true;
    }

    /**
     * 判断整组联动箱子能否同时沿指定方向推动：
     * 每个箱子的目标格必须不越界、不是墙、没有玩家，
     * 且各组员的目标格互不重合、目标格上若有箱子必须是本组成员。
     */
    private boolean canPushGroup(Set<Box> group, Direction direction) {
        Player player = map.getPlayer();
        Set<Position> targets = new HashSet<>();
        for (Box box : group) {
            Position target = box.getPosition().move(direction);
            if (!map.isInBounds(target)
                    || map.isWall(target)
                    || target.equals(player.getPosition())) {
                return false;
            }
            // 两个组员的目标重合说明会互相碰撞
            if (!targets.add(target)) {
                return false;
            }
        }
        // 目标格要么为空，要么被本组其他箱子占据（该箱子会一起移走）
        for (Position target : targets) {
            Box occupant = map.getBoxAt(target);
            if (occupant != null && !group.contains(occupant)) {
                return false;
            }
        }
        return true;
    }

    /** 同时推动整组联动箱子（所有组员移动一格）。 */
    private void pushGroup(Set<Box> group, Direction direction) {
        for (Box box : group) {
            box.push(direction, map);
        }
    }

    /**
     * 结算传送带：所有位于传送带上的箱子沿传送方向移动一格。
     * 由控制器按固定间隔调用，独立于玩家移动；
     * 箱子每次只移动一格，不连续传导。
     *
     * @return true 表示至少有一个箱子被传送
     */
    @Override
    public boolean stepConveyors() {
        boolean moved = false;
        for (Box box : map.getBoxes()) {
            if (map.getCell(box.getPosition()) instanceof Conveyor conveyor) {
                Direction dir = conveyor.getDirection();
                Position target = box.getPosition().move(dir);
                if (map.isFree(target)) {
                    box.push(dir, map);
                    moved = true;
                }
            }
        }
        return moved;
    }

    @Override
    public void reset() {
        map.getPlayer().setPosition(playerStart);
        for (Map.Entry<Box, Position> entry : boxStarts.entrySet()) {
            Box box = entry.getKey();
            box.setPosition(entry.getValue());
            box.setOnGoal(map.isGoal(box.getPosition()));
        }
        moveCount = 0;
    }

    @Override
    public boolean isWin() {
        for (Goal goal : map.getGoals()) {
            if (map.getBoxAt(goal.getPosition()) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getMoveCount() {
        return moveCount;
    }

    @Override
    public GameMap getMap() {
        return map;
    }
}
