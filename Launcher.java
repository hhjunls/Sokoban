package org.csu;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.csu.controller.GameController;
import org.csu.model.api.Element;
import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;
import org.csu.model.base.Position;
import org.csu.model.impl.Box;
import org.csu.model.impl.Conveyor;
import org.csu.model.impl.Goal;
import org.csu.model.impl.Player;
import org.csu.model.impl.Wall;

/**
 * 游戏主类（Launcher）：组装 Model / View / Controller，
 * 并以 {@link AnimationTimer} 驱动"固定步长逻辑 + 逐帧渲染"的游戏主循环。
 * <p>
 * 固定步长说明：逻辑更新以 {@link #FIXED_STEP} 为最小步长推进，
 * 与显示器刷新率解耦；长时间卡顿时用 {@link #MAX_FRAME_TIME} 钳制单帧耗时，
 * 避免"死亡螺旋"式追帧。
 * <p>
 * 操作：方向键 / WASD 移动，R 重置关卡，N 下一关，Esc 退出。
 */
public class Launcher extends Application {

    /** 固定步长：每秒 60 个逻辑帧 */
    private static final double FIXED_STEP = 1.0 / 60.0;
    /** 单帧最大耗时上限（秒），防止长卡顿后疯狂追帧 */
    private static final double MAX_FRAME_TIME = 0.25;
    /** 每个格子的像素尺寸 */
    private static final double CELL_SIZE = 40;

    /** 关卡资源列表（resources 目录下），按顺序循环加载 */
    private static final String[] LEVEL_RESOURCES = {
            "/maps/1.txt",
            "/maps/2.txt",
            "/maps/3.txt"
    };

    // ---- 视图配色 ----
    private static final Color WALL_FILL = Color.web("#5a5a5a");
    private static final Color FLOOR_FILL = Color.web("#e8e0c9");
    private static final Color GRID_LINE = Color.web("#d5cbb0");
    private static final Color GOAL_MARK = Color.web("#d9534f");
    private static final Color CONVEYOR_FILL = Color.web("#7387c4");
    private static final Color ARROW_FILL = Color.web("#ffffff");
    private static final Color BOX_FILL = Color.web("#b8860b");
    private static final Color BOX_ON_GOAL_FILL = Color.web("#2e8b57");
    private static final Color PLAYER_FILL = Color.web("#2f6fed");

    private GameController controller;
    private Canvas canvas;
    private Stage stage;
    /** 当前关卡序号（从 0 开始） */
    private int currentLevelIndex;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // ---- 组装 MVC：Controller 通过回调驱动 View、退出 ----
        controller = new GameController(this::render, () -> Platform.exit());
        canvas = new Canvas(0, 0);

        // ---- 加载第一关并显示窗口 ----
        stage.setTitle("推箱子 Sokoban");
        stage.setResizable(false);
        loadLevel(0);
        stage.show();

        controller.startGame();
        startGameLoop();
    }

    /**
     * 加载指定序号的关卡（超出范围自动循环）：
     * 从 resources 读取关卡文件，按地图尺寸重建画布与场景。
     *
     * @param index 关卡序号（从 0 开始）
     */
    private void loadLevel(int index) {
        currentLevelIndex = Math.floorMod(index, LEVEL_RESOURCES.length);
        List<String> lines = readLevelLines(LEVEL_RESOURCES[currentLevelIndex]);
        controller.loadLevel(lines.toArray(new String[0]));

        GameMap map = controller.getMap();
        canvas.setWidth(map.getWidth() * CELL_SIZE);
        canvas.setHeight(map.getHeight() * CELL_SIZE);

        Scene scene = new Scene(new Pane(canvas), canvas.getWidth(), canvas.getHeight());
        scene.setOnKeyPressed(event -> handleKey(event.getCode()));
        stage.setScene(scene);
        if (stage.isShowing()) {
            stage.sizeToScene();
        }
    }

    /**
     * 读取关卡资源并规范化为纯关卡文本行。
     * <p>
     * 兼容两种写法：文件里带 Java 字符串包装（如 {@code "#######",}），
     * 或直接书写纯关卡文本行。
     *
     * @param resource classpath 资源路径（如 {@code /maps/1.txt}）
     * @return 规范化后的关卡文本行
     */
    static List<String> readLevelLines(String resource) {
        try (InputStream in = Launcher.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("找不到关卡资源: " + resource);
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<String> lines = new ArrayList<>();
            for (String raw : content.split("\\R")) {
                String line = normalizeLevelLine(raw);
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException("读取关卡资源失败: " + resource, e);
        }
    }

    /** 去除行首尾空白、结尾逗号与包裹引号，得到纯关卡行。 */
    private static String normalizeLevelLine(String raw) {
        String line = raw.strip();
        if (line.endsWith(",")) {
            line = line.substring(0, line.length() - 1).strip();
        }
        if (line.length() >= 2 && line.startsWith("\"") && line.endsWith("\"")) {
            line = line.substring(1, line.length() - 1);
        }
        return line;
    }

    /**
     * 键盘事件分发：方向键 / WASD 移动，R 重置，N 下一关，Esc 退出。
     */
    private void handleKey(KeyCode code) {
        switch (code) {
            case UP, W -> controller.onKeyPressed(Direction.UP);
            case DOWN, S -> controller.onKeyPressed(Direction.DOWN);
            case LEFT, A -> controller.onKeyPressed(Direction.LEFT);
            case RIGHT, D -> controller.onKeyPressed(Direction.RIGHT);
            case R -> controller.resetGame();
            case N -> loadLevel(currentLevelIndex + 1);
            case ESCAPE -> controller.quitGame();
            default -> { }
        }
    }

    /**
     * AnimationTimer 固定步长主循环：
     * 累积真实经过时间，按 {@link #FIXED_STEP} 逐帧推进逻辑更新；
     * 渲染每帧执行一次，由控制器脏标记决定是否真正重绘。
     */
    private void startGameLoop() {
        AnimationTimer timer = new AnimationTimer() {
            private long lastTime = 0;
            private double accumulator = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }
                double frameTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                frameTime = Math.min(frameTime, MAX_FRAME_TIME);
                accumulator += frameTime;

                // 固定步长逻辑帧：每个步长消费至多一个输入
                while (accumulator >= FIXED_STEP) {
                    controller.fixedUpdate(FIXED_STEP);
                    accumulator -= FIXED_STEP;
                }

                // 渲染帧
                controller.renderView();
            }
        };
        timer.start();
    }

    // ==================== 渲染 ====================

    /**
     * 视图渲染回调（由 GameController 调用）：绘制地形层、箱子层、玩家层，
     * 并同步窗口标题中的步数与胜利状态。
     */
    private void render(GameMap map) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 1. 地形层：墙 / 地板 / 目标点 / 传送带
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

        stage.setTitle("推箱子 Sokoban - 第 " + (currentLevelIndex + 1) + "/" + LEVEL_RESOURCES.length + " 关"
                + " - 步数: " + controller.getMoveCount()
                + (controller.isGameWin() ? " - 通关！按 N 进入下一关" : ""));
    }

    private void drawTerrain(GraphicsContext g, GameMap map, int x, int y) {
        Element cell = map.getCell(x, y);
        double px = x * CELL_SIZE;
        double py = y * CELL_SIZE;

        if (cell instanceof Wall) {
            g.setFill(WALL_FILL);
            g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            return;
        }
        // 地板底色与网格线
        g.setFill(FLOOR_FILL);
        g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
        g.setStroke(GRID_LINE);
        g.strokeRect(px + 0.5, py + 0.5, CELL_SIZE - 1, CELL_SIZE - 1);

        if (cell instanceof Goal) {
            // 目标点：中央圆点
            double d = CELL_SIZE * 0.26;
            g.setFill(GOAL_MARK);
            g.fillOval(px + (CELL_SIZE - d) / 2, py + (CELL_SIZE - d) / 2, d, d);
        } else if (cell instanceof Conveyor conveyor) {
            // 传送带：底色 + 方向箭头
            g.setFill(CONVEYOR_FILL);
            g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            drawArrow(g, px, py, conveyor.getDirection());
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

    private void drawBox(GraphicsContext g, Box box) {
        Position p = box.getPosition();
        double padding = CELL_SIZE * 0.12;
        g.setFill(box.isOnGoal() ? BOX_ON_GOAL_FILL : BOX_FILL);
        g.fillRoundRect(p.x() * CELL_SIZE + padding, p.y() * CELL_SIZE + padding,
                CELL_SIZE - 2 * padding, CELL_SIZE - 2 * padding, 8, 8);
    }

    private void drawPlayer(GraphicsContext g, Player player) {
        Position p = player.getPosition();
        double padding = CELL_SIZE * 0.15;
        g.setFill(PLAYER_FILL);
        g.fillOval(p.x() * CELL_SIZE + padding, p.y() * CELL_SIZE + padding,
                CELL_SIZE - 2 * padding, CELL_SIZE - 2 * padding);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
