package net.abdymazhit.mthd.game;

import com.google.gson.*;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.customs.latestgame.LatestGame;
import net.abdymazhit.mthd.customs.match.Match;
import net.abdymazhit.mthd.customs.match.Player;
import net.abdymazhit.mthd.customs.match.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Менеджер активных игр
 *
 * @version   17.09.2021
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
        gson = new Gson();
    }

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

    public void removeLiveGame(Game game) {
        liveGames.remove(game);
        if(liveGames.isEmpty()) {
            cancelChecking();
        }
    }

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
                "https://api.vimeworld.ru/match/" + matchId +
                        "?token=" + MTHD.getInstance().config.vimeApiToken);

        Match match = gson.fromJson(matchString, Match.class);
        if(match == null) {
            return "Ошибка! Не удалось найти матч!";
        }

        for(Game liveGame : liveGames) {
            boolean hasFirstTeamPlayer = false;
            boolean hasSecondTeamPlayer = false;

            for(Player player : match.getPlayers()) {
                if(player.getId().equals(liveGame.firstTeamPlayersVimeId.get(0))) {
                    hasFirstTeamPlayer = true;
                }

                if(player.getId().equals(liveGame.secondTeamPlayersVimeId.get(0))) {
                    hasSecondTeamPlayer = true;
                }

                if(hasFirstTeamPlayer && hasSecondTeamPlayer) {
                    break;
                }
            }

            if(hasFirstTeamPlayer && hasSecondTeamPlayer) {
                String winnerTeam = match.getWinner().getTeam();
                for(Team team : match.getTeams()) {
                    if(team.getId().equals(winnerTeam)) {
                        if(liveGame.firstTeamPlayersVimeId.contains(team.getMembers().get(0))) {
                            int firstTeamRating = getTeamRating(liveGame.firstTeamPoints,
                                    liveGame.secondTeamPoints, true);
                            int secondTeamRating = getTeamRating(liveGame.secondTeamPoints,
                                    liveGame.firstTeamPoints, false);

                            MTHD.getInstance().gameManager.finishGame(liveGame, matchId,
                                    liveGame.firstTeamId, firstTeamRating, secondTeamRating);

                            if(team.getBedAlive()) {
                                MTHD.getInstance().gameManager.finishGameTeam(firstTeamRating, 1, 1,
                                        1, 0, liveGame.firstTeamId);
                                MTHD.getInstance().gameManager.finishGameTeam(secondTeamRating, 1, 0,
                                        0, 1, liveGame.secondTeamId);
                            } else {
                                MTHD.getInstance().gameManager.finishGameTeam(firstTeamRating, 1, 1,
                                        1, 1, liveGame.firstTeamId);
                                MTHD.getInstance().gameManager.finishGameTeam(secondTeamRating, 1, 0,
                                        1, 1, liveGame.secondTeamId);
                            }
                        } else if(liveGame.secondTeamPlayersVimeId.contains(team.getMembers().get(0))) {
                            int firstTeamRating = getTeamRating(liveGame.firstTeamPoints,
                                    liveGame.secondTeamPoints, false);
                            int secondTeamRating = getTeamRating(liveGame.secondTeamPoints,
                                    liveGame.firstTeamPoints, true);

                            MTHD.getInstance().gameManager.finishGame(liveGame, matchId,
                                    liveGame.secondTeamId, firstTeamRating, secondTeamRating);

                            if(team.getBedAlive()) {
                                MTHD.getInstance().gameManager.finishGameTeam(secondTeamRating, 1, 1,
                                        1, 0, liveGame.secondTeamId);
                                MTHD.getInstance().gameManager.finishGameTeam(firstTeamRating, 1, 0,
                                        0, 1, liveGame.firstTeamId);
                            } else {
                                MTHD.getInstance().gameManager.finishGameTeam(secondTeamRating, 1, 1,
                                        1, 1, liveGame.secondTeamId);
                                MTHD.getInstance().gameManager.finishGameTeam(firstTeamRating, 1, 0,
                                        1, 1, liveGame.firstTeamId);
                            }
                        }

                        MTHD.getInstance().liveGamesManager.removeLiveGame(liveGame);
                    }
                }
            } else {
                return "Ошибка! Вы пытаетесь установить id матча в которой не участвовали эти команды!";
            }
        }

        return null;
    }

    private int getTeamRating(int currentTeamPoints, int enemyPoints, boolean isWinner) {
        double pow = Math.pow(10, (enemyPoints - currentTeamPoints) / 400.0);
        double expectedScore = (1 / (1 + pow));
        if(isWinner) {
            return (int) (currentTeamPoints + (20 * (1 - expectedScore)));
        } else {
            return (int) (currentTeamPoints + (20 * (0 - expectedScore)));
        }
    }

    private void cancelChecking() {
        isStartedChecking = false;
        timer.cancel();
    }
}
