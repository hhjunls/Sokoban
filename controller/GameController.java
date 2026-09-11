package org.csu.controller;

import org.csu.model.api.GameModel;
import org.csu.model.base.Direction;
import org.csu.model.base.GameMap;
import org.csu.model.impl.SokobanGameModel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * 游戏控制器（完整实现）。
 * <p>
 * 与固定步长主循环（AnimationTimer）配合的工作方式：
 * <ul>
 *   <li>{@link #onKeyPressed} 只把输入放入队列（事件线程调用，不直接改模型）；</li>
 *   <li>{@link #fixedUpdate} 在固定步长逻辑帧中累积传送带计时器
 *       （达到 {@link #CONVEYOR_INTERVAL} 自动结算一格，与玩家移动无关），
 *       并消费一个输入推进游戏模型；</li>
 *   <li>{@link #renderView} 在渲染帧检查脏标记，仅在画面变化时通知视图重绘。</li>
 * </ul>
 * 通过回调与视图解耦，本类不依赖 JavaFX。
 */
public class GameController {

    /** 输入队列容量上限，防止疯狂按键积压过多输入 */
    private static final int MAX_PENDING_INPUTS = 2;

    /** 传送带结算间隔（秒）：每经过该时长，传送带自动移动一格 */
    private static final double CONVEYOR_INTERVAL = 0.5;

    /** 视图刷新回调，由 Launcher 注入（对应 View 的 render(map)） */
    private final Consumer<GameMap> viewRenderer;

    /** 退出回调，由 Launcher 注入（对应关闭游戏窗口） */
    private final Runnable quitAction;

    /** 当前游戏模型，由 {@link #loadLevel} 创建 */
    private GameModel model;

    /** 待处理输入队列（先入先出） */
    private final Deque<Direction> inputQueue = new ArrayDeque<>();

    /** 传送带计时器（秒），由固定步长累积，达到间隔后触发一次结算 */
    private double conveyorTimer;

    /** 画面脏标记：逻辑变化后置 true，渲染完成后置回 false */
    private boolean viewDirty = true;

    /** 本关卡的胜利逻辑是否已处理，防止重复触发 */
    private boolean winHandled;

    public GameController(Consumer<GameMap> viewRenderer, Runnable quitAction) {
        this.viewRenderer = viewRenderer;
        this.quitAction = quitAction;
    }

    // ==================== 输入事件 ====================

    /**
     * 处理键盘方向键事件：仅入队，实际移动在固定步长逻辑帧中执行。
     * 队列满时丢弃最早的输入，保证最新的按键意图优先生效。
     *
     * @param direction 玩家移动方向
     */
    public void onKeyPressed(Direction direction) {
        if (direction == null || model == null) {
            return;
        }
        while (inputQueue.size() >= MAX_PENDING_INPUTS) {
            inputQueue.pollFirst();
        }
        inputQueue.offerLast(direction);
    }

    // ==================== 固定步长逻辑帧 ====================

    /**
     * 固定步长逻辑更新：
     * <ol>
     *   <li>累积传送带计时器，达到 {@link #CONVEYOR_INTERVAL} 后自动结算一格
     *       （传送带独立于玩家移动运转，逐格推进并在渲染帧刷新）；</li>
     *   <li>消费至多一个待处理输入并执行玩家移动，移动成功后检测胜利。</li>
     * </ol>
     *
     * @param deltaSeconds 固定步长（秒），由主循环传入
     */
    public void fixedUpdate(double deltaSeconds) {
        if (model == null) {
            return;
        }
        // 1. 传送带独立结算（与玩家移动无关）
        conveyorTimer += deltaSeconds;
        while (conveyorTimer >= CONVEYOR_INTERVAL) {
            conveyorTimer -= CONVEYOR_INTERVAL;
            if (model.stepConveyors()) {
                viewDirty = true;
                if (model.isWin()) {
                    handleWin();
                }
            }
        }
        // 2. 消费输入，执行玩家移动
        Direction direction = inputQueue.pollFirst();
        if (direction == null) {
            return;
        }
        if (model.movePlayer(direction)) {
            viewDirty = true;
            if (model.isWin()) {
                handleWin();
            }
        }
    }

    // ==================== 渲染帧 ====================

    /**
     * 渲染帧：仅当画面脏时通知视图重绘，避免每帧重复绘制。
     */
    public void renderView() {
        if (model != null && viewDirty) {
            if (viewRenderer != null) {
                viewRenderer.accept(model.getMap());
            }
            viewDirty = false;
        }
    }

    // ==================== 生命周期 ====================

    /**
     * 启动游戏：要求关卡已加载，清空输入并标记画面待重绘。
     */
    public void startGame() {
        if (model == null) {
            throw new IllegalStateException("请先调用 loadLevel 加载关卡");
        }
        inputQueue.clear();
        conveyorTimer = 0;
        winHandled = false;
        viewDirty = true;
    }

    /**
     * 加载关卡：从关卡文本行创建新的游戏模型，并重置控制器状态。
     *
     * @param levelLines 关卡文本行，符号约定见 {@link GameMap#parse(String...)}
     */
    public void loadLevel(String... levelLines) {
        model = new SokobanGameModel(levelLines);
        inputQueue.clear();
        conveyorTimer = 0;
        winHandled = false;
        viewDirty = true;
    }

    /**
     * 重置当前关卡：模型恢复初始快照，清空输入与胜利状态。
     */
    public void resetGame() {
        if (model == null) {
            return;
        }
        model.reset();
        inputQueue.clear();
        conveyorTimer = 0;
        winHandled = false;
        viewDirty = true;
    }

    /**
     * 处理胜利：保证同一关卡只触发一次，输出过关步数。
     */
    public void handleWin() {
        if (winHandled) {
            return;
        }
        winHandled = true;
        System.out.println("恭喜通关！共移动 " + model.getMoveCount() + " 步");
    }

    /**
     * 退出游戏：清空输入并触发注入的退出回调。
     */
    public void quitGame() {
        inputQueue.clear();
        if (quitAction != null) {
            quitAction.run();
        }
    }

    // ==================== 查询 ====================

    /**
     * 当前地图（未加载关卡时返回 null）。
     */
    public GameMap getMap() {
        return model == null ? null : model.getMap();
    }

    /**
     * 当前已走步数（未加载关卡时为 0）。
     */
    public int getMoveCount() {
        return model == null ? 0 : model.getMoveCount();
    }

    /**
     * 当前是否已通关（未加载关卡时为 false）。
     */
    public boolean isGameWin() {
        return model != null && model.isWin();
    }
}
