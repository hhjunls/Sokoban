package org.csu.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 选关界面视图（View 层）：关卡按钮网格与"返回主菜单"按钮。
 * <p>
 * 与 Controller 解耦：按钮动作通过构造参数 {@link Actions} 在组装 MVC 时注入（见 Launcher）。
 */
public class LevelSelectView extends StackPane {

    /** 界面设计尺寸（内容尺寸，不含窗口装饰） */
    private static final double DESIGN_WIDTH = 480;
    private static final double DESIGN_HEIGHT = 380;

    /** 每行展示的关卡按钮数 */
    private static final int COLUMNS = 3;

    /** 关卡按钮样式 */
    private static final String LEVEL_BTN_STYLE = "-fx-background-color: #5a5d5e; -fx-text-fill: #f0f0f0; "
            + "-fx-background-radius: 8; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 15; "
            + "-fx-min-width: 120; -fx-min-height: 64; -fx-cursor: hand;";

    /** 返回按钮样式 */
    private static final String BACK_BTN_STYLE = "-fx-background-color: #3c3f41; -fx-text-fill: #9a9a9a; "
            + "-fx-background-radius: 6; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 13; "
            + "-fx-min-width: 130; -fx-min-height: 34; -fx-cursor: hand;";

    /** 按钮动作回调，由 Launcher 注入 */
    public interface Actions {

        /**
         * 选择关卡并进入游戏
         *
         * @param levelIndex 关卡序号（从 0 开始）
         */
        void selectLevel(int levelIndex);

        /** 返回主界面 */
        void backToMenu();
    }

    /**
     * @param levelCount 关卡总数（决定关卡按钮数量）
     * @param actions    按钮动作回调
     */
    public LevelSelectView(int levelCount, Actions actions) {
        setStyle("-fx-background-color: #2b2b2b;");
        setPrefWidth(DESIGN_WIDTH);
        setPrefHeight(DESIGN_HEIGHT);

        Label title = new Label("选择关卡");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#f0f0f0"));

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);
        for (int i = 0; i < levelCount; i++) {
            final int levelIndex = i;
            Button button = new Button("第 " + (i + 1) + " 关");
            button.setFocusTraversable(false);
            button.setStyle(LEVEL_BTN_STYLE);
            button.setOnAction(event -> actions.selectLevel(levelIndex));
            grid.add(button, i % COLUMNS, i / COLUMNS);
        }

        Button backButton = new Button("返回主菜单 (Esc)");
        backButton.setFocusTraversable(false);
        backButton.setStyle(BACK_BTN_STYLE);
        backButton.setOnAction(event -> actions.backToMenu());

        VBox box = new VBox(title, grid, backButton);
        box.setAlignment(Pos.CENTER);
        VBox.setMargin(grid, new Insets(36, 0, 0, 0));
        VBox.setMargin(backButton, new Insets(40, 0, 0, 0));
        getChildren().add(box);
    }

    /**
     * 界面所需宽度（内容尺寸，不含窗口装饰）。
     */
    public double computeRequiredWidth() {
        return DESIGN_WIDTH;
    }

    /**
     * 界面所需高度（内容尺寸，不含窗口装饰）。
     */
    public double computeRequiredHeight() {
        return DESIGN_HEIGHT;
    }
}
