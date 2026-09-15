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
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.csu.controller.GameController;
import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;
import org.csu.view.GameView;
import org.csu.view.LevelSelectView;
import org.csu.view.MainMenuView;

/**
 * 游戏主类（Launcher）：组装 Model / View / Controller，
 * 并以 {@link AnimationTimer} 驱动"固定步长逻辑 + 逐帧渲染"的游戏主循环。
 * <p>
 * 固定步长说明：逻辑更新以 {@link #FIXED_STEP} 为最小步长推进，
 * 与显示器刷新率解耦；长时间卡顿时用 {@link #MAX_FRAME_TIME} 钳制单帧耗时，
 * 避免"死亡螺旋"式追帧。
 * <p>
 * 界面流程：主界面 →（开始游戏）→ 选关界面 →（选择关卡）→ 游戏界面；
 * 游戏内 Esc 返回主菜单，主界面"退出游戏"结束程序。
 * <p>
 * 操作：方向键 / WASD 移动，R 重置关卡，N 下一关，数字键选关，Esc 返回主菜单。
 */
public class Launcher extends Application {

    /** 固定步长：每秒 60 个逻辑帧 */
    private static final double FIXED_STEP = 1.0 / 60.0;
    /** 单帧最大耗时上限（秒），防止长卡顿后疯狂追帧 */
    private static final double MAX_FRAME_TIME = 0.25;
    /** 关卡资源列表（resources 目录下），按顺序循环加载 */
    private static final String[] LEVEL_RESOURCES = {
            "/maps/1.txt",
            "/maps/2.txt",
            "/maps/3.txt"
    };

    /** 当前显示的界面类型 */
    private enum Screen { MENU, LEVEL_SELECT, GAME }

    private GameController controller;
    private GameView view;
    private MainMenuView mainMenuView;
    private LevelSelectView levelSelectView;
    private Scene scene;
    private Stage stage;
    /** 当前显示的界面 */
    private Screen currentScreen = Screen.MENU;
    /** 当前关卡序号（从 0 开始） */
    private int currentLevelIndex;
    /** 窗口装饰尺寸（标题栏 + 边框），用于按内容尺寸精确适配窗口 */
    private double windowDecorationWidth;
    private double windowDecorationHeight;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // ---- View：游戏界面，按钮动作回交给 Controller / Launcher ----
        view = new GameView(LEVEL_RESOURCES.length, new GameView.Actions() {
            @Override
            public void reset() {
                controller.resetGame();
            }

            @Override
            public void nextLevel() {
                loadLevel(currentLevelIndex + 1);
            }

            @Override
            public void selectLevel(int levelIndex) {
                loadLevel(levelIndex);
            }

            @Override
            public void backToMenu() {
                showMainMenu();
            }
        });

        // ---- View：主界面与选关界面 ----
        mainMenuView = new MainMenuView(new MainMenuView.Actions() {
            @Override
            public void startGame() {
                showLevelSelect();
            }

            @Override
            public void quit() {
                controller.quitGame();
            }
        });

        levelSelectView = new LevelSelectView(LEVEL_RESOURCES.length, new LevelSelectView.Actions() {
            @Override
            public void selectLevel(int levelIndex) {
                loadLevel(levelIndex);
            }

            @Override
            public void backToMenu() {
                showMainMenu();
            }
        });

        // ---- 组装 MVC：Controller 通过回调驱动 View、退出 ----
        controller = new GameController(this::renderView, () -> Platform.exit());

        // ---- 场景只创建一次：初始显示主界面，root 随界面切换 ----
        scene = new Scene(mainMenuView);
        scene.setOnKeyPressed(event -> handleKey(event.getCode()));
        stage.setScene(scene);

        // ---- 显示窗口（初始主界面） ----
        stage.setTitle("推箱子 Sokoban");
        stage.setResizable(false);
        stage.show();

        // 记录窗口装饰尺寸（初始窗口尺寸与场景尺寸之差）
        windowDecorationWidth = stage.getWidth() - scene.getWidth();
        windowDecorationHeight = stage.getHeight() - scene.getHeight();
        // 窗口完全就绪后再按内容尺寸校正一次（首次显示时的尺寸可能滞后）
        Platform.runLater(() -> fitWindow(mainMenuView.computeRequiredWidth(), mainMenuView.computeRequiredHeight()));

        startGameLoop();
    }

    /**
     * 加载指定序号的关卡（超出范围自动循环）并切换到游戏界面：
     * 从 resources 读取关卡文件，按地图尺寸重建画布，并让窗口适配新尺寸。
     *
     * @param index 关卡序号（从 0 开始）
     */
    private void loadLevel(int index) {
        currentLevelIndex = Math.floorMod(index, LEVEL_RESOURCES.length);
        List<String> lines = readLevelLines(LEVEL_RESOURCES[currentLevelIndex]);
        controller.loadLevel(lines.toArray(new String[0]));
        controller.startGame();

        // 切换到游戏界面（高亮关卡按钮、按地图尺寸重置画布），再让窗口适配新尺寸
        currentScreen = Screen.GAME;
        scene.setRoot(view);
        view.beginLevel(currentLevelIndex + 1, controller.getMap());
        fitWindow(view.computeRequiredWidth(), view.computeRequiredHeight());
    }

    /** 切换到主界面：更新窗口标题并按界面尺寸适配窗口。 */
    private void showMainMenu() {
        currentScreen = Screen.MENU;
        scene.setRoot(mainMenuView);
        stage.setTitle("推箱子 Sokoban");
        fitWindow(mainMenuView.computeRequiredWidth(), mainMenuView.computeRequiredHeight());
    }

    /** 切换到选关界面：更新窗口标题并按界面尺寸适配窗口。 */
    private void showLevelSelect() {
        currentScreen = Screen.LEVEL_SELECT;
        scene.setRoot(levelSelectView);
        stage.setTitle("推箱子 Sokoban - 选择关卡");
        fitWindow(levelSelectView.computeRequiredWidth(), levelSelectView.computeRequiredHeight());
    }

    /**
     * 让窗口按内容尺寸适配（切换界面与切关后尺寸不同）。
     * 非可调整大小的窗口 sizeToScene 不生效，且布局尺寸更新滞后，
     * 因此直接用内容尺寸 + 窗口装饰尺寸设置窗口大小。
     */
    private void fitWindow(double contentWidth, double contentHeight) {
        if (!stage.isShowing()) {
            return;
        }
        stage.setWidth(contentWidth + windowDecorationWidth);
        stage.setHeight(contentHeight + windowDecorationHeight);
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
     * 键盘事件分发（按当前界面分流）：
     * 游戏界面支持方向键 / WASD 移动、R 重置、N 下一关、数字键选关、Esc 返回主菜单；
     * 选关界面支持 Esc 返回主菜单；主界面仅用鼠标操作。
     */
    private void handleKey(KeyCode code) {
        if (currentScreen == Screen.LEVEL_SELECT && code == KeyCode.ESCAPE) {
            showMainMenu();
            return;
        }
        if (currentScreen != Screen.GAME) {
            return;
        }
        switch (code) {
            case UP, W -> controller.onKeyPressed(Direction.UP);
            case DOWN, S -> controller.onKeyPressed(Direction.DOWN);
            case LEFT, A -> controller.onKeyPressed(Direction.LEFT);
            case RIGHT, D -> controller.onKeyPressed(Direction.RIGHT);
            case R -> controller.resetGame();
            case N -> loadLevel(currentLevelIndex + 1);
            case ESCAPE -> showMainMenu();
            default -> {
                // 数字键 1-9 快速选关
                String name = code.getName();
                if (name.length() == 1 && Character.isDigit(name.charAt(0))) {
                    int levelNumber = name.charAt(0) - '0';
                    if (levelNumber >= 1 && levelNumber <= LEVEL_RESOURCES.length) {
                        loadLevel(levelNumber - 1);
                    }
                }
            }
        }
    }

    /**
     * AnimationTimer 固定步长主循环：
     * 累积真实经过时间，按 {@link #FIXED_STEP} 逐帧推进逻辑更新；
     * 渲染每帧执行一次，由控制器脏标记决定是否真正重绘。
     * 仅在游戏界面推进逻辑，主界面 / 选关界面暂停游戏。
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

                // 非游戏界面：不推进逻辑，仅保持时间基准
                if (currentScreen != Screen.GAME) {
                    accumulator = 0;
                    return;
                }

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
     * 渲染回调（由 GameController 脏标记触发）：
     * 交给 View 绘制地图，并同步状态栏步数、通关横幅与窗口标题。
     */
    private void renderView(GameMap map) {
        view.render(map);
        view.updateStatus(controller.getMoveCount(), controller.isGameWin());
        stage.setTitle("推箱子 Sokoban - 第 " + (currentLevelIndex + 1) + "/" + LEVEL_RESOURCES.length + " 关"
                + (controller.isGameWin() ? " - 通关！按 N 进入下一关" : ""));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
