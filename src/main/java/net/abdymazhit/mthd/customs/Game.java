package net.abdymazhit.mthd.customs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.Rating;
import net.dv8tion.jda.api.entities.Member;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Представляет собой игру
 *
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class Game {

    /** Рейтинговая система игры */
    public final Rating rating;

    /** Id матча */
    public int id;

    /** Первая команда */
    public Team firstTeam;

    /** Капитан первой игры */
    public UserAccount firstTeamCaptain;

    /** Капитан первой игры */
    public Member firstTeamCaptainMember;

    /** Вторая команда */
    public Team secondTeam;

    /** Капитан второй игры */
    public UserAccount secondTeamCaptain;

    /** Капитан второй игры */
    public Member secondTeamCaptainMember;

    /** Помощник игры */
    public UserAccount assistantAccount;

    /** Время начала матча */
    public Timestamp startedAt;

    /** Формат матча */
    public String format;

    /** Выбранная карта */
    public GameMap gameMap;

    /** Стадия игры */
    public GameState gameState;

    /** Игроки */
    public List<String> players;

    /** Id игроков */
    public List<Integer> playersIds;

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
     * @param rating Рейтинговая система игры
     * @param id Id матча
     * @param firstTeamId Id первой команды
     * @param firstTeamCaptainId Id начавшего первой команды
     * @param secondTeamId Id второй команды
     * @param secondTeamCaptainId Id начавшего второй команды
     * @param format Формат матча
     * @param gameMap Выбранная карта
     * @param gameState Стадия игры
     * @param assistantId Id помощника
     * @param startedAt Время начала матча
     */
    public Game(Rating rating, int id, int firstTeamId, int firstTeamCaptainId,  int secondTeamId, int secondTeamCaptainId,
                String format, GameMap gameMap, GameState gameState, int assistantId, Timestamp startedAt) {
        this.rating = rating;
        this.id = id;
        this.firstTeam = new Team(firstTeamId);
        this.firstTeamCaptain = new UserAccount(firstTeamCaptainId);
        this.secondTeam = new Team(secondTeamId);
        this.secondTeamCaptain = new UserAccount(secondTeamCaptainId);
        this.format = format;
        this.gameMap = gameMap;
        this.gameState = gameState;
        this.assistantAccount = new UserAccount(assistantId);
        this.startedAt = startedAt;
    }

    /**
     * Инициализирует игру
     * @param rating Рейтинговая система игры
     * @param id Id игры
     * @param firstTeamId Id первой команды
     * @param firstTeamCaptainId Id начавшего первой команды
     * @param secondTeamId Id второй команды
     * @param secondTeamCaptainId Id начавшего второй команды
     * @param format Формат игры
     * @param assistantId Помощник игры
     */
    public Game(Rating rating, int id, int firstTeamId, int firstTeamCaptainId, int secondTeamId, int secondTeamCaptainId,
                String format, int assistantId) {
        this.rating = rating;
        this.id = id;
        this.firstTeam = new Team(firstTeamId);
        this.firstTeamCaptain = new UserAccount(firstTeamCaptainId);
        this.secondTeam = new Team(secondTeamId);
        this.secondTeamCaptain = new UserAccount(secondTeamCaptainId);
        this.format = format;
        this.assistantAccount = new UserAccount(assistantId);
    }

    /**
     * Инициализирует игру
     * @param rating Рейтинговая система игры
     * @param playersIds Id игроков
     * @param id Id игры
     * @param firstTeamCaptainId Id начавшего первой команды
     * @param secondTeamCaptainId Id начавшего второй команды
     * @param format Формат игры
     * @param assistantId Помощник игры
     */
    public Game(Rating rating, List<Integer> playersIds, int id, int firstTeamCaptainId, int secondTeamCaptainId,
                String format, int assistantId) {
        this.rating = rating;
        this.playersIds = playersIds;
        this.id = id;
        this.firstTeamCaptain = new UserAccount(firstTeamCaptainId);
        this.secondTeamCaptain = new UserAccount(secondTeamCaptainId);
        this.format = format;
        this.assistantAccount = new UserAccount(assistantId);
    }

    /**
     * Инициализирует игру
     * @param rating Рейтинговая система игры
     * @param playersIds Id игроков
     * @param id Id игры
     * @param firstTeamCaptainId Id начавшего первой команды
     * @param secondTeamCaptainId Id начавшего второй команды
     * @param format Формат игры
     * @param assistantId Помощник игры
     * @param startedAt Время начала игры
     */
    public Game(Rating rating, List<Integer> playersIds, int id, int firstTeamCaptainId, int secondTeamCaptainId,
                String format, GameMap gameMap, GameState gameState, int assistantId, Timestamp startedAt) {
        this.rating = rating;
        this.playersIds = playersIds;
        this.id = id;
        this.firstTeamCaptain = new UserAccount(firstTeamCaptainId);
        this.secondTeamCaptain = new UserAccount(secondTeamCaptainId);
        this.format = format;
        this.gameMap = gameMap;
        this.gameState = gameState;
        this.assistantAccount = new UserAccount(assistantId);
        this.startedAt = startedAt;
    }

    /**
     * Получает подробную информацию о команде
     */
    public void getData() {
        firstTeamCaptain.discordId = MTHD.getInstance().database.getUserDiscordId(firstTeamCaptain.id);
        firstTeamCaptain.username = MTHD.getInstance().database.getUserName(firstTeamCaptain.id);
        secondTeamCaptain.discordId = MTHD.getInstance().database.getUserDiscordId(secondTeamCaptain.id);
        secondTeamCaptain.username = MTHD.getInstance().database.getUserName(secondTeamCaptain.id);
        assistantAccount.username = MTHD.getInstance().database.getUserName(assistantAccount.id);
        assistantAccount.discordId = MTHD.getInstance().database.getUserDiscordId(assistantAccount.id);

        if(rating.equals(Rating.TEAM_RATING)) {
            firstTeam.points = MTHD.getInstance().database.getTeamPoints(firstTeam.id);
            firstTeam.name = MTHD.getInstance().database.getTeamName(firstTeam.id);
            secondTeam.points = MTHD.getInstance().database.getTeamPoints(secondTeam.id);
            secondTeam.name = MTHD.getInstance().database.getTeamName(secondTeam.id);
        } else {
            try {
                firstTeamCaptainMember = MTHD.getInstance().guild.retrieveMemberById(firstTeamCaptain.discordId).submit().get();
                secondTeamCaptainMember = MTHD.getInstance().guild.retrieveMemberById(secondTeamCaptain.discordId).submit().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Установить id участников первой команды
     */
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

    /**
     * Установить id участников второй команды
     */
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
