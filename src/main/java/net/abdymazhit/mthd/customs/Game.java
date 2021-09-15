package net.abdymazhit.mthd.customs;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.GameMap;

import java.sql.Timestamp;
import java.util.List;

/**
 * Представляет собой матч
 *
 * @version   13.09.2021
 * @author    Islam Abdymazhit
 */
public class Game {

    /** Id матча */
    public int id;

    /** Id первой команды */
    public int firstTeamId;

    /** Название первой команды */
    public String firstTeamName;

    /** Id начавшего первой команды */
    public int firstTeamStarterId;

    /** Discord id начавшего первой команды */
    public String firstTeamStarterDiscordId;

    /** Id второй команды */
    public int secondTeamId;

    /** Название второй команды */
    public String secondTeamName;

    /** Id начавшего второй команды */
    public int secondTeamStarterId;

    /** Discord id начавшего второй команды */
    public String secondTeamStarterDiscordId;

    /** Id помощника */
    public int assistantId;

    /** Имя помощника */
    public String assistantName;

    /** Discord id ассистента */
    public String assistantDiscordId;

    /** Время начала матча */
    public Timestamp startedAt;

    /** Формат матча */
    public String format;

    /** Выбранная карта */
    public GameMap gameMap;

    /** Игроки первой команды */
    public List<String> firstTeamPlayers;

    /** Игроки второй команды */
    public List<String> secondTeamPlayers;

    /**
     * Инициализирует матч
     * @param id Id матча
     * @param firstTeamId Id первой команды
     * @param firstTeamStarterId Id начавшего первой команды
     * @param secondTeamId Id второй команды
     * @param secondTeamStarterId Id начавшего второй команды
     * @param format Формат матча
     * @param assistantId Id помощника
     * @param startedAt Время начала матча
     */
    public Game(int id, int firstTeamId, int firstTeamStarterId,  int secondTeamId, int secondTeamStarterId, String format,
                int assistantId, Timestamp startedAt) {
        this.id = id;
        this.firstTeamId = firstTeamId;
        this.firstTeamStarterId = firstTeamStarterId;
        this.secondTeamId = secondTeamId;
        this.secondTeamStarterId = secondTeamStarterId;
        this.format = format;
        this.assistantId = assistantId;
        this.startedAt = startedAt;
    }

    /**
     * Инициализирует игру
     * @param id Id игры
     * @param firstTeamId Id первой команды
     * @param firstTeamName Название первой команды
     * @param firstTeamStarterId Id начавшего первой команды
     * @param secondTeamId Id второй команды
     * @param secondTeamName Название второй команды
     * @param secondTeamStarterId Id начавшего второй команды
     */
    public Game(int id, int firstTeamId, String firstTeamName, int firstTeamStarterId, int secondTeamId,
                String secondTeamName, int secondTeamStarterId, String format, int assistantId) {
        this.id = id;
        this.firstTeamId = firstTeamId;
        this.firstTeamName = firstTeamName;
        this.firstTeamStarterId = firstTeamStarterId;
        this.secondTeamId = secondTeamId;
        this.secondTeamName = secondTeamName;
        this.secondTeamStarterId = secondTeamStarterId;
        this.format = format;
        this.assistantId = assistantId;
    }

    /**
     * Получает подробную информацию о команде
     */
    public void getData() {
        firstTeamName = MTHD.getInstance().database.getTeamName(firstTeamId);
        firstTeamStarterDiscordId = MTHD.getInstance().database.getUserDiscordId(firstTeamStarterId);
        secondTeamName = MTHD.getInstance().database.getTeamName(secondTeamId);
        secondTeamStarterDiscordId = MTHD.getInstance().database.getUserDiscordId(secondTeamStarterId);
        assistantName = MTHD.getInstance().database.getUserName(assistantId);
        assistantDiscordId = MTHD.getInstance().database.getUserDiscordId(assistantId);
    }
}
