package org.csu.resourse;

import org.csu.model.base.GameMap;

import java.util.List;

public class Maps {

    private static final List<GameMap> LEVELS = List.of(
            GameMap.parse(
                    "#######",
                    "#   . #",
                    "# # # #",
                    "# $@$ #",
                    "# # #.#",
                    "#  *  #",
                    "#######"
            ),
            GameMap.parse(
                    "#########",
                    "#       #",
                    "# >>>v  #",
                    "# $  v^.#",
                    "#    v ##",
                    "# $  v  #",
                    "#@>>>v  #",
                    "#      .#",
                    "#########"
            ),
            GameMap.parse(
                    "###########",
                    "#         #",
                    "#   ###   #",
                    "#    #    #",
                    "# A  # A  #",
                    "# @  #    #",
                    "#   ###   #",
                    "#  .     .#",
                    "###########"
            )
    );

    public static GameMap getLevel(int index) {
        if (index < 0 || index >= LEVELS.size()) {
            throw new IllegalArgumentException("关卡编号越界: " + index);
        }
        return LEVELS.get(index);
    }

    public static int getLevelCount() {
        return LEVELS.size();
    }
}
