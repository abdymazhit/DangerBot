package net.abdymazhit.mthd.enums;

/**
 * Представляет собой стадию игры
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public enum GameState {
    PLAYERS_CHOICE(0),
    MAP_CHOICE(1),
    GAME_CREATION(2),
    GAME(3);

    private final int id;

    /**
     * Инициализирует стадию
     * @param id Id стадии
     */
    GameState(int id) {
        this.id = id;
    }

    /**
     * Получает id стадии
     * @return Id стадии
     */
    public int getId() {
        return id;
    }
}
