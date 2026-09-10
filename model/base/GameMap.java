package org.csu.model.base;

import org.csu.model.api.Element;
import org.csu.model.impl.Box;
import org.csu.model.impl.Conveyor;
import org.csu.model.impl.Floor;
import org.csu.model.impl.Goal;
import org.csu.model.impl.Player;
import org.csu.model.impl.Wall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏地图。
 * <p>
 * 地图由两层组成：
 * <ul>
 *   <li>基础地形层 {@code cells}：墙、地板、目标点、传送带，每个格子必有其一；</li>
 *   <li>活动对象层：箱子列表 {@link #boxes} 与玩家 {@link #player}，
 *       它们的位置通过坐标实时查询。</li>
 * </ul>
 * 支持从文本关卡解析地图，符号约定见 {@link #parse(List)}。
 */
public class GameMap {

    /** 地图宽度（列数） */
    private final int width;
    /** 地图高度（行数） */
    private final int height;
    /** 基础地形网格，元素永不为 null */
    private final Element[][] cells;
    /** 地图中所有箱子 */
    private final List<Box> boxes = new ArrayList<>();
    /** 玩家 */
    private Player player;

    /**
     * 创建指定尺寸的空地图，默认所有格子为地板。
     */
    public GameMap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("地图宽高必须为正数");
        }
        this.width = width;
        this.height = height;
        this.cells = new Element[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = new Floor(new Position(x, y));
            }
        }
    }

    // ==================== 基础查询 ====================

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * 判断坐标是否在地图范围内。
     */
    public boolean isInBounds(Position p) {
        return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height;
    }

    /**
     * 获取某位置的基础地形元素；越界返回 null。
     */
    public Element getCell(Position p) {
        if (!isInBounds(p)) {
            return null;
        }
        return cells[p.y()][p.x()];
    }

    /**
     * 获取某位置的基础地形元素；越界返回 null。
     */
    public Element getCell(int x, int y) {
        return getCell(new Position(x, y));
    }

    /**
     * 设置某位置的基础地形元素（墙/地板/目标点/传送带）。
     */
    public void setCell(Element element) {
        Position p = element.getPosition();
        if (!isInBounds(p)) {
            throw new IllegalArgumentException("坐标超出地图范围: " + p);
        }
        cells[p.y()][p.x()] = element;
    }

    /**
     * 该位置是否为墙（越界同样视为不可通行）。
     */
    public boolean isWall(Position p) {
        Element cell = getCell(p);
        return cell == null || cell instanceof Wall;
    }

    /**
     * 该位置是否为目标点。
     */
    public boolean isGoal(Position p) {
        return getCell(p) instanceof Goal;
    }

    /**
     * 该位置是否完全空闲：不越界、不是墙、没有箱子、没有玩家。
     */
    public boolean isFree(Position p) {
        return isInBounds(p)
                && !isWall(p)
                && getBoxAt(p) == null
                && !p.equals(player.getPosition());
    }

    // ==================== 玩家 ====================

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    // ==================== 箱子 ====================

    /**
     * 返回所有箱子（不可修改视图）。
     */
    public List<Box> getBoxes() {
        return List.copyOf(boxes);
    }

    public void addBox(Box box) {
        boxes.add(box);
    }

    /**
     * 获取某位置的箱子；没有则返回 null。
     */
    public Box getBoxAt(Position p) {
        for (Box box : boxes) {
            if (box.getPosition().equals(p)) {
                return box;
            }
        }
        return null;
    }

    /**
     * 返回地图上所有目标点。
     */
    public List<Goal> getGoals() {
        List<Goal> goals = new ArrayList<>();
        for (Element[] row : cells) {
            for (Element cell : row) {
                if (cell instanceof Goal goal) {
                    goals.add(goal);
                }
            }
        }
        return goals;
    }

    // ==================== 关卡解析 ====================

    /**
     * 从关卡文本行解析地图。符号约定：
     * <ul>
     *   <li>'#' 墙；'-' 或 ' ' 地板；'.' 目标点；</li>
     *   <li>'$' 箱子；'*' 箱子在目标点上；</li>
     *   <li>'@' 玩家；'+' 玩家在目标点上；</li>
     *   <li>'^' / 'v' / '&lt;' / '&gt;' 传送带（方向：上/下/左/右）；</li>
     *   <li>字母 'A'~'Z' 联动箱子，相同字母的箱子相互联动。</li>
     * </ul>
     *
     * @param lines 每一行文本代表地图的一行
     * @return 解析完成的地图
     * @throws IllegalArgumentException 关卡文本为空、含非法字符或缺少玩家
     */
    public static GameMap parse(String... lines) {
        return parse(List.of(lines));
    }

    /**
     * 从关卡文本行列表解析地图，符号约定见 {@link #parse(String...)}。
     */
    public static GameMap parse(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("关卡文本不能为空");
        }
        int height = lines.size();
        int width = lines.stream().mapToInt(String::length).max().orElse(0);
        GameMap map = new GameMap(width, height);
        // 记录联动箱子的分组：字母 -> 该组箱子
        Map<Character, List<Box>> linkedGroups = new HashMap<>();

        for (int y = 0; y < height; y++) {
            String line = lines.get(y);
            for (int x = 0; x < line.length(); x++) {
                char ch = line.charAt(x);
                Position pos = new Position(x, y);
                switch (ch) {
                    case '#' -> map.setCell(new Wall(pos));
                    case '-', ' ' -> map.setCell(new Floor(pos));
                    case '.' -> map.setCell(new Goal(pos));
                    case '$' -> map.addBox(new Box(pos));
                    case '*' -> {
                        Box box = new Box(pos);
                        box.setOnGoal(true);
                        map.addBox(box);
                        map.setCell(new Goal(pos));
                    }
                    case '@' -> map.setPlayer(new Player(pos));
                    case '+' -> {
                        map.setPlayer(new Player(pos));
                        map.setCell(new Goal(pos));
                    }
                    case '^', 'v', '<', '>' ->
                            map.setCell(new Conveyor(pos, Direction.fromSymbol(ch)));
                    default -> {
                        if (ch >= 'A' && ch <= 'Z') {
                            Box box = new Box(pos);
                            linkedGroups.computeIfAbsent(ch, k -> new ArrayList<>()).add(box);
                            map.addBox(box);
                        } else {
                            throw new IllegalArgumentException(
                                    "第 " + (y + 1) + " 行含非法字符: " + ch);
                        }
                    }
                }
            }
        }

        // 建立联动关系：同一字母的箱子两两联动
        for (List<Box> group : linkedGroups.values()) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    group.get(i).linkWith(group.get(j));
                }
            }
        }

        if (map.player == null) {
            throw new IllegalArgumentException("关卡中缺少玩家 '@'");
        }
        return map;
    }
}
