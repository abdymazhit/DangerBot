package net.abdymazhit.mthd.managers;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.Rating;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Менеджер Single Rating игр
 *
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class SingleGameManager {

    /** Менеджер игр */
    private final GameManager gameManager;

    /**
     * Инициализирует менеджер Single Rating игр
     */
    public SingleGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Попытается начать игру
     */
    public void tryStartGame() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT player_id, format FROM players_in_game_search;");
            ResultSet resultSet = preparedStatement.executeQuery();
            List<PlayerInGameSearch> players = new ArrayList<>();
            while(resultSet.next()) {
                int playerId = resultSet.getInt("player_id");
                String format = resultSet.getString("format");
                players.add(new PlayerInGameSearch(playerId, format));
            }

            // Недостаточно игроков для начала игры
            if(players.size() < 8) return;

            List<PlayerInGameSearch> playersIn4x2Format = new ArrayList<>();
            List<PlayerInGameSearch> playersIn6x2Format = new ArrayList<>();

            for(PlayerInGameSearch playerInGameSearch : players) {
                if(playerInGameSearch.format.equals("4x2")) {
                    playersIn4x2Format.add(playerInGameSearch);
                } else if(playerInGameSearch.format.equals("6x2")) {
                    playersIn6x2Format.add(playerInGameSearch);
                }
            }

            if(playersIn4x2Format.size() < 8) playersIn4x2Format.clear();
            if(playersIn6x2Format.size() < 12) playersIn6x2Format.clear();

            List<PlayerInGameSearch> playersList = new ArrayList<>();

            for(PlayerInGameSearch playerInGameSearch : players) {
                if(playerInGameSearch.format.equals("4x2")) {
                    if(!playersIn4x2Format.isEmpty()) {
                        for(int i = 0; i < 8; i++) {
                            playersList.add(playersIn4x2Format.get(i));
                        }
                        break;
                    }
                } else if(playerInGameSearch.format.equals("6x2")) {
                    if(!playersIn6x2Format.isEmpty()) {
                        for(int i = 0; i < 12; i++) {
                            playersList.add(playersIn6x2Format.get(i));
                        }
                        break;
                    }
                }
            }

            // Недостаточно игроков для начала игры
            if(playersList.size() < 8) return;

            int firstTeamCaptainId = -1;
            int secondTeamCaptainId = -1;

            Map<Integer, Integer> playersPoints = new HashMap<>();

            for(PlayerInGameSearch player : playersList) {
                PreparedStatement captainStatement = connection.prepareStatement(
                        "SELECT points FROM players WHERE player_id = ? AND is_deleted is null;");
                captainStatement.setInt(1, player.id);
                ResultSet captainResultSet = captainStatement.executeQuery();
                if(captainResultSet.next()) {
                    int points = captainResultSet.getInt("points");
                    playersPoints.put(player.id, points);
                }
            }

            List<Map.Entry<Integer, Integer>> list = new LinkedList<>(playersPoints.entrySet());
            list.sort(Map.Entry.comparingByValue());
            for(int i = list.size() -1; i >= 0; i--) {
                if(firstTeamCaptainId == -1) {
                    firstTeamCaptainId = list.get(i).getKey();
                } else {
                    secondTeamCaptainId = list.get(i).getKey();
                    break;
                }
            }

            PreparedStatement assistantsStatement = connection.prepareStatement("SELECT assistant_id FROM available_assistants;");
            ResultSet assistantsResultSet = assistantsStatement.executeQuery();
            if(assistantsResultSet.next()) {
                int assistantId = assistantsResultSet.getInt("assistant_id");
                startGame(playersList, firstTeamCaptainId, secondTeamCaptainId, playersList.get(0).format, assistantId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Начинает игру между игроками
     * @param players Список игроков
     * @param firstTeamCaptainId Id первой команды
     * @param secondTeamCaptainId Id второй команды
     * @param format Формат игры
     * @param assistantId Id помощника
     */
    private void startGame(List<PlayerInGameSearch> players, int firstTeamCaptainId, int secondTeamCaptainId, String format, int assistantId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("""
                INSERT INTO single_live_games (first_team_captain_id, second_team_captain_id,
                format, assistant_id, started_at, game_state) SELECT ?, ?, ?, ?, ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM team_live_games WHERE first_team_captain_id = ? OR second_team_captain_id = ?)""", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, firstTeamCaptainId);
            preparedStatement.setInt(2, secondTeamCaptainId);
            preparedStatement.setString(3, format);
            preparedStatement.setInt(4, assistantId);
            preparedStatement.setTimestamp(5, Timestamp.from(Instant.now()));
            preparedStatement.setInt(6, GameState.PLAYERS_CHOICE.getId());
            preparedStatement.setInt(7, firstTeamCaptainId);
            preparedStatement.setInt(8, secondTeamCaptainId);
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if(resultSet.next()) {
                int liveGameId = resultSet.getInt(1);

                List<Integer> playersIds = new ArrayList<>();

                for(PlayerInGameSearch player : players) {
                    if(player.id != firstTeamCaptainId && player.id != secondTeamCaptainId) {
                        PreparedStatement addPlayersStatement = connection.prepareStatement("""
                        INSERT INTO single_live_games_players (live_game_id, player_id) SELECT ?, ?
                        WHERE NOT EXISTS (SELECT 1 FROM single_live_games_players WHERE player_id = ?)""");
                        addPlayersStatement.setInt(1, liveGameId);
                        addPlayersStatement.setInt(2, player.id);
                        addPlayersStatement.setInt(3, player.id);
                        addPlayersStatement.executeUpdate();
                        playersIds.add(player.id);
                    }

                    PreparedStatement deletePlayersStatement = connection.prepareStatement("DELETE FROM players_in_game_search WHERE player_id = ?;");
                    deletePlayersStatement.setInt(1, player.id);
                    deletePlayersStatement.executeUpdate();
                }

                MTHD.getInstance().database.setUnready(assistantId);
                createGame(playersIds, liveGameId, firstTeamCaptainId, secondTeamCaptainId, format, assistantId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает игру
     */
    private void createGame(List<Integer> playersIds, int liveGameId, int firstTeamCaptainId, int secondTeamCaptainId, String format, int assistantId) {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Game-" + liveGameId, true);
        if(categories.isEmpty()) {
            Game game = new Game(Rating.SINGLE_RATING, playersIds, liveGameId, firstTeamCaptainId, secondTeamCaptainId, format, assistantId);
            gameManager.gameCategories.add(new GameCategoryManager(game));
            MTHD.getInstance().singleFindGameChannel.updatePlayersInGameSearchCountMessage();
            MTHD.getInstance().singleFindGameChannel.updateAvailableAssistantsMessage();
        }
    }

    /**
     * Заканчивает игру
     * @param game Игра
     * @param matchId Id матча
     * @param winnerTeamId Id победивщей команды
     * @param firstTeamRating Очки первой команды
     * @param secondTeamRating Очки второй команды
     */
    public void finishGame(Game game, String matchId, int winnerTeamId, int firstTeamRating, int secondTeamRating) {
        for(GameCategoryManager gameCategoryManager : gameManager.gameCategories) {
            if(gameCategoryManager.game.equals(game)) {
                if(gameCategoryManager.gameChannel.channelId != null && gameCategoryManager.gameChannel.channelMessageId != null) {
                    TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(gameCategoryManager.gameChannel.channelId);
                    if(textChannel != null) {
                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        if(winnerTeamId == 0) {
                            embedBuilder.setTitle("Побидетель: team_" + game.firstTeamCaptain.username);
                            embedBuilder.addField("Рейтинг", "Рейтинг team_" + game.firstTeamCaptain.username + ": +" + (firstTeamRating)
                                                             + "\n" + "Рейтинг team_" + game.secondTeamCaptain.username + ": " + (secondTeamRating), true);
                        } else if(winnerTeamId == 1) {
                            embedBuilder.setTitle("Побидетель: team_" + game.secondTeamCaptain.username);
                            embedBuilder.addField("Рейтинг", "Рейтинг team_" + game.secondTeamCaptain.username + ": +" + (secondTeamRating)
                                                             + "\n" + "Рейтинг team_" + game.firstTeamCaptain.username + ": " + (firstTeamRating), true);
                        }

                        embedBuilder.setColor(3092790);
                        embedBuilder.addField("Помощник", game.assistantAccount.username, false);

                        textChannel.editMessageEmbedsById(gameCategoryManager.gameChannel.channelMessageId, embedBuilder.build()).queue();
                        embedBuilder.clear();

                        gameManager.deleteGame(game);
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Category category = MTHD.getInstance().guild.getCategoryById(gameCategoryManager.categoryId);
                                if(category != null) {
                                    gameManager.deleteGame(category.getId());
                                }
                            }
                        }, 30000);
                    }
                }
            }
        }

        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement finishStatement = connection.prepareStatement("""
                INSERT INTO single_finished_games_history (first_team_captain_id,
                second_team_captain_id, format, map_name, match_id, winner_team_id, assistant_id, finished_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);""", Statement.RETURN_GENERATED_KEYS);
            finishStatement.setInt(1, game.firstTeamCaptain.id);
            finishStatement.setInt(2, game.secondTeamCaptain.id);
            finishStatement.setString(3, game.format);
            finishStatement.setString(4, game.gameMap.getName());
            finishStatement.setString(5, matchId);
            finishStatement.setInt(6, winnerTeamId);
            finishStatement.setInt(7, game.assistantAccount.id);
            finishStatement.setTimestamp(8, Timestamp.from(Instant.now()));
            finishStatement.executeUpdate();
            ResultSet createResultSet = finishStatement.getGeneratedKeys();
            if(createResultSet.next()) {
                int finishedGameId = createResultSet.getInt(1);

                for(int playerId : game.firstTeamPlayersId) {
                    PreparedStatement playersStatement = connection.prepareStatement(
                            "INSERT INTO single_finished_games_players_history (finished_game_id, team_id, player_id, rating_changes) VALUES (?, ?, ?, ?);");
                    playersStatement.setInt(1, finishedGameId);
                    playersStatement.setInt(2, 0);
                    playersStatement.setInt(3, playerId);
                    playersStatement.setInt(4, firstTeamRating);
                    playersStatement.executeUpdate();
                }

                for(int playerId : game.secondTeamPlayersId) {
                    PreparedStatement playersStatement = connection.prepareStatement(
                            "INSERT INTO single_finished_games_players_history (finished_game_id, team_id, player_id, rating_changes) VALUES (?, ?, ?, ?);");
                    playersStatement.setInt(1, finishedGameId);
                    playersStatement.setInt(2, 1);
                    playersStatement.setInt(3, playerId);
                    playersStatement.setInt(4, secondTeamRating);
                    playersStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Заканчивает игру команды
     * @param points Очки команды
     * @param games Количеств игр команды
     * @param wins Количество побед команды
     * @param id Id команды
     */
    public void finishGamePlayer(int points, int games, int wins, int id) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement playersStatement = connection.prepareStatement(
                    "UPDATE players SET points = points + ?, games = games + ?, wins = wins + ? WHERE id = ? AND is_deleted is null;");
            playersStatement.setInt(1, points);
            playersStatement.setInt(2, games);
            playersStatement.setInt(3, wins);
            playersStatement.setInt(4, id);
            playersStatement.executeUpdate();
            MTHD.getInstance().playersChannel.updateTopMessage();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Представляет собой игрока в поиске игры
     */
    private static class PlayerInGameSearch {

        /** Id игрока */
        public int id;

        /** Формат игры */
        public String format;

        /**
         * Инициализирует игрока в поиске игры
         * @param id Id команды
         * @param format Формат игры
         */
        public PlayerInGameSearch(int id, String format) {
            this.id = id;
            this.format = format;
        }
    }
}