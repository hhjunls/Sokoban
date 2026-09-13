package org.csu.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;
import org.csu.model.base.Position;
import org.csu.model.impl.*;

/**
 * 游戏视图层：负责所有渲染逻辑。
 * 从 Launcher 中提取出来，通过回调方式接收地图数据并绘制。
 */
public class GameView {

    private static final double CELL_SIZE = 40;

    /** 纯色回退配色（图片加载失败时使用） */
    private static final Color WALL_FILL = Color.web("#5a5a5a");
    private static final Color FLOOR_FILL = Color.web("#e8e0c9");
    private static final Color GRID_LINE = Color.web("#d5cbb0");
    private static final Color GOAL_MARK = Color.web("#d9534f");
    private static final Color CONVEYOR_FILL = Color.web("#7387c4");
    private static final Color ARROW_FILL = Color.web("#ffffff");
    private static final Color BOX_FILL = Color.web("#b8860b");
    private static final Color BOX_ON_GOAL_FILL = Color.web("#2e8b57");
    private static final Color PLAYER_FILL = Color.web("#2f6fed");

    private final Canvas canvas;
    private final Stage stage;
    private final ImageLoader imageLoader;
    private int currentLevelIndex;
    private int totalLevels;

    public GameView(Canvas canvas, Stage stage) {
        this.canvas = canvas;
        this.stage = stage;
        this.imageLoader = ImageLoader.getInstance();
    }

    /**
     * 设置关卡信息（用于标题显示）
     */
    public void setLevelInfo(int currentLevelIndex, int totalLevels) {
        this.currentLevelIndex = currentLevelIndex;
        this.totalLevels = totalLevels;
    }

    /**
     * 渲染回调方法，由 GameController 调用。
     * 这是 view 层对外暴露的唯一接口，签名与 Consumer<GameMap> 兼容。
     */
    public void render(GameMap map) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 1. 地形层
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                drawTerrain(g, map, x, y);
            }
        }
        // 2. 箱子层
        for (Box box : map.getBoxes()) {
            drawBox(g, box);
        }
        // 3. 玩家层
        drawPlayer(g, map.getPlayer());

        // 4. 更新窗口标题
        updateTitle(map);
    }

    /**
     * 绘制地形（图片优先，失败回退纯色）
     */
    private void drawTerrain(GraphicsContext g, GameMap map, int x, int y) {
        var cell = map.getCell(x, y);
        double px = x * CELL_SIZE;
        double py = y * CELL_SIZE;

        if (cell instanceof Wall) {
            ImageLoader loader = imageLoader;
            var img = loader.get(ImageLoader.WALL);
            if (img != null) {
                g.drawImage(img, px, py, CELL_SIZE, CELL_SIZE);
            } else {
                g.setFill(WALL_FILL);
                g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            }
            return;
        }

        // 地板底色
        var floorImg = imageLoader.get(ImageLoader.FLOOR);
        if (floorImg != null) {
            g.drawImage(floorImg, px, py, CELL_SIZE, CELL_SIZE);
        } else {
            g.setFill(FLOOR_FILL);
            g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            g.setStroke(GRID_LINE);
            g.strokeRect(px + 0.5, py + 0.5, CELL_SIZE - 1, CELL_SIZE - 1);
        }

        if (cell instanceof Goal) {
            // 目标点：优先用图片，回退为红色圆点
            var goalImg = imageLoader.get(ImageLoader.GOAL);
            if (goalImg != null) {
                g.drawImage(goalImg, px, py, CELL_SIZE, CELL_SIZE);
            } else {
                double d = CELL_SIZE * 0.26;
                g.setFill(GOAL_MARK);
                g.fillOval(px + (CELL_SIZE - d) / 2, py + (CELL_SIZE - d) / 2, d, d);
            }
        } else if (cell instanceof Conveyor conveyor) {
            // 传送带：优先用方向图片，回退为纯色+箭头
            var convImg = imageLoader.getConveyorImage(conveyor.getDirection());
            if (convImg != null) {
                g.drawImage(convImg, px, py, CELL_SIZE, CELL_SIZE);
            } else {
                g.setFill(CONVEYOR_FILL);
                g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                drawArrow(g, px, py, conveyor.getDirection());
            }
        }
    }

    private void drawArrow(GraphicsContext g, double px, double py, Direction direction) {
        double cx = px + CELL_SIZE / 2;
        double cy = py + CELL_SIZE / 2;
        double r = CELL_SIZE * 0.26;
        double[][] vertices = switch (direction) {
            case UP -> new double[][]{
                    {cx, cx - r, cx + r}, {cy - r, cy + r, cy + r}};
            case DOWN -> new double[][]{
                    {cx, cx - r, cx + r}, {cy + r, cy - r, cy - r}};
            case LEFT -> new double[][]{
                    {cx - r, cx + r, cx + r}, {cy, cy - r, cy + r}};
            case RIGHT -> new double[][]{
                    {cx + r, cx - r, cx - r}, {cy, cy - r, cy + r}};
        };
        g.setFill(ARROW_FILL);
        g.fillPolygon(vertices[0], vertices[1], 3);
    }

    /**
     * 绘制箱子（图片优先）
     */
    private void drawBox(GraphicsContext g, Box box) {
        Position p = box.getPosition();
        double px = p.x() * CELL_SIZE;
        double py = p.y() * CELL_SIZE;

        String imgKey = box.isOnGoal() ? ImageLoader.BOX_ON_GOAL : ImageLoader.BOX;
        var img = imageLoader.get(imgKey);

        if (img != null) {
            g.drawImage(img, px, py, CELL_SIZE, CELL_SIZE);
        } else {
            // 回退：圆角矩形
            double padding = CELL_SIZE * 0.12;
            g.setFill(box.isOnGoal() ? BOX_ON_GOAL_FILL : BOX_FILL);
            g.fillRoundRect(px + padding, py + padding,
                    CELL_SIZE - 2 * padding, CELL_SIZE - 2 * padding, 8, 8);
        }
    }

    /**
     * 绘制玩家（图片优先）
     */
    private void drawPlayer(GraphicsContext g, Player player) {
        Position p = player.getPosition();
        double px = p.x() * CELL_SIZE;
        double py = p.y() * CELL_SIZE;

        var img = imageLoader.get(ImageLoader.PLAYER);
        if (img != null) {
            g.drawImage(img, px, py, CELL_SIZE, CELL_SIZE);
        } else {
            // 回退：蓝色圆
            double padding = CELL_SIZE * 0.15;
            g.setFill(PLAYER_FILL);
            g.fillOval(px + padding, py + padding,
                    CELL_SIZE - 2 * padding, CELL_SIZE - 2 * padding);
        }
    }

    /**
     * 更新窗口标题
     */
    private void updateTitle(GameMap map) {
        // 注意：这里需要从外部获取步数和胜利状态
        // 暂时只显示基本信息，步数由 Launcher 传入或后续通过接口获取
        if (stage != null) {
            stage.setTitle("推箱子 Sokoban - 第 " + (currentLevelIndex + 1)
                    + "/" + totalLevels + " 关");
        }
    }
}