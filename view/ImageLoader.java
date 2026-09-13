package org.csu.view;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
     * 图片资源加载器（单例），统一管理所有游戏图片。
     * 图片放在 src/main/resources/images/ 下。
     */
    public class ImageLoader {

        private static final ImageLoader INSTANCE = new ImageLoader();

        public static ImageLoader getInstance() {
            return INSTANCE;
        }

        /** 图片缓存，避免重复加载 */
        private final Map<String, Image> cache = new HashMap<>();

        private static final String BASE_PATH = "/images/";

        // ===== 图片路径常量 =====
        public static final String WALL = "wall.png";
        public static final String FLOOR = "floor.png";
        public static final String GOAL = "goal.png";
        public static final String BOX = "box.png";
        public static final String BOX_ON_GOAL = "box_on_goal.png";
        public static final String PLAYER = "player.png";
        public static final String CONVEYOR_UP = "conveyor_up.png";
        public static final String CONVEYOR_DOWN = "conveyor_down.png";
        public static final String CONVEYOR_LEFT = "conveyor_left.png";
        public static final String CONVEYOR_RIGHT = "conveyor_right.png";

        private ImageLoader() {
            // 预加载所有图片
            preload(WALL);
            preload(FLOOR);
            preload(GOAL);
            preload(BOX);
            preload(BOX_ON_GOAL);
            preload(PLAYER);
            preload(CONVEYOR_UP);
            preload(CONVEYOR_DOWN);
            preload(CONVEYOR_LEFT);
            preload(CONVEYOR_RIGHT);
        }

        private void preload(String filename) {
            try {
                Image img = new Image(getClass().getResourceAsStream(BASE_PATH + filename));
                cache.put(filename, img);
            } catch (Exception e) {
                System.err.println("图片加载失败: " + filename + "，将使用纯色替代");
                cache.put(filename, null);
            }
        }

        public Image get(String filename) {
            return cache.getOrDefault(filename, null);
        }

        /**
         * 根据传送带方向获取对应图片
         */
        public Image getConveyorImage(org.csu.model.base.Direction direction) {
            return switch (direction) {
                case UP -> get(CONVEYOR_UP);
                case DOWN -> get(CONVEYOR_DOWN);
                case LEFT -> get(CONVEYOR_LEFT);
                case RIGHT -> get(CONVEYOR_RIGHT);
            };
        }
    }
