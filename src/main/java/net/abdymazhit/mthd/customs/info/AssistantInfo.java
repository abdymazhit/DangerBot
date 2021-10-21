package net.abdymazhit.mthd.customs.info;

import java.sql.Timestamp;

/**
 * Представляет собой помощника
 *
 * @version   21.10.2021
 * @author    Islam Abdymazhit
 */
public class AssistantInfo {

    /** Id помощника */
    public int id;

    /** Discord id помощника */
    public String discordId;

    /** Имя помощника */
    public String username;

    /** Количество проведенных игр */
    public int games;

    /** Количество проведенных игр за неделю */
    public int weeklyGames;

    /** Количество проведенных игр за сегодня */
    public int todayGames;

    /** Время последней проведенной игры */
    public Timestamp lastGameTimestamp;

    /**
     * Инициализирует помощника
     * @param id Id помощника
     * @param username Имя помощника
     * @param discordId Discord id помощника
     */
    public AssistantInfo(int id, String username, String discordId) {
        this.id = id;
        this.username = username;
        this.discordId = discordId;
        this.games = 0;
        this.weeklyGames = 0;
        this.todayGames = 0;
    }

    /**
     * Получает количество проведенных игр за сегодня
     * @return Количество проведенных игр за сегодня
     */
    public int getTodayGames() {
        return todayGames;
    }
}
