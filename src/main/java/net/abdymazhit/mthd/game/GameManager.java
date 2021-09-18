package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.customs.TeamInGameSearch;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Менеджер игры
 *
 * @version   18.09.2021
 * @author    Islam Abdymazhit
 */
public class GameManager {

    /** Список категорий игр */
    private final List<GameCategory> gameCategories;

    /**
     * Инициализирует менеджер игры
     */
    public GameManager() {
        gameCategories = new ArrayList<>();

        for(Category category : MTHD.getInstance().guild.getCategories()) {
            if(category.getName().contains("Game-")) {
                int id = Integer.parseInt(category.getName().replace("Game-", ""));

                try {
                    Connection connection = MTHD.getInstance().database.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "SELECT * FROM live_games WHERE id = ?;");
                    preparedStatement.setInt(1, id);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    

                    if(resultSet.next()) {
                        int firstTeamId = resultSet.getInt("first_team_id");
                        int firstTeamStarterId = resultSet.getInt("first_team_starter_id");
                        int secondTeamId = resultSet.getInt("second_team_id");
                        int secondTeamStarterId = resultSet.getInt("second_team_starter_id");
                        String format = resultSet.getString("format");
                        String mapName = resultSet.getString("map_name");
                        int assistantId = resultSet.getInt("assistant_id");
                        Timestamp startedAt = resultSet.getTimestamp("started_at");
                        int gameStateId = resultSet.getInt("game_state");

                        String firstTeamName = MTHD.getInstance().database.getTeamName(firstTeamId);
                        if(firstTeamName == null) {
                            return;
                        }

                        String secondTeamName = MTHD.getInstance().database.getTeamName(secondTeamId);
                        if(secondTeamName == null) {
                            return;
                        }

                        GameMap selectedMap = null;
                        for(GameMap gameMap : GameMap.values()) {
                            if(gameMap.getName().equals(mapName)) {
                                selectedMap = gameMap;
                            }
                        }

                        GameState state = null;
                        for(GameState gameState : GameState.values()) {
                            if(gameState.getId() == gameStateId) {
                                state = gameState;
                            }
                        }

                        PreparedStatement firstStatement = connection.prepareStatement(
                                "SELECT player_id FROM live_games_players WHERE team_id = ?;");
                        firstStatement.setInt(1, firstTeamId);
                        ResultSet firstResultSet = firstStatement.executeQuery();
                        

                        List<Integer> firstTeamPlayersId = new ArrayList<>();
                        while(firstResultSet.next()) {
                            firstTeamPlayersId.add(firstResultSet.getInt("player_id"));
                        }

                        PreparedStatement secondStatement = connection.prepareStatement(
                                "SELECT player_id FROM live_games_players WHERE team_id = ?;");
                        secondStatement.setInt(1, secondTeamId);
                        ResultSet secondResultSet = secondStatement.executeQuery();

                        List<Integer> secondTeamPlayersId = new ArrayList<>();
                        while(secondResultSet.next()) {
                            secondTeamPlayersId.add(secondResultSet.getInt("player_id"));
                        }

                        Game game = new Game(id, firstTeamId, firstTeamStarterId, secondTeamId,
                                secondTeamStarterId, format, selectedMap, state, assistantId, startedAt);
                        game.firstTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(firstTeamPlayersId);
                        game.secondTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(secondTeamPlayersId);

                        gameCategories.add(new GameCategory(game, category));
                    } else {
                        deleteGame(category.getId());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Попытается начать игру
     */
    public void tryStartGame() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT team_id, format, starter_id FROM teams_in_game_search;");
            ResultSet resultSet = preparedStatement.executeQuery();
            

            List<TeamInGameSearch> teams = new ArrayList<>();
            while(resultSet.next()) {
                teams.add(new TeamInGameSearch(resultSet.getInt("team_id"),
                        resultSet.getString("format"), resultSet.getInt("starter_id")));
            }

            if(teams.size() < 2) {
                // Недостаточно команд для начала игры
                return;
            }

            List<TeamInGameSearch> teamsIn4x2Format = new ArrayList<>();
            List<TeamInGameSearch> teamsIn6x2Format = new ArrayList<>();

            for(TeamInGameSearch teamInGameSearch : teams) {
                if(teamInGameSearch.format.equals("4x2")) {
                    teamsIn4x2Format.add(teamInGameSearch);
                } else if(teamInGameSearch.format.equals("6x2")) {
                    teamsIn6x2Format.add(teamInGameSearch);
                }
            }

            if(teamsIn4x2Format.size() < 2) {
                teamsIn4x2Format.clear();
            }

            if(teamsIn6x2Format.size() < 2) {
                teamsIn6x2Format.clear();
            }

            TeamInGameSearch firstTeam = null;
            TeamInGameSearch secondTeam = null;

            for(TeamInGameSearch teamInGameSearch : teams) {
                if(teamInGameSearch.format.equals("4x2")) {
                    if(!teamsIn4x2Format.isEmpty()) {
                        firstTeam = teamsIn4x2Format.get(0);
                        secondTeam = teamsIn4x2Format.get(1);
                        break;
                    }
                } else if(teamInGameSearch.format.equals("6x2")) {
                    if(!teamsIn6x2Format.isEmpty()) {
                        firstTeam = teamsIn6x2Format.get(0);
                        secondTeam = teamsIn6x2Format.get(1);
                        break;
                    }
                }
            }

            if(firstTeam == null || secondTeam == null) {
                // Недостаточно команд для начала игры
                return;
            }

            PreparedStatement assistantsStatement = connection.prepareStatement(
                    "SELECT assistant_id FROM available_assistants;");
            ResultSet assistantsResultSet = assistantsStatement.executeQuery();


            if(assistantsResultSet.next()) {
                UserAccount assistant = new UserAccount(assistantsResultSet.getInt("assistant_id"));
                startGame(firstTeam.id, firstTeam.starterId, secondTeam.id, secondTeam.starterId,
                        firstTeam.format, assistant.getId());
            }
        } catch (SQLException e) {
            // Критическая ошибка
            e.printStackTrace();
        }
    }

    /**
     * Начинает игру между двумя командами
     * @param firstTeamId Id первой команды
     * @param secondTeamId Id второй команды
     * @param format Формат игры
     * @param assistantId Id помощника
     */
    private void startGame(int firstTeamId, int first_team_starter_id, int secondTeamId, int second_team_starter_id,
                           String format, int assistantId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO live_games (first_team_id, first_team_starter_id, second_team_id, second_team_starter_id, " +
                            "format, assistant_id, started_at, game_state) SELECT ?, ?, ?, ?, ?, ?, ?, ? " +
                            "WHERE NOT EXISTS (SELECT 1 FROM live_games WHERE first_team_id = ? AND second_team_id = ?)", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, firstTeamId);
            preparedStatement.setInt(2, first_team_starter_id);
            preparedStatement.setInt(3, secondTeamId);
            preparedStatement.setInt(4, second_team_starter_id);
            preparedStatement.setString(5, format);
            preparedStatement.setInt(6, assistantId);
            preparedStatement.setTimestamp(7, Timestamp.from(Instant.now()));
            preparedStatement.setInt(8, GameState.PLAYERS_CHOICE.getId());
            preparedStatement.setInt(9, firstTeamId);
            preparedStatement.setInt(10, secondTeamId);
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();

            if(resultSet.next()) {
                int gameId = resultSet.getInt(1);

                PreparedStatement deleteFirstTeamStatement = connection.prepareStatement(
                        "DELETE FROM teams_in_game_search WHERE team_id = ?;");
                deleteFirstTeamStatement.setInt(1, firstTeamId);
                deleteFirstTeamStatement.executeUpdate();

                PreparedStatement deleteSecondTeamStatement = connection.prepareStatement(
                        "DELETE FROM teams_in_game_search WHERE team_id = ?;", Statement.RETURN_GENERATED_KEYS);
                deleteSecondTeamStatement.setInt(1, secondTeamId);
                deleteSecondTeamStatement.executeUpdate();

                MTHD.getInstance().liveGamesChannel.updateLiveGamesMessages();
                MTHD.getInstance().database.setUnready(assistantId);
                createGame(gameId, firstTeamId, first_team_starter_id, secondTeamId, second_team_starter_id, format, assistantId);
            }
        } catch (SQLException e) {
            // Критическая ошибка
            e.printStackTrace();
        }
    }

    /**
     * Создает игру
     */
    private void createGame(int gameId, int firstTeamId, int firstTeamStarterId, int secondTeamId, int secondTeamStarterId,
                            String format, int assistantId) {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Game-" + gameId, true);

        if(categories.isEmpty()) {
            Game game = new Game(gameId, firstTeamId, firstTeamStarterId, secondTeamId, secondTeamStarterId, format, assistantId);
            gameCategories.add(new GameCategory(game));
            MTHD.getInstance().findGameChannel.updateTeamsInGameSearchCountMessage();
            MTHD.getInstance().findGameChannel.updateAvailableAssistantsMessage();
        }
    }

    /**
     * Удаляет игру
     * @param game Игра
     */
    public void deleteGame(Game game) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement gameStatement = connection.prepareStatement(
                    "DELETE FROM live_games WHERE id = ?;");
            gameStatement.setInt(1, game.id);
            gameStatement.executeUpdate();

            PreparedStatement firstTeamStatement = connection.prepareStatement(
                    "DELETE FROM live_games_players WHERE team_id = ?;");
            firstTeamStatement.setInt(1, game.firstTeamId);
            firstTeamStatement.executeUpdate();

            PreparedStatement secondTeamStatement = connection.prepareStatement(
                    "DELETE FROM live_games_players WHERE team_id = ?;");
            secondTeamStatement.setInt(1, game.secondTeamId);
            secondTeamStatement.executeUpdate();

            MTHD.getInstance().liveGamesChannel.updateLiveGamesMessages();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаляет игру
     * @param categoryId Id категория игры
     */
    public void deleteGame(String categoryId) {
        Category category = MTHD.getInstance().guild.getCategoryById(categoryId);
        if(category != null) {
            for(GuildChannel channel : category.getChannels()) {
                channel.delete().queue();
            }
            category.delete().queue();

            for(GameCategory gameCategory : gameCategories) {
                if(gameCategory.categoryId.equals(category.getId())) {
                    gameCategories.remove(gameCategory);
                    break;
                }
            }
        }
    }

    public void finishGame(Game game, String matchId, int winnerTeamId, int firstTeamRating, int secondTeamRating) {
        for(GameCategory gameCategory : gameCategories) {
            if(gameCategory.game.equals(game)) {
                if(gameCategory.gameChannel.channelId != null && gameCategory.gameChannel.channelMessageId != null) {
                    TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(gameCategory.gameChannel.channelId);
                    if(textChannel != null) {
                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        if(game.firstTeamId == winnerTeamId) {
                            embedBuilder.setTitle("Побидетель: " + game.firstTeamName);
                            embedBuilder.addField("Рейтинг", "Рейтинг победителя: +" + firstTeamRating + " (+" + (firstTeamRating - game.firstTeamPoints)
                                    +")\n" + "Рейтинг проигравшего: " + secondTeamRating + " (" + (secondTeamRating - game.secondTeamPoints) + ")", true);
                        } else if(game.secondTeamId == winnerTeamId) {
                            embedBuilder.setTitle("Побидетель: " + game.secondTeamName);
                            embedBuilder.addField("Рейтинг", "Рейтинг победителя: +" + secondTeamRating + " (+" + (secondTeamRating - game.secondTeamPoints)
                                    +")\n" + "Рейтинг проигравшего: " + firstTeamRating + "(" + (firstTeamRating - game.firstTeamPoints) + ")", true);
                        }

                        embedBuilder.setColor(3092790);
                        embedBuilder.addField("Помощник", game.assistantName, false);

                        textChannel.editMessageEmbedsById(gameCategory.gameChannel.channelMessageId, embedBuilder.build()).queue();
                        embedBuilder.clear();

                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Category category = MTHD.getInstance().guild.getCategoryById(gameCategory.categoryId);
                                if(category != null) {
                                    MTHD.getInstance().gameManager.deleteGame(game);
                                    MTHD.getInstance().gameManager.deleteGame(category.getId());
                                }
                            }
                        }, 30000);
                    }
                }
            }
        }

        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement finishStatement = connection.prepareStatement(
                    "INSERT INTO finished_games_history (first_team_id, first_team_starter_id, second_team_id, " +
                            "second_team_starter_id, format, map_name, match_id, winner_team_id, first_team_rating_changes, " +
                            "second_team_rating_changes, assistant_id, finished_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                    Statement.RETURN_GENERATED_KEYS);
            finishStatement.setInt(1, game.firstTeamId);
            finishStatement.setInt(2, game.firstTeamStarterId);
            finishStatement.setInt(3, game.secondTeamId);
            finishStatement.setInt(4, game.secondTeamStarterId);
            finishStatement.setString(5, game.format);
            finishStatement.setString(6, game.gameMap.getName());
            finishStatement.setString(7, matchId);
            finishStatement.setInt(8, winnerTeamId);
            finishStatement.setInt(9, firstTeamRating - game.firstTeamPoints);
            finishStatement.setInt(10, secondTeamRating - game.secondTeamPoints);
            finishStatement.setInt(11, game.assistantId);
            finishStatement.setTimestamp(12, Timestamp.from(Instant.now()));
            finishStatement.executeUpdate();
            ResultSet createResultSet = finishStatement.getGeneratedKeys();

            if(createResultSet.next()) {
                int finishedGameId = createResultSet.getInt(1);

                System.out.println(game.firstTeamPlayersId);

                for(int playerId : game.firstTeamPlayersId) {
                    PreparedStatement playersStatement = connection.prepareStatement(
                            "INSERT INTO finished_games_players_history (finished_game_id, team_id, player_id) VALUES (?, ?, ?);");
                    playersStatement.setInt(1, finishedGameId);
                    playersStatement.setInt(2, game.firstTeamId);
                    playersStatement.setInt(3, playerId);
                    playersStatement.executeUpdate();
                }

                System.out.println(game.secondTeamPlayersId);

                for(int playerId : game.secondTeamPlayersId) {
                    PreparedStatement playersStatement = connection.prepareStatement(
                            "INSERT INTO finished_games_players_history (finished_game_id, team_id, player_id) VALUES (?, ?, ?);");
                    playersStatement.setInt(1, finishedGameId);
                    playersStatement.setInt(2, game.firstTeamId);
                    playersStatement.setInt(3, playerId);
                    playersStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void finishGameTeam(int points, int games, int wins, int wonBeds, int lostBeds, int id) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement playersStatement = connection.prepareStatement(
                    "UPDATE teams SET points = ?, games = games + ?, wins = wins + ?, won_beds = won_beds + ?, " +
                            "lost_beds = lost_beds + ? WHERE id = ?;");
            playersStatement.setInt(1, points);
            playersStatement.setInt(2, games);
            playersStatement.setInt(3, wins);
            playersStatement.setInt(4, wonBeds);
            playersStatement.setInt(5, lostBeds);
            playersStatement.setInt(6, id);
            playersStatement.executeUpdate();

            MTHD.getInstance().teamsChannel.updateTopMessage();
            MTHD.getInstance().liveGamesChannel.updateLiveGamesMessages();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает категории игр
     * @return Категории игр
     */
    public List<GameCategory> getGameCategories() {
        return gameCategories;
    }
}
