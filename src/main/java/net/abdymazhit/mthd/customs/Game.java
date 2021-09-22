package net.abdymazhit.mthd.customs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Представляет собой игру
 *
 * @version   22.09.2021
 * @author    Islam Abdymazhit
 */
public class Game {

    /** Id матча */
    public int id;

    /** Id первой команды */
    public int firstTeamId;

    /** Очки первой команды */
    public int firstTeamPoints;

    /** Название первой команды */
    public String firstTeamName;

    /** Id начавшего первой команды */
    public int firstTeamStarterId;

    /** Discord id начавшего первой команды */
    public String firstTeamStarterDiscordId;

    /** Id второй команды */
    public int secondTeamId;

    /** Очки второй команды */
    public int secondTeamPoints;

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

    /** Стадия игры */
    public GameState gameState;

    /** Игроки первой команды */
    public List<String> firstTeamPlayers;

    /** Id игроков первой команды */
    public List<Integer> firstTeamPlayersId;

    /** Vime id игроков первой команды */
    public List<Integer> firstTeamPlayersVimeId;

    /** Игроки второй команды */
    public List<String> secondTeamPlayers;

    /** Id игроков второй команды */
    public List<Integer> secondTeamPlayersId;

    /** Vime id игроков второй команды */
    public List<Integer> secondTeamPlayersVimeId;

    /**
     * Инициализирует матч
     * @param id Id матча
     * @param firstTeamId Id первой команды
     * @param firstTeamStarterId Id начавшего первой команды
     * @param secondTeamId Id второй команды
     * @param secondTeamStarterId Id начавшего второй команды
     * @param format Формат матча
     * @param gameMap Выбранная карта
     * @param gameState Стадия игры
     * @param assistantId Id помощника
     * @param startedAt Время начала матча
     */
    public Game(int id, int firstTeamId, int firstTeamStarterId,  int secondTeamId, int secondTeamStarterId, String format,
        GameMap gameMap, GameState gameState, int assistantId, Timestamp startedAt) {
        this.id = id;
        this.firstTeamId = firstTeamId;
        this.firstTeamStarterId = firstTeamStarterId;
        this.secondTeamId = secondTeamId;
        this.secondTeamStarterId = secondTeamStarterId;
        this.format = format;
        this.gameMap = gameMap;
        this.gameState = gameState;
        this.assistantId = assistantId;
        this.startedAt = startedAt;
    }

    /**
     * Инициализирует игру
     * @param id Id игры
     * @param firstTeamId Id первой команды
     * @param firstTeamStarterId Id начавшего первой команды
     * @param secondTeamId Id второй команды
     * @param secondTeamStarterId Id начавшего второй команды
     */
    public Game(int id, int firstTeamId, int firstTeamStarterId, int secondTeamId,
        int secondTeamStarterId, String format, int assistantId) {
        this.id = id;
        this.firstTeamId = firstTeamId;
        this.firstTeamStarterId = firstTeamStarterId;
        this.secondTeamId = secondTeamId;
        this.secondTeamStarterId = secondTeamStarterId;
        this.format = format;
        this.assistantId = assistantId;
    }

    /**
     * Получает подробную информацию о команде
     */
    public void getData() {
        firstTeamPoints = MTHD.getInstance().database.getTeamPoints(firstTeamId);
        firstTeamName = MTHD.getInstance().database.getTeamName(firstTeamId);
        firstTeamStarterDiscordId = MTHD.getInstance().database.getUserDiscordId(firstTeamStarterId);
        secondTeamPoints = MTHD.getInstance().database.getTeamPoints(secondTeamId);
        secondTeamName = MTHD.getInstance().database.getTeamName(secondTeamId);
        secondTeamStarterDiscordId = MTHD.getInstance().database.getUserDiscordId(secondTeamStarterId);
        assistantName = MTHD.getInstance().database.getUserName(assistantId);
        assistantDiscordId = MTHD.getInstance().database.getUserDiscordId(assistantId);
    }

    public void setFirstTeamPlayersIds() {
        firstTeamPlayersId = new ArrayList<>();
        for(String playerName : firstTeamPlayers) {
            int playerId = MTHD.getInstance().database.getUserIdByUsername(playerName);
            if(playerId > 0) {
                firstTeamPlayersId.add(playerId);
            }
        }

        firstTeamPlayersVimeId = new ArrayList<>();
        StringBuilder names = new StringBuilder();
        for(String name : firstTeamPlayers) {
            names.append(name).append(",");
        }

        String info = MTHD.getInstance().utils.sendGetRequest("https://api.vimeworld.ru/user/name/" + names +
                                                              "?token=" + MTHD.getInstance().config.vimeApiToken);
        if(info == null) return;

        JsonArray infoArray = JsonParser.parseString(info).getAsJsonArray();
        for(JsonElement infoElement : infoArray) {
            JsonObject infoObject = infoElement.getAsJsonObject();

            int vimeId = infoObject.get("id").getAsInt();
            firstTeamPlayersVimeId.add(vimeId);
        }
    }

    public void setSecondTeamPlayersIds() {
        secondTeamPlayersId = new ArrayList<>();
        for(String playerName : secondTeamPlayers) {
            int playerId = MTHD.getInstance().database.getUserIdByUsername(playerName);
            if(playerId > 0) {
                secondTeamPlayersId.add(playerId);
            }
        }

        secondTeamPlayersVimeId = new ArrayList<>();
        StringBuilder names = new StringBuilder();
        for(String name : secondTeamPlayers) {
            names.append(name).append(",");
        }

        String info = MTHD.getInstance().utils.sendGetRequest("https://api.vimeworld.ru/user/name/" + names +
                                                              "?token=" + MTHD.getInstance().config.vimeApiToken);
        if(info == null) return;

        JsonArray infoArray = JsonParser.parseString(info).getAsJsonArray();
        for(JsonElement infoElement : infoArray) {
            JsonObject infoObject = infoElement.getAsJsonObject();

            int vimeId = infoObject.get("id").getAsInt();
            secondTeamPlayersVimeId.add(vimeId);
        }
    }
}
