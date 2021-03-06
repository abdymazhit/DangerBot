package net.abdymazhit.dangerbot.managers;

import com.google.gson.*;
import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Game;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.customs.serialization.*;
import net.abdymazhit.dangerbot.enums.Rating;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Менеджер активных игр
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class LiveGamesManager {

    /** Список активных игр */
    private final List<Game> liveGames;

    /** Значение, начата ли проверка активных игр */
    private boolean isStartedChecking;

    /** Таймер проверки активных игр */
    private Timer timer;

    /** Gson объект */
    private final Gson gson;

    /**
     * Инициализирует менеджер активных игр
     */
    public LiveGamesManager() {
        liveGames = new ArrayList<>();
        isStartedChecking = false;
        gson = new GsonBuilder().setLenient().create();
    }

    /**
     * Добавляет игру в активные игры
     * @param game Активная игра
     */
    public void addLiveGame(Game game) {
        if(!liveGames.contains(game)) {
            liveGames.add(game);
        }

        if(!game.firstTeamInfo.members.isEmpty()) {
            UserAccount userAccount = game.firstTeamInfo.members.get(0);
            if(userAccount.vimeId <= 0) {
                game.setFirstTeamPlayersIds();
            }
        }

        if(!game.secondTeamInfo.members.isEmpty()) {
            UserAccount userAccount = game.secondTeamInfo.members.get(0);
            if(userAccount.vimeId <= 0) {
                game.setSecondTeamPlayersIds();
            }
        }

        if(!isStartedChecking) {
            startChecking();
        }
    }

    /**
     * Удаляет игру из активных игр
     * @param game Активная игра
     */
    public void removeLiveGame(Game game) {
        liveGames.remove(game);
        if(liveGames.isEmpty()) {
            cancelChecking();
        }
    }

    /**
     * Начинает проверку результатов
     */
    private void startChecking() {
        isStartedChecking = true;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<LatestGame> latestGames = new ArrayList<>();

                String latestGamesString = DangerBot.getInstance().utils.sendGetRequest(
                        "https://api.vimeworld.ru/match/latest?count=100&token=%token%"
                        .replace("%token%", DangerBot.getInstance().config.vimeApiToken));
                JsonArray infoArray = JsonParser.parseString(latestGamesString).getAsJsonArray();
                for(JsonElement infoElement : infoArray) {
                    JsonObject infoObject = infoElement.getAsJsonObject();

                    LatestGame latestGame = gson.fromJson(infoObject, LatestGame.class);
                    latestGames.add(latestGame);
                }

                List<LatestGame> games = new ArrayList<>();
                for(LatestGame latestGame : latestGames) {
                    if(latestGame.getGame().equals("BWH")) {
                        for(Game game : liveGames) {
                            if(latestGame.getMap() != null && game.gameMap != null) {
                                if(latestGame.getMap().getName().equals(game.gameMap.getName())) {
                                    games.add(latestGame);
                                }
                            }
                        }
                    }
                }

                for(LatestGame latestGame : games) {
                    finishMatch(latestGame.getId());
                }
            }
        }, 0, 20000);
    }

    /**
     * Заканчивает игру
     * @param matchId Id матча
     * @return Текст ошибки заканчивания игры
     */
    public String finishMatch(String matchId) {
        String matchString = DangerBot.getInstance().utils.sendGetRequest("https://api.vimeworld.ru/match/%match_id%?token=%token%"
                .replace("%match_id%", matchId)
                .replace("%token%", DangerBot.getInstance().config.vimeApiToken));
        Match match = gson.fromJson(matchString, Match.class);
        if(match == null) {
            return "Ошибка! Не удалось найти матч!";
        }

        List<Game> games = new ArrayList<>(liveGames);
        for(Game liveGame : games) {
            finishGameMatch(liveGame, match, matchId);
        }
        return null;
    }

    /**
     * Заканчивает матч игры
     * @param liveGame Игра
     * @param match Матч
     * @param matchId Id матча
     */
    private void finishGameMatch(Game liveGame, Match match, String matchId) {
        boolean hasFirstTeamPlayer = false;
        boolean hasSecondTeamPlayer = false;

        List<Integer> firstTeamPlayersVimeId = new ArrayList<>();
        for(UserAccount userAccount : liveGame.firstTeamInfo.members) {
            firstTeamPlayersVimeId.add(userAccount.vimeId);
        }

        List<Integer> secondTeamPlayersVimeId = new ArrayList<>();
        for(UserAccount userAccount : liveGame.secondTeamInfo.members) {
            secondTeamPlayersVimeId.add(userAccount.vimeId);
        }

        if(match.getPlayers() == null) return;

        for(Player player : match.getPlayers()) {
            if(player.getId().equals(firstTeamPlayersVimeId.get(0))) {
                hasFirstTeamPlayer = true;
            }

            if(player.getId().equals(secondTeamPlayersVimeId.get(0))) {
                hasSecondTeamPlayer = true;
            }
        }

        if(!hasFirstTeamPlayer || !hasSecondTeamPlayer) return;

        if(!liveGame.startedAt.before(Timestamp.from(Instant.ofEpochSecond(match.getEnd())))) return;

        String winnerTeam = match.getWinner().getTeam();
        for(Team team : match.getTeams()) {
            if(team.getId().equals(winnerTeam)) {
                if(firstTeamPlayersVimeId.contains(team.getMembers().get(0))) {
                    if(liveGame.rating.equals(Rating.TEAM_RATING)) {
                        int firstTeamRating = getTeamRating(liveGame.firstTeamInfo.points,
                                liveGame.secondTeamInfo.points, true);
                        int secondTeamRating = getTeamRating(liveGame.secondTeamInfo.points,
                                liveGame.firstTeamInfo.points, false);

                        DangerBot.getInstance().gameManager.teamGameManager.finishGame(liveGame, matchId,
                                liveGame.firstTeamInfo.id, firstTeamRating, secondTeamRating);

                        if(team.getBedAlive()) {
                            DangerBot.getInstance().gameManager.teamGameManager.finishGameTeam(firstTeamRating, 1, 1,
                                    1, 0, liveGame.firstTeamInfo.id);
                            DangerBot.getInstance().gameManager.teamGameManager.finishGameTeam(secondTeamRating, 1, 0,
                                    0, 1, liveGame.secondTeamInfo.id);
                        } else {
                            DangerBot.getInstance().gameManager.teamGameManager.finishGameTeam(firstTeamRating, 1, 1,
                                    1, 1, liveGame.firstTeamInfo.id);
                            DangerBot.getInstance().gameManager.teamGameManager.finishGameTeam(secondTeamRating, 1, 0,
                                    1, 1, liveGame.secondTeamInfo.id);
                        }
                    } else {
                        int firstPoints = DangerBot.getInstance().database.getSingleTeamPoints(liveGame.id, 0);
                        int secondPoints = DangerBot.getInstance().database.getSingleTeamPoints(liveGame.id, 1);

                        if(liveGame.format.equals("4x2")) {
                            firstPoints /= 4;
                            secondPoints /= 4;
                        } else {
                            firstPoints /= 6;
                            secondPoints /= 6;
                        }

                        int firstTeamRating = getTeamRating(firstPoints, secondPoints, true) - firstPoints;
                        int secondTeamRating = getTeamRating(secondPoints, firstPoints, false) - secondPoints;

                        DangerBot.getInstance().gameManager.singleGameManager.finishGame(liveGame, matchId, 0, firstTeamRating, secondTeamRating);
                    }
                } else if(secondTeamPlayersVimeId.contains(team.getMembers().get(0))) {
                    if(liveGame.rating.equals(Rating.TEAM_RATING)) {
                        int firstTeamRating = getTeamRating(liveGame.firstTeamInfo.points,
                                liveGame.secondTeamInfo.points, false);
                        int secondTeamRating = getTeamRating(liveGame.secondTeamInfo.points,
                                liveGame.firstTeamInfo.points, true);

                        DangerBot.getInstance().gameManager.teamGameManager.finishGame(liveGame, matchId,
                                liveGame.secondTeamInfo.id, firstTeamRating, secondTeamRating);

                        if(team.getBedAlive()) {
                            DangerBot.getInstance().gameManager.teamGameManager.finishGameTeam(secondTeamRating, 1, 1,
                                    1, 0, liveGame.secondTeamInfo.id);
                            DangerBot.getInstance().gameManager.teamGameManager.finishGameTeam(firstTeamRating, 1, 0,
                                    0, 1, liveGame.firstTeamInfo.id);
                        } else {
                            DangerBot.getInstance().gameManager.teamGameManager.finishGameTeam(secondTeamRating, 1, 1,
                                    1, 1, liveGame.secondTeamInfo.id);
                            DangerBot.getInstance().gameManager.teamGameManager.finishGameTeam(firstTeamRating, 1, 0,
                                    1, 1, liveGame.firstTeamInfo.id);
                        }
                    } else {
                        int firstPoints = DangerBot.getInstance().database.getSingleTeamPoints(liveGame.id, 0);
                        int secondPoints = DangerBot.getInstance().database.getSingleTeamPoints(liveGame.id, 1);

                        if(liveGame.format.equals("4x2")) {
                            firstPoints /= 4;
                            secondPoints /= 4;
                        } else {
                            firstPoints /= 6;
                            secondPoints /= 6;
                        }

                        int firstTeamRating = getTeamRating(firstPoints, secondPoints, false) - firstPoints;
                        int secondTeamRating = getTeamRating(secondPoints, firstPoints, true) - secondPoints;

                        DangerBot.getInstance().gameManager.singleGameManager.finishGame(liveGame, matchId, 1, firstTeamRating, secondTeamRating);
                    }
                }

                DangerBot.getInstance().liveGamesManager.removeLiveGame(liveGame);
            }
        }
    }

    /**
     * Получает очки команды
     * @param currentTeamPoints Очки текущей команды
     * @param enemyPoints Очки враждебной команды
     * @param isWinner Значение, является ли текущая команда победителем игры
     * @return Очки команды
     */
    private int getTeamRating(int currentTeamPoints, int enemyPoints, boolean isWinner) {
        double pow = Math.pow(10, (enemyPoints - currentTeamPoints) / 400.0);
        double expectedScore = (1 / (1 + pow));
        double addValue;
        if(isWinner) {
            addValue = (20 * (1 - expectedScore));
            addValue += addValue * 0.33;
        } else {
            addValue = (20 * (0 - expectedScore));
            addValue -= addValue * 0.33;
        }
        return (int) (currentTeamPoints + addValue);
    }

    /**
     * Заканчивает проверку результатов команд
     */
    private void cancelChecking() {
        isStartedChecking = false;
        if(timer != null) {
            timer.cancel();
        }
    }
}
