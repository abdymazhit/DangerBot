package net.abdymazhit.mthd.enums;

/**
 * Представляет собой стадию игры
 *
 * @version   13.09.2021
 * @author    Islam Abdymazhit
 */
public enum GameState {
    PLAYERS_SELECTION(0, "Выбор игроков на игру");

    private final int id;
    private final String name;

    /**
     * Инициализирует стадию
     * @param id Id стадии
     * @param name Название стадии
     */
    GameState(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Получает id стадии
     * @return Id стадии
     */
    public int getId() {
        return id;
    }

    /**
     * Получает название стадии
     * @return Название стадии
     */
    public String getName() {
        return name;
    }
}
