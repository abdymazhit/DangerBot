package net.abdymazhit.dangerbot.managers;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Game;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.GameMap;
import net.abdymazhit.dangerbot.enums.GameState;
import net.abdymazhit.dangerbot.enums.Rating;
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
 * @version   23.10.2021
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
            Connection connection = DangerBot.getInstance().database.getConnection();
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

                String firstTeamName = DangerBot.getInstance().database.getTeamName(firstTeamId);
                String secondTeamName = DangerBot.getInstance().database.getTeamName(secondTeamId);

                if(firstTeamName == null || secondTeamName == null) return;

                GameMap selectedMap = null;
                for(GameMap gameMap : GameMap.values()) {
                    if(gameMap.getName().equals(mapName)) selectedMap = gameMap;
                }

                GameState state = null;
                for(GameState gameState : GameState.values()) {
                    if(gameState.getId() == gameStateId) state = gameState;
                }

                Game game = new Game(Rating.TEAM_RATING, id, format, selectedMap, state, startedAt, firstTeamId, firstTeamCaptainId, secondTeamId,
                        secondTeamCaptainId, assistantId);
                for(String playerName : DangerBot.getInstance().database.getTeamPlayersNames(firstTeamId)) {
                    game.firstTeamInfo.members.add(new UserAccount(playerName));
                }
                for(String playerName : DangerBot.getInstance().database.getTeamPlayersNames(secondTeamId)) {
                    game.secondTeamInfo.members.add(new UserAccount(playerName));
                }

                liveGames.add(game);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
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
                    if(gameMap.getName().equals(mapName)) selectedMap = gameMap;
                }

                GameState state = null;
                for(GameState gameState : GameState.values()) {
                    if(gameState.getId() == gameStateId) state = gameState;
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

                Game game = new Game(Rating.SINGLE_RATING, id, format, selectedMap, state, startedAt, firstTeamCaptainId, secondTeamCaptainId,
                        assistantId, playersIds);
                game.firstTeamInfo.members = new ArrayList<>();
                for(String playerName : DangerBot.getInstance().database.getSinglePlayersNames(game.id, 0)) {
                    game.firstTeamInfo.members.add(new UserAccount(playerName));
                }

                game.secondTeamInfo.members = new ArrayList<>();
                for(String playerName : DangerBot.getInstance().database.getSinglePlayersNames(game.id, 1)) {
                    game.secondTeamInfo.members.add(new UserAccount(playerName));
                }

                liveGames.add(game);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Integer> gameCategoriesIds = new ArrayList<>();
        for(Category category : DangerBot.getInstance().guild.getCategories()) {
            if(category.getName().contains("Game-")) {
                int id = Integer.parseInt(category.getName().replace("Game-", ""));
                gameCategoriesIds.add(id);
            }
        }

        for(Game game : liveGames) {
            if(gameCategoriesIds.contains(game.id)) {
                List<Category> categories = DangerBot.getInstance().guild.getCategoriesByName("Game-" + game.id, true);
                if(categories.size() == 1) {
                    gameCategories.add(new GameCategoryManager(game, categories.get(0)));
                    if(game.gameState.equals(GameState.GAME)) {
                        DangerBot.getInstance().liveGamesManager.addLiveGame(game);
                    }
                    gameCategoriesIds.remove(Integer.valueOf(game.id));
                }
            } else {
                DangerBot.getInstance().guild.createCategory("Game-" + game.id).queue(category -> {
                    gameCategories.add(new GameCategoryManager(game, category));
                    if(game.gameState.equals(GameState.GAME)) {
                        DangerBot.getInstance().liveGamesManager.addLiveGame(game);
                    }
                    gameCategoriesIds.remove(Integer.valueOf(game.id));
                });
            }

        }

        for(int id : gameCategoriesIds) {
            List<Category> categories = DangerBot.getInstance().guild.getCategoriesByName("Game-" + id, true);
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
        Category category = DangerBot.getInstance().guild.getCategoryById(categoryId);
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
                if(gameCategoryManager.category.equals(category)) {
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
            Connection connection = DangerBot.getInstance().database.getConnection();
            if(game.rating.equals(Rating.TEAM_RATING)) {
                PreparedStatement gameStatement = connection.prepareStatement("DELETE FROM team_live_games WHERE id = ?;");
                gameStatement.setInt(1, game.id);
                gameStatement.executeUpdate();

                PreparedStatement firstTeamStatement = connection.prepareStatement("DELETE FROM team_live_games_players WHERE team_id = ?;");
                firstTeamStatement.setInt(1, game.firstTeamInfo.id);
                firstTeamStatement.executeUpdate();

                PreparedStatement secondTeamStatement = connection.prepareStatement("DELETE FROM team_live_games_players WHERE team_id = ?;");
                secondTeamStatement.setInt(1, game.secondTeamInfo.id);
                secondTeamStatement.executeUpdate();

                DangerBot.getInstance().teamLiveGamesChannel.updateLiveGamesMessages();
            } else {
                PreparedStatement gameStatement = connection.prepareStatement("DELETE FROM single_live_games WHERE id = ?;");
                gameStatement.setInt(1, game.id);
                gameStatement.executeUpdate();

                PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM single_live_games_players WHERE live_game_id = ?;");
                deleteStatement.setInt(1, game.id);
                deleteStatement.executeUpdate();

                DangerBot.getInstance().singleLiveGamesChannel.updateLiveGamesMessages();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
