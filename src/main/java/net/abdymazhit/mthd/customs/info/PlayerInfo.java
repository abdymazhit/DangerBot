package net.abdymazhit.mthd.customs.info;

import net.abdymazhit.mthd.enums.GameResult;

import java.sql.Timestamp;
import java.util.List;

/**
 * Представляет собой игрока
 *
 * @version   21.10.2021
 * @author    Islam Abdymazhit
 */
public class PlayerInfo {

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

    /** Последние результаты игр игрока */
    public List<GameResult> lastGameResults;

    /** Время последней игры игрока */
    public Timestamp latestActive;

    /**
     * Инициализирует игрока
     * @param id Id игрока
     * @param username Имя игрока
     * @param points Очки игрока
     */
    public PlayerInfo(int id, String username, int points) {
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
     * @param latestActive Время последней игры игрока
     */
    public PlayerInfo(int id, String username, int points, int games, int wins, List<GameResult> lastGameResults, Timestamp latestActive) {
        this.id = id;
        this.username = username;
        this.points = points;
        this.games = games;
        this.wins = wins;
        this.lastGameResults = lastGameResults;
        this.latestActive = latestActive;
    }
}
