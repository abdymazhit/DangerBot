package net.abdymazhit.mthd.customs;

/**
 * Представляет собой игрока
 *
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class Player {

    /** Id игрока */
    public final int id;

    /** Имя игрока */
    public String username;

    /** Количество очков игрока */
    public int points;

    /** Количество сыгранных игр игрока */
    public int games;

    /** Количество побед игрока */
    public int wins;

    /**
     * Инициализирует игрока
     * @param id Id игрока
     * @param username Имя игрока
     * @param points Очки игрока
     */
    public Player(int id, String username, int points) {
        this.id = id;
        this.username = username;
        this.points = points;
    }

    /**
     * Инициализирует игрока
     * @param id Id игрока
     * @param username Имя игрока
     * @param points Очки игрока
     * @param games Количество игр игрока
     * @param wins Количество побед игрока
     */
    public Player(int id, String username, int points, int games, int wins) {
        this.id = id;
        this.username = username;
        this.points = points;
        this.games = games;
        this.wins = wins;
    }
}
