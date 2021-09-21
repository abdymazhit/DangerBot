package net.abdymazhit.mthd.enums;

/**
 * Представляет собой стадию игры
 *
 * @version   21.09.2021
 * @author    Islam Abdymazhit
 */
public enum GameState {
    PLAYERS_CHOICE(0, "Выбор игроков"),
    MAP_CHOICE(1, "Выбор карты"),
    GAME_CREATION(2, "Создание сервера"),
    GAME(3, "Игра");

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
