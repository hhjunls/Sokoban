package org.csu;

/**
 * 程序入口（兼容代理）：转发到 JavaFX 游戏主类 {@link Launcher}。
 * <p>
 * Maven 配置的启动主类为 {@code org.csu.Launcher}，本类仅作为备用入口保留。
 */
public class Main {
    public static void main(String[] args) {
        Launcher.main(args);
    }
}
