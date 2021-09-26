package net.abdymazhit.mthd.managers;

import com.google.gson.*;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.customs.serialization.LatestGame;
import net.abdymazhit.mthd.customs.serialization.Match;
import net.abdymazhit.mthd.customs.serialization.Player;
import net.abdymazhit.mthd.customs.serialization.Team;
import net.abdymazhit.mthd.enums.Rating;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Менеджер активных игр
 *
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class LiveGamesManager {

    /** Список активных игр */
    private final List<Game> liveGames;

    /** Значение, начата ли проверка активных игр*/
    private boolean isStartedChecking;

    /** Таймер проверки активных игр */
    private Timer timer;

    /** Gson объект */
    private final Gson gson;

    /**
     * Инициализирует менеджер игры
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
        if(game.firstTeamPlayersVimeId == null || game.secondTeamPlayersVimeId == null) {
            game.setFirstTeamPlayersIds();
            game.setSecondTeamPlayersIds();
        }

        if(!liveGames.contains(game)) {
            liveGames.add(game);
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

                String latestGamesString = MTHD.getInstance().utils.sendGetRequest(
                    "https://api.vimeworld.ru/match/latest?count=100&token=" + MTHD.getInstance().config.vimeApiToken);
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
                            if(latestGame.getMap().getName().equals(game.gameMap.getName())) {
                                games.add(latestGame);
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
        String matchString = MTHD.getInstance().utils.sendGetRequest(
            "https://api.vimeworld.ru/match/" + matchId + "?token=" + MTHD.getInstance().config.vimeApiToken);
        Match match = gson.fromJson(matchString, Match.class);
        if(match == null) {
            return "Ошибка! Не удалось найти матч!";
        }

        List<Game> games = new ArrayList<>(liveGames);
        for(Game liveGame : games) {
            boolean hasFirstTeamPlayer = false;
            boolean hasSecondTeamPlayer = false;

            if(liveGame.firstTeamPlayersVimeId == null || liveGame.secondTeamPlayersVimeId == null) {
                liveGame.setFirstTeamPlayersIds();
                liveGame.setSecondTeamPlayersIds();
            }

            for(Player player : match.getPlayers()) {
                if(player.getId().equals(liveGame.firstTeamPlayersVimeId.get(0))) {
                    hasFirstTeamPlayer = true;
                }

                if(player.getId().equals(liveGame.secondTeamPlayersVimeId.get(0))) {
                    hasSecondTeamPlayer = true;
                }
            }

            if(!hasFirstTeamPlayer || !hasSecondTeamPlayer) {
                return "Ошибка! В данной не участовали игроки этой игры!";
            }

            if(!liveGame.startedAt.before(Timestamp.from(Instant.ofEpochSecond(match.getEnd())))) {
                return "Ошибка! Вы пытаетесь установить id матча, которая была завершена до начала этой игры!";
            }

            String winnerTeam = match.getWinner().getTeam();
            for(Team team : match.getTeams()) {
                if(team.getId().equals(winnerTeam)) {
                    if(liveGame.firstTeamPlayersVimeId.contains(team.getMembers().get(0))) {
                        if(liveGame.rating.equals(Rating.TEAM_RATING)) {
                            int firstTeamRating = getTeamRating(liveGame.firstTeam.points,
                                    liveGame.secondTeam.points, true);
                            int secondTeamRating = getTeamRating(liveGame.secondTeam.points,
                                    liveGame.firstTeam.points, false);

                            MTHD.getInstance().gameManager.teamGameManager.finishGame(liveGame, matchId,
                                    liveGame.firstTeam.id, firstTeamRating, secondTeamRating);

                            if(team.getBedAlive()) {
                                MTHD.getInstance().gameManager.teamGameManager.finishGameTeam(firstTeamRating, 1, 1,
                                        1, 0, liveGame.firstTeam.id);
                                MTHD.getInstance().gameManager.teamGameManager.finishGameTeam(secondTeamRating, 1, 0,
                                        0, 1, liveGame.secondTeam.id);
                            } else {
                                MTHD.getInstance().gameManager.teamGameManager.finishGameTeam(firstTeamRating, 1, 1,
                                        1, 1, liveGame.firstTeam.id);
                                MTHD.getInstance().gameManager.teamGameManager.finishGameTeam(secondTeamRating, 1, 0,
                                        1, 1, liveGame.secondTeam.id);
                            }
                        } else {
                            int firstPoints = MTHD.getInstance().database.getSingleTeamPoints(liveGame.id, 0);
                            int secondPoints = MTHD.getInstance().database.getSingleTeamPoints(liveGame.id, 1);

                            if(liveGame.format.equals("4x2")) {
                                firstPoints /= 4;
                                secondPoints /= 4;
                            } else {
                                firstPoints /= 6;
                                secondPoints /= 6;
                            }

                            int firstTeamRating = getTeamRating(firstPoints, secondPoints, true) - firstPoints;
                            int secondTeamRating = getTeamRating(secondPoints, firstPoints, false) - secondPoints;

                            MTHD.getInstance().gameManager.singleGameManager.finishGame(liveGame, matchId, 0, firstTeamRating, secondTeamRating);

                            for(int id : liveGame.firstTeamPlayersId) {
                                MTHD.getInstance().gameManager.singleGameManager.finishGamePlayer(firstTeamRating, 1, 1, id);
                            }

                            for(int id : liveGame.secondTeamPlayersId) {
                                MTHD.getInstance().gameManager.singleGameManager.finishGamePlayer(secondTeamRating, 1, 0, id);
                            }
                        }
                    } else if(liveGame.secondTeamPlayersVimeId.contains(team.getMembers().get(0))) {
                        if(liveGame.rating.equals(Rating.TEAM_RATING)) {
                            int firstTeamRating = getTeamRating(liveGame.firstTeam.points,
                                    liveGame.secondTeam.points, false);
                            int secondTeamRating = getTeamRating(liveGame.secondTeam.points,
                                    liveGame.firstTeam.points, true);

                            MTHD.getInstance().gameManager.teamGameManager.finishGame(liveGame, matchId,
                                    liveGame.secondTeam.id, firstTeamRating, secondTeamRating);

                            if(team.getBedAlive()) {
                                MTHD.getInstance().gameManager.teamGameManager.finishGameTeam(secondTeamRating, 1, 1,
                                        1, 0, liveGame.secondTeam.id);
                                MTHD.getInstance().gameManager.teamGameManager.finishGameTeam(firstTeamRating, 1, 0,
                                        0, 1, liveGame.firstTeam.id);
                            } else {
                                MTHD.getInstance().gameManager.teamGameManager.finishGameTeam(secondTeamRating, 1, 1,
                                        1, 1, liveGame.secondTeam.id);
                                MTHD.getInstance().gameManager.teamGameManager.finishGameTeam(firstTeamRating, 1, 0,
                                        1, 1, liveGame.firstTeam.id);
                            }
                        } else {
                            int firstPoints = MTHD.getInstance().database.getSingleTeamPoints(liveGame.id, 0);
                            int secondPoints = MTHD.getInstance().database.getSingleTeamPoints(liveGame.id, 1);

                            if(liveGame.format.equals("4x2")) {
                                firstPoints /= 4;
                                secondPoints /= 4;
                            } else {
                                firstPoints /= 6;
                                secondPoints /= 6;
                            }

                            int firstTeamRating = getTeamRating(firstPoints, secondPoints, false) - firstPoints;
                            int secondTeamRating = getTeamRating(secondPoints, firstPoints, true) - secondPoints;

                            MTHD.getInstance().gameManager.singleGameManager.finishGame(liveGame, matchId,
                                    1, firstTeamRating, secondTeamRating);

                            for(int id : liveGame.firstTeamPlayersId) {
                                MTHD.getInstance().gameManager.singleGameManager.finishGamePlayer(firstTeamRating, 1, 0, id);
                            }

                            for(int id : liveGame.secondTeamPlayersId) {
                                MTHD.getInstance().gameManager.singleGameManager.finishGamePlayer(secondTeamRating, 1, 1, id);
                            }
                        }
                    }

                    MTHD.getInstance().liveGamesManager.removeLiveGame(liveGame);
                }
            }
        }

        return null;
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
