package net.abdymazhit.mthd.managers;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.Rating;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static net.dv8tion.jda.api.exceptions.ErrorResponseException.ignore;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_CHANNEL;

/**
 * Менеджер игры
 *
 * @version   08.10.2021
 * @author    Islam Abdymazhit
 */
public class GameManager {

    /** Список категорий игр */
    public final List<GameCategoryManager> gameCategories;

    /** Менеджер Team Rating игр */
    public final TeamGameManager teamGameManager;

    /** Менеджер Single Rating игр */
    public final SingleGameManager singleGameManager;

    /**
     * Инициализирует менеджер игры
     */
    public GameManager() {
        gameCategories = new ArrayList<>();
        teamGameManager = new TeamGameManager(this);
        singleGameManager = new SingleGameManager(this);

        List<Game> liveGames = new ArrayList<>();

        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM team_live_games;");
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                int firstTeamId = resultSet.getInt("first_team_id");
                int firstTeamCaptainId = resultSet.getInt("first_team_captain_id");
                int secondTeamId = resultSet.getInt("second_team_id");
                int secondTeamCaptainId = resultSet.getInt("second_team_captain_id");
                String format = resultSet.getString("format");
                String mapName = resultSet.getString("map_name");
                int assistantId = resultSet.getInt("assistant_id");
                Timestamp startedAt = resultSet.getTimestamp("started_at");
                int gameStateId = resultSet.getInt("game_state");

                String firstTeamName = MTHD.getInstance().database.getTeamName(firstTeamId);
                String secondTeamName = MTHD.getInstance().database.getTeamName(secondTeamId);

                if(firstTeamName == null || secondTeamName == null) {
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

                Game game = new Game(Rating.TEAM_RATING, id, firstTeamId, firstTeamCaptainId, secondTeamId,
                        secondTeamCaptainId, format, selectedMap, state, assistantId, startedAt);
                game.firstTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(firstTeamId);
                game.secondTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(secondTeamId);
                liveGames.add(game);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM single_live_games;");
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                int firstTeamCaptainId = resultSet.getInt("first_team_captain_id");
                int secondTeamCaptainId = resultSet.getInt("second_team_captain_id");
                String format = resultSet.getString("format");
                String mapName = resultSet.getString("map_name");
                int assistantId = resultSet.getInt("assistant_id");
                Timestamp startedAt = resultSet.getTimestamp("started_at");
                int gameStateId = resultSet.getInt("game_state");

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

                PreparedStatement playersPreparedStatement = connection.prepareStatement(
                        "SELECT player_id FROM single_live_games_players WHERE live_game_id = ?;");
                playersPreparedStatement.setInt(1, id);
                ResultSet playersResultSet = playersPreparedStatement.executeQuery();
                List<Integer> playersIds = new ArrayList<>();
                while(playersResultSet.next()) {
                    int playerId = playersResultSet.getInt("player_id");
                    playersIds.add(playerId);
                }

                Game game = new Game(Rating.SINGLE_RATING, playersIds, id, firstTeamCaptainId,
                        secondTeamCaptainId, format, selectedMap, state, assistantId, startedAt);
                game.firstTeamPlayers = MTHD.getInstance().database.getSinglePlayersNames(game.id, 0);
                game.secondTeamPlayers = MTHD.getInstance().database.getSinglePlayersNames(game.id, 1);
                liveGames.add(game);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Integer> gameCategoriesIds = new ArrayList<>();
        for(Category category : MTHD.getInstance().guild.getCategories()) {
            if(category.getName().contains("Game-")) {
                int id = Integer.parseInt(category.getName().replace("Game-", ""));
                gameCategoriesIds.add(id);
            }
        }

        for(Game game : liveGames) {
            if(gameCategoriesIds.contains(game.id)) {
                List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Game-" + game.id, true);
                if(categories.size() == 1) {
                    gameCategories.add(new GameCategoryManager(game, categories.get(0)));
                    MTHD.getInstance().liveGamesManager.addLiveGame(game);
                    gameCategoriesIds.remove(Integer.valueOf(game.id));
                }
            } else {
                MTHD.getInstance().guild.createCategory("Game-" + game.id).queue(
                        category -> {
                            gameCategories.add(new GameCategoryManager(game, category));
                            MTHD.getInstance().liveGamesManager.addLiveGame(game);
                            gameCategoriesIds.remove(Integer.valueOf(game.id));
                        });
            }

        }

        for(int id : gameCategoriesIds) {
            List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Game-" + id, true);
            for(Category category : categories) {
                for(GuildChannel guildChannel : category.getChannels()) {
                    guildChannel.delete().queue();
                }

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        category.delete().queue();
                    }
                }, 1000);
            }
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
                channel.delete().queue(null, ignore(UNKNOWN_CHANNEL));
            }

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    category.delete().queue();
                }
            }, 1000);

            for(GameCategoryManager gameCategoryManager : gameCategories) {
                if(gameCategoryManager.categoryId.equals(category.getId())) {
                    gameCategories.remove(gameCategoryManager);
                    break;
                }
            }
        }
    }

    /**
     * Удаляет игру
     * @param game Игра
     */
    public void deleteGame(Game game) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            if(game.rating.equals(Rating.TEAM_RATING)) {
                PreparedStatement gameStatement = connection.prepareStatement("DELETE FROM team_live_games WHERE id = ?;");
                gameStatement.setInt(1, game.id);
                gameStatement.executeUpdate();

                PreparedStatement firstTeamStatement = connection.prepareStatement("DELETE FROM team_live_games_players WHERE team_id = ?;");
                firstTeamStatement.setInt(1, game.firstTeam.id);
                firstTeamStatement.executeUpdate();

                PreparedStatement secondTeamStatement = connection.prepareStatement("DELETE FROM team_live_games_players WHERE team_id = ?;");
                secondTeamStatement.setInt(1, game.secondTeam.id);
                secondTeamStatement.executeUpdate();

                MTHD.getInstance().teamLiveGamesChannel.updateLiveGamesMessages();
            } else {
                PreparedStatement gameStatement = connection.prepareStatement("DELETE FROM single_live_games WHERE id = ?;");
                gameStatement.setInt(1, game.id);
                gameStatement.executeUpdate();

                PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM single_live_games_players WHERE live_game_id = ?;");
                deleteStatement.setInt(1, game.id);
                deleteStatement.executeUpdate();

                MTHD.getInstance().singleLiveGamesChannel.updateLiveGamesMessages();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
