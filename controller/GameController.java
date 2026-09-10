package org.csu.controller;

import org.csu.model.base.Direction;

/**
 * 游戏控制器：接收视图层事件，调度模型层业务，并驱动视图刷新。
 * <p>
 * 本类仅给出方法签名，具体实现留待后续填充。
 */
public class GameController {

    /**
     * 处理键盘方向键事件。
     *
     * @param direction 玩家移动方向
     */
    public void onKeyPressed(Direction direction) {
        // TODO 待实现：转发给 GameModel.movePlayer，并刷新视图
    }

    /**
     * 启动游戏。
     */
    public void startGame() {
        // TODO 待实现
    }

    /**
     * 加载关卡。
     *
     * @param levelLines 关卡文本行
     */
    public void loadLevel(String... levelLines) {
        // TODO 待实现：解析关卡并创建 GameModel
    }

    /**
     * 重置当前关卡。
     */
    public void resetGame() {
        // TODO 待实现：调用 GameModel.reset 并刷新视图
    }

    /**
     * 刷新视图画面。
     */
    public void renderView() {
        // TODO 待实现：调用 View 的 render(map)
    }

    /**
     * 处理胜利事件。
     */
    public void handleWin() {
        // TODO 待实现
    }

    /**
     * 退出游戏。
     */
    public void quitGame() {
        // TODO 待实现
    }
}
