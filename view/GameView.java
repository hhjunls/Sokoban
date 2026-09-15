package org.csu.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
 * 游戏视图（View 层）：地图渲染与界面展示。
 * <p>
 * 布局：顶部状态栏（关卡选择按钮 / 步数）、中央 {@link Canvas} 地图画布
 * （通关时叠加深色横幅）、底部操作提示与按钮。
 * <p>
 * 与 Controller 解耦：本类不直接调用 Controller，按钮动作通过构造参数
 * {@link Actions} 在组装 MVC 时注入（见 Launcher）。
 * <p>
 * 地图绘制采用“图片优先、纯色回退”策略：resources/images/ 下的图片素材
 * 加载成功时绘制图片，缺失或加载失败时退回内置纯色绘制（见 {@link ImageLoader}）。
 */
public class GameView extends BorderPane {

    /** 每个格子的像素尺寸 */
    private static final double CELL_SIZE = 40;

    /** 画布区域的内边距（四周） */
    private static final double CANVAS_AREA_PADDING = 14;

    // ---- 地图配色 ----
    private static final Color WALL_FILL = Color.web("#5a5a5a");
    private static final Color FLOOR_FILL = Color.web("#e8e0c9");
    private static final Color GRID_LINE = Color.web("#d5cbb0");
    private static final Color GOAL_MARK = Color.web("#d9534f");
    private static final Color CONVEYOR_FILL = Color.web("#7387c4");
    private static final Color ARROW_FILL = Color.web("#ffffff");
    private static final Color BOX_FILL = Color.web("#b8860b");
    private static final Color BOX_ON_GOAL_FILL = Color.web("#2e8b57");
    private static final Color PLAYER_FILL = Color.web("#2f6fed");

    // ---- 界面配色 ----
    private static final String BAR_BG = "-fx-background-color: #3c3f41;";
    private static final String PANEL_BG = "-fx-background-color: #2b2b2b;";
    private static final Color TEXT_LIGHT = Color.web("#f0f0f0");
    private static final Color TEXT_MUTED = Color.web("#9a9a9a");

    /** 关卡选择按钮样式（普通 / 当前关卡高亮） */
    private static final String LEVEL_BTN_STYLE = "-fx-background-color: #5a5d5e; -fx-text-fill: #f0f0f0; "
            + "-fx-background-radius: 6; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 12; "
            + "-fx-min-width: 32; -fx-padding: 3 8;";
    private static final String LEVEL_BTN_ACTIVE_STYLE = "-fx-background-color: #2f6fed; -fx-text-fill: white; "
            + "-fx-background-radius: 6; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 12; "
            + "-fx-font-weight: bold; -fx-min-width: 32; -fx-padding: 3 8;";

    /** 按钮动作回调，由 Launcher 注入 */
    public interface Actions {

        /** 重开当前关卡 */
        void reset();

        /** 进入下一关卡 */
        void nextLevel();

        /**
         * 直接跳到指定关卡
         *
         * @param levelIndex 关卡序号（从 0 开始）
         */
        void selectLevel(int levelIndex);

        /** 返回主菜单 */
        void backToMenu();
    }

    /** 图片资源加载器（单例；图片缺失时各绘制方法自动回退纯色） */
    private final ImageLoader imageLoader = ImageLoader.getInstance();

    private final Canvas canvas = new Canvas(0, 0);
    private final Label moveLabel = new Label();
    private final Label winBanner = new Label("恭喜通关！按 N 进入下一关");
    private final HBox topBar;
    private final HBox bottomBar;

    /** 关卡选择按钮（数量与关卡总数一致） */
    private final Button[] levelButtons;

    /**
     * @param levelCount 关卡总数（决定关卡选择按钮数量）
     * @param actions    按钮动作回调
     */
    public GameView(int levelCount, Actions actions) {
        levelButtons = new Button[levelCount];
        setStyle(PANEL_BG);
        topBar = buildTopBar(actions);
        bottomBar = buildBottomBar(actions);
        setTop(topBar);
        setCenter(buildCanvasArea());
        setBottom(bottomBar);
        updateStatus(0, false);
    }

    // ==================== 对外接口 ====================

    /**
     * 进入新关卡：高亮对应关卡按钮，并按地图尺寸重置画布大小。
     * 场景尺寸计算前需先调用本方法，保证窗口能适配新地图。
     *
     * @param levelNumber 关卡号（从 1 开始）
     */
    public void beginLevel(int levelNumber, GameMap map) {
        for (int i = 0; i < levelButtons.length; i++) {
            levelButtons[i].setStyle(i == levelNumber - 1 ? LEVEL_BTN_ACTIVE_STYLE : LEVEL_BTN_STYLE);
        }
        canvas.setWidth(map.getWidth() * CELL_SIZE);
        canvas.setHeight(map.getHeight() * CELL_SIZE);
    }

    /**
     * 渲染地图（由渲染帧回调触发，仅在画面变化时调用）：
     * 依次绘制地形层（墙 / 地板 / 目标点 / 传送带）、箱子层与玩家层。
     */
    public void render(GameMap map) {
        syncCanvasSize(map);
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
    }

    /** 更新状态栏：步数与通关横幅。 */
    public void updateStatus(int moveCount, boolean win) {
        moveLabel.setText("步数: " + moveCount);
        winBanner.setVisible(win);
    }

    /**
     * 适配当前地图所需的界面宽度（内容尺寸，不含窗口装饰）。
     * 取地图画布区与状态栏、操作栏的较大者，保证切关后地图与按钮都完整可见。
     */
    public double computeRequiredWidth() {
        double mapArea = canvas.getWidth() + CANVAS_AREA_PADDING * 2;
        return Math.max(mapArea, Math.max(topBar.prefWidth(-1), bottomBar.prefWidth(-1)));
    }

    /**
     * 适配当前地图所需的界面高度（内容尺寸，不含窗口装饰）：
     * 顶部状态栏 + 地图画布区 + 底部操作栏。
     */
    public double computeRequiredHeight() {
        double width = computeRequiredWidth();
        double canvasArea = canvas.getHeight() + CANVAS_AREA_PADDING * 2;
        return topBar.prefHeight(width) + canvasArea + bottomBar.prefHeight(width);
    }

    // ==================== 界面构建 ====================

    /** 顶部状态栏：左关卡选择按钮，右步数。 */
    private HBox buildTopBar(Actions actions) {
        Label title = new Label("关卡");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 15));
        title.setTextFill(TEXT_LIGHT);

        HBox levelBox = new HBox(8);
        levelBox.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < levelButtons.length; i++) {
            final int levelIndex = i;
            Button button = new Button(String.valueOf(i + 1));
            button.setFocusTraversable(false);
            button.setStyle(LEVEL_BTN_STYLE);
            button.setOnAction(event -> actions.selectLevel(levelIndex));
            levelButtons[i] = button;
            levelBox.getChildren().add(button);
        }

        moveLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 15));
        moveLabel.setTextFill(TEXT_LIGHT);

        HBox bar = new HBox(12, title, levelBox, spacer(), moveLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.setStyle(BAR_BG);
        return bar;
    }

    /** 中央画布区：地图 Canvas 叠加通关横幅。 */
    private StackPane buildCanvasArea() {
        winBanner.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
        winBanner.setTextFill(Color.WHITE);
        winBanner.setBackground(new Background(new BackgroundFill(
                Color.web("#000000bf"), new CornerRadii(10), Insets.EMPTY)));
        winBanner.setPadding(new Insets(12, 26, 12, 26));
        winBanner.setVisible(false);

        StackPane area = new StackPane(canvas, winBanner);
        area.setPadding(new Insets(CANVAS_AREA_PADDING));
        area.setStyle(PANEL_BG);
        return area;
    }

    /** 底部操作栏：提示文字 + 返回菜单 / 重开 / 下一关按钮。 */
    private HBox buildBottomBar(Actions actions) {
        Label hint = new Label("方向键 / WASD 移动 · R 重开 · N 下一关 · 数字键选关 · Esc 返回菜单");
        hint.setFont(Font.font("Microsoft YaHei", 12));
        hint.setTextFill(TEXT_MUTED);

        String buttonStyle = "-fx-background-color: #5a5d5e; -fx-text-fill: #f0f0f0; "
                + "-fx-background-radius: 6; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 12;";

        Button menuButton = new Button("返回菜单 (Esc)");
        menuButton.setFocusTraversable(false);
        menuButton.setStyle(buttonStyle);
        menuButton.setOnAction(event -> actions.backToMenu());

        Button resetButton = new Button("重开 (R)");
        resetButton.setFocusTraversable(false);
        resetButton.setStyle(buttonStyle);
        resetButton.setOnAction(event -> actions.reset());

        Button nextButton = new Button("下一关 (N)");
        nextButton.setFocusTraversable(false);
        nextButton.setStyle(buttonStyle);
        nextButton.setOnAction(event -> actions.nextLevel());

        HBox bar = new HBox(10, hint, spacer(), menuButton, resetButton, nextButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.setStyle(BAR_BG);
        return bar;
    }

    /** 弹性空白：撑开两侧内容。 */
    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /** 画布尺寸与地图不符时自动同步（尺寸变化的兜底）。 */
    private void syncCanvasSize(GameMap map) {
        double width = map.getWidth() * CELL_SIZE;
        double height = map.getHeight() * CELL_SIZE;
        if (canvas.getWidth() != width || canvas.getHeight() != height) {
            canvas.setWidth(width);
            canvas.setHeight(height);
        }
    }

    // ==================== 地图绘制 ====================

    private void drawTerrain(GraphicsContext g, GameMap map, int x, int y) {
        Element cell = map.getCell(x, y);
        double px = x * CELL_SIZE;
        double py = y * CELL_SIZE;

        if (cell instanceof Wall) {
            Image wallImg = imageLoader.get(ImageLoader.WALL);
            if (wallImg != null) {
                g.drawImage(wallImg, px, py, CELL_SIZE, CELL_SIZE);
            } else {
                g.setFill(WALL_FILL);
                g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            }
            return;
        }
        // 地板底色（图片优先，失败回退纯色 + 网格线）
        Image floorImg = imageLoader.get(ImageLoader.FLOOR);
        if (floorImg != null) {
            g.drawImage(floorImg, px, py, CELL_SIZE, CELL_SIZE);
        } else {
            g.setFill(FLOOR_FILL);
            g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
            g.setStroke(GRID_LINE);
            g.strokeRect(px + 0.5, py + 0.5, CELL_SIZE - 1, CELL_SIZE - 1);
        }

        if (cell instanceof Goal) {
            // 目标点：图片优先，回退中央圆点
            Image goalImg = imageLoader.get(ImageLoader.GOAL);
            if (goalImg != null) {
                g.drawImage(goalImg, px, py, CELL_SIZE, CELL_SIZE);
            } else {
                double d = CELL_SIZE * 0.26;
                g.setFill(GOAL_MARK);
                g.fillOval(px + (CELL_SIZE - d) / 2, py + (CELL_SIZE - d) / 2, d, d);
            }
        } else if (cell instanceof Conveyor conveyor) {
            // 传送带：图片优先，回退底色 + 方向箭头
            Image convImg = imageLoader.getConveyorImage(conveyor.getDirection());
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

    private void drawBox(GraphicsContext g, Box box) {
        Position p = box.getPosition();
        double px = p.x() * CELL_SIZE;
        double py = p.y() * CELL_SIZE;

        Image img = imageLoader.get(box.isOnGoal() ? ImageLoader.BOX_ON_GOAL : ImageLoader.BOX);
        if (img != null) {
            g.drawImage(img, px, py, CELL_SIZE, CELL_SIZE);
            return;
        }
        // 回退：圆角矩形
        double padding = CELL_SIZE * 0.12;
        g.setFill(box.isOnGoal() ? BOX_ON_GOAL_FILL : BOX_FILL);
        g.fillRoundRect(px + padding, py + padding,
                CELL_SIZE - 2 * padding, CELL_SIZE - 2 * padding, 8, 8);
    }

    private void drawPlayer(GraphicsContext g, Player player) {
        Position p = player.getPosition();
        double px = p.x() * CELL_SIZE;
        double py = p.y() * CELL_SIZE;

        Image img = imageLoader.get(ImageLoader.PLAYER);
        if (img != null) {
            g.drawImage(img, px, py, CELL_SIZE, CELL_SIZE);
            return;
        }
        // 回退：蓝色圆
        double padding = CELL_SIZE * 0.15;
        g.setFill(PLAYER_FILL);
        g.fillOval(px + padding, py + padding,
                CELL_SIZE - 2 * padding, CELL_SIZE - 2 * padding);
    }
}
