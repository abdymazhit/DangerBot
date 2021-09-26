package net.abdymazhit.mthd.customs;

import net.abdymazhit.mthd.enums.GameState;

/**
 * Представляет собой активную игру
 *
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class LiveGame {

    /** Id активной игры */
    public int id;

    /** Название первой команды */
    public String firstTeamName;

    /** Имя капитана первой команды */
    public String firstTeamCaptainName;

    /** Название второй команды */
    public String secondTeamName;

    /** Имя капитана второй команды */
    public String secondTeamCaptainName;

    /** Формат игры */
    public String format;

    /** Имя помощника */
    public String assistantName;

    /** Стадия игры */
    public GameState gameState;

    /**
     * Инициализирует активную игру
     * @param id Id активной игры
     * @param firstTeamName Название первой команды
     * @param secondTeamName Название второй команды
     * @param format Формат игры
     * @param assistantName Имя помощника
     * @param gameState Стадия игры
     */
    public LiveGame(int id, String firstTeamName, String secondTeamName, String format, String assistantName, GameState gameState) {
        this.id = id;
        this.firstTeamName = firstTeamName;
        this.secondTeamName = secondTeamName;
        this.format = format;
        this.assistantName = assistantName;
        this.gameState = gameState;
    }

    /**
     * Инициализирует активную игру
     * @param id Id активной игры
     * @param gameState Стадия игры
     * @param firstTeamCaptainName Имя капитана первой команды
     * @param secondTeamCaptainName Имя капитана второй команды
     * @param format Формат игры
     * @param assistantName Имя помощника
     */
    public LiveGame(int id, GameState gameState, String firstTeamCaptainName, String secondTeamCaptainName, String format, String assistantName) {
        this.id = id;
        this.gameState = gameState;
        this.firstTeamCaptainName = firstTeamCaptainName;
        this.secondTeamCaptainName = secondTeamCaptainName;
        this.format = format;
        this.assistantName = assistantName;
    }
}