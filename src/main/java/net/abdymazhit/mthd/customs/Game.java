package net.abdymazhit.mthd.customs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.info.TeamInfo;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.Rating;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Представляет собой игру
 *
 * @version   21.10.2021
 * @author    Islam Abdymazhit
 */
public class Game {

    /** Рейтинговая система игры */
    public final Rating rating;

    /** Id матча */
    public int id;

    /** Первая команда */
    public TeamInfo firstTeamInfo;

    /** Вторая команда */
    public TeamInfo secondTeamInfo;

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

    /** Участвующие игроки игры */
    public List<UserAccount> playersAccounts;

    /**
     * Инициализирует новую Team Rating игру
     * @param rating Рейтинговая система игры
     * @param id Id игры
     * @param format Формат игры
     * @param firstTeamId Id первой команды
     * @param firstTeamCaptainId Id капитана первой команды
     * @param secondTeamId Id второй команды
     * @param secondTeamCaptainId Id капитана второй команды
     * @param assistantId Id помощника игры
     */
    public Game(Rating rating, int id, String format, int firstTeamId, int firstTeamCaptainId, int secondTeamId, int secondTeamCaptainId, int assistantId) {
        this.rating = rating;
        this.id = id;
        this.format = format;
        firstTeamInfo = new TeamInfo(firstTeamId);
        firstTeamInfo.captain = new UserAccount(firstTeamCaptainId);
        secondTeamInfo = new TeamInfo(secondTeamId);
        secondTeamInfo.captain = new UserAccount(secondTeamCaptainId);
        assistantAccount = new UserAccount(assistantId);
        startedAt = Timestamp.from(Instant.now());
        getData();
    }

    /**
     * Инициализирует новую Single Rating игру
     * @param rating Рейтинговая система игры
     * @param id Id игры
     * @param format Формат игры
     * @param firstTeamCaptainId Id капитана первой команды
     * @param secondTeamCaptainId Id капитана второй команды
     * @param assistantId Id помощника игры
     * @param playersIds Id участвующих игроков
     */
    public Game(Rating rating, int id, String format, int firstTeamCaptainId, int secondTeamCaptainId, int assistantId, List<Integer> playersIds) {
        this.rating = rating;
        this.id = id;
        this.format = format;
        startedAt = Timestamp.from(Instant.now());
        firstTeamInfo = new TeamInfo();
        firstTeamInfo.captain = new UserAccount(firstTeamCaptainId);
        secondTeamInfo = new TeamInfo();
        secondTeamInfo.captain = new UserAccount(secondTeamCaptainId);
        assistantAccount = new UserAccount(assistantId);
        playersAccounts = new ArrayList<>();
        for(int playerId : playersIds) {
            playersAccounts.add(new UserAccount(playerId));
        }
        getData();
    }

    /**
     * Инициализирует уже существующую Team Rating игру
     * @param rating Рейтинговая система игры
     * @param id Id игры
     * @param format Формат игры
     * @param gameMap Карта игры
     * @param gameState Стадия игры
     * @param startedAt Время начала игры
     * @param firstTeamId Id первой команды
     * @param firstTeamCaptainId Id капитана первой команды
     * @param secondTeamId Id второй команды
     * @param secondTeamCaptainId Id капитана второй команды
     * @param assistantId Id помощника игры
     */
    public Game(Rating rating, int id, String format, GameMap gameMap, GameState gameState, Timestamp startedAt,
                int firstTeamId, int firstTeamCaptainId,  int secondTeamId, int secondTeamCaptainId, int assistantId) {
        this.rating = rating;
        this.id = id;
        this.format = format;
        this.gameMap = gameMap;
        this.gameState = gameState;
        this.startedAt = startedAt;
        firstTeamInfo = new TeamInfo(firstTeamId);
        firstTeamInfo.captain = new UserAccount(firstTeamCaptainId);
        secondTeamInfo = new TeamInfo(secondTeamId);
        secondTeamInfo.captain = new UserAccount(secondTeamCaptainId);
        assistantAccount = new UserAccount(assistantId);
        getData();
    }

    /**
     * Инициализирует уже существующую Single Rating игру
     * @param rating Рейтинговая система игры
     * @param id Id игры
     * @param format Формат игры
     * @param gameMap Карта игры
     * @param gameState Стадия игры
     * @param startedAt Время начала игры
     * @param firstTeamCaptainId Id капитана первой команды
     * @param secondTeamCaptainId Id капитана второй команды
     * @param assistantId Id помощника игры
     * @param playersIds Id участвующих игроков
     */
    public Game(Rating rating, int id, String format, GameMap gameMap, GameState gameState, Timestamp startedAt, int firstTeamCaptainId, int secondTeamCaptainId,
                int assistantId, List<Integer> playersIds) {
        this.rating = rating;
        this.id = id;
        this.format = format;
        this.gameMap = gameMap;
        this.gameState = gameState;
        this.startedAt = startedAt;
        firstTeamInfo = new TeamInfo();
        firstTeamInfo.captain = new UserAccount(firstTeamCaptainId);
        secondTeamInfo = new TeamInfo();
        secondTeamInfo.captain = new UserAccount(secondTeamCaptainId);
        assistantAccount = new UserAccount(assistantId);
        playersAccounts = new ArrayList<>();
        for(int playerId : playersIds) {
            playersAccounts.add(new UserAccount(playerId));
        }
        getData();
    }

    /**
     * Получает подробную информацию о команде
     */
    private void getData() {
        firstTeamInfo.captain.discordId = MTHD.getInstance().database.getUserDiscordId(firstTeamInfo.captain.id);
        firstTeamInfo.captain.username = MTHD.getInstance().database.getUserName(firstTeamInfo.captain.id);
        secondTeamInfo.captain.discordId = MTHD.getInstance().database.getUserDiscordId(secondTeamInfo.captain.id);
        secondTeamInfo.captain.username = MTHD.getInstance().database.getUserName(secondTeamInfo.captain.id);
        assistantAccount.username = MTHD.getInstance().database.getUserName(assistantAccount.id);
        assistantAccount.discordId = MTHD.getInstance().database.getUserDiscordId(assistantAccount.id);

        if(rating.equals(Rating.TEAM_RATING)) {
            firstTeamInfo.name = MTHD.getInstance().database.getTeamName(firstTeamInfo.id);
            firstTeamInfo.points = MTHD.getInstance().database.getTeamPoints(firstTeamInfo.id);
            secondTeamInfo.name = MTHD.getInstance().database.getTeamName(secondTeamInfo.id);
            secondTeamInfo.points = MTHD.getInstance().database.getTeamPoints(secondTeamInfo.id);
        } else {
            firstTeamInfo.captain.member = MTHD.getInstance().guild.getMemberById(firstTeamInfo.captain.discordId);
            secondTeamInfo.captain.member = MTHD.getInstance().guild.getMemberById(secondTeamInfo.captain.discordId);
            assistantAccount.member = MTHD.getInstance().guild.getMemberById(assistantAccount.discordId);
        }
    }

    /**
     * Установить id участников первой команды
     */
    public void setFirstTeamPlayersIds() {
        for(UserAccount userAccount : firstTeamInfo.members) {
            int playerId = MTHD.getInstance().database.getUserIdByUsername(userAccount.username);
            if(playerId > 0) {
                userAccount.id = playerId;
            }
        }

        StringBuilder names = new StringBuilder();
        for(UserAccount userAccount : firstTeamInfo.members) {
            names.append(userAccount.username).append(",");
        }

        String info = MTHD.getInstance().utils.sendGetRequest("https://api.vimeworld.ru/user/name/%names%?token=%token%"
                .replace("%names%", names)
                .replace("%token%", MTHD.getInstance().config.vimeApiToken));
        if(info == null) return;

        for(JsonElement infoElement : JsonParser.parseString(info).getAsJsonArray()) {
            JsonObject jsonObject = infoElement.getAsJsonObject();
            int vimeId = jsonObject.get("id").getAsInt();
            String username = jsonObject.get("username").getAsString();

            for(UserAccount userAccount : firstTeamInfo.members) {
                if(userAccount.username.equals(username)) {
                    userAccount.vimeId = vimeId;
                }

                if(firstTeamInfo.captain.username.equals(username)) {
                    firstTeamInfo.captain.vimeId = vimeId;
                }
            }
        }
    }

    /**
     * Установить id участников второй команды
     */
    public void setSecondTeamPlayersIds() {
        for(UserAccount userAccount : secondTeamInfo.members) {
            int playerId = MTHD.getInstance().database.getUserIdByUsername(userAccount.username);
            if(playerId > 0) {
                userAccount.id = playerId;
            }
        }

        StringBuilder names = new StringBuilder();
        for(UserAccount userAccount : secondTeamInfo.members) {
            names.append(userAccount.username).append(",");
        }

        String info = MTHD.getInstance().utils.sendGetRequest("https://api.vimeworld.ru/user/name/%names%?token=%token%"
                .replace("%names%", names)
                .replace("%token%", MTHD.getInstance().config.vimeApiToken));
        if(info == null) return;

        for(JsonElement infoElement : JsonParser.parseString(info).getAsJsonArray()) {
            JsonObject jsonObject = infoElement.getAsJsonObject();
            int vimeId = jsonObject.get("id").getAsInt();
            String username = jsonObject.get("username").getAsString();

            for(UserAccount userAccount : secondTeamInfo.members) {
                if(userAccount.username.equals(username)) {
                    userAccount.vimeId = vimeId;
                }

                if(secondTeamInfo.captain.username.equals(username)) {
                    secondTeamInfo.captain.vimeId = vimeId;
                }
            }
        }
    }
}
