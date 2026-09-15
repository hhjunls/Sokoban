package org.csu.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 主界面视图（View 层）：游戏标题与"开始游戏 / 退出游戏"按钮。
 * <p>
 * 与 Controller 解耦：按钮动作通过构造参数 {@link Actions} 在组装 MVC 时注入（见 Launcher）。
 */
public class MainMenuView extends StackPane {

    /** 界面设计尺寸（内容尺寸，不含窗口装饰） */
    private static final double DESIGN_WIDTH = 440;
    private static final double DESIGN_HEIGHT = 340;

    /** 主按钮（开始游戏）样式 */
    private static final String PRIMARY_BTN_STYLE = "-fx-background-color: #2f6fed; -fx-text-fill: white; "
            + "-fx-background-radius: 8; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 16; "
            + "-fx-font-weight: bold; -fx-min-width: 240; -fx-min-height: 46; -fx-cursor: hand;";

    /** 次按钮（退出游戏）样式 */
    private static final String SECONDARY_BTN_STYLE = "-fx-background-color: #5a5d5e; -fx-text-fill: #f0f0f0; "
            + "-fx-background-radius: 8; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 14; "
            + "-fx-min-width: 240; -fx-min-height: 42; -fx-cursor: hand;";

    /** 按钮动作回调，由 Launcher 注入 */
    public interface Actions {

        /** 点击"开始游戏"：进入选关界面 */
        void startGame();

        /** 点击"退出游戏"：结束程序 */
        void quit();
    }

    public MainMenuView(Actions actions) {
        setStyle("-fx-background-color: #2b2b2b;");
        setPrefWidth(DESIGN_WIDTH);
        setPrefHeight(DESIGN_HEIGHT);

        Label title = new Label("推箱子");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 54));
        title.setTextFill(Color.web("#f0f0f0"));

        Label subtitle = new Label("S O K O B A N");
        subtitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 13));
        subtitle.setTextFill(Color.web("#9a9a9a"));

        Button startButton = new Button("开始游戏");
        startButton.setFocusTraversable(false);
        startButton.setStyle(PRIMARY_BTN_STYLE);
        startButton.setOnAction(event -> actions.startGame());

        Button quitButton = new Button("退出游戏");
        quitButton.setFocusTraversable(false);
        quitButton.setStyle(SECONDARY_BTN_STYLE);
        quitButton.setOnAction(event -> actions.quit());

        VBox box = new VBox(title, subtitle, startButton, quitButton);
        box.setAlignment(Pos.CENTER);
        VBox.setMargin(subtitle, new Insets(6, 0, 0, 0));
        VBox.setMargin(startButton, new Insets(52, 0, 0, 0));
        VBox.setMargin(quitButton, new Insets(14, 0, 0, 0));
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
