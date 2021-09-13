package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.customs.TeamInGameSearch;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameState;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер игры
 *
 * @version   13.09.2021
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
            preparedStatement.close();

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
            assistantsStatement.close();

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
                            "WHERE NOT EXISTS (SELECT 1 FROM live_games WHERE first_team_id = ? AND second_team_id = ?) " +
                            "RETURNING id;");
            preparedStatement.setInt(1, firstTeamId);
            preparedStatement.setInt(2, first_team_starter_id);
            preparedStatement.setInt(3, secondTeamId);
            preparedStatement.setInt(4, second_team_starter_id);
            preparedStatement.setString(5, format);
            preparedStatement.setInt(6, assistantId);
            preparedStatement.setTimestamp(7, Timestamp.from(Instant.now()));
            preparedStatement.setInt(8, GameState.PLAYERS_SELECTION.getId());
            preparedStatement.setInt(9, firstTeamId);
            preparedStatement.setInt(10, secondTeamId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                int gameId = resultSet.getInt("id");

                PreparedStatement deleteFirstTeamStatement = connection.prepareStatement(
                        "DELETE FROM teams_in_game_search WHERE team_id = ? RETURNING id;");
                deleteFirstTeamStatement.setInt(1, firstTeamId);
                ResultSet deleteFirstTeamResultSet = deleteFirstTeamStatement.executeQuery();
                deleteFirstTeamStatement.close();

                if(!deleteFirstTeamResultSet.next()) {
                    // Критическая ошибка при удалении первой команды из поиска игры! Свяжитесь с разработчиком бота!
                    return;
                }

                PreparedStatement deleteSecondTeamStatement = connection.prepareStatement(
                        "DELETE FROM teams_in_game_search WHERE team_id = ? RETURNING  id;");
                deleteSecondTeamStatement.setInt(1, secondTeamId);
                ResultSet deleteSecondTeamResultSet = deleteSecondTeamStatement.executeQuery();
                deleteSecondTeamStatement.close();

                if(!deleteSecondTeamResultSet.next()) {
                    // Критическая ошибка при удалении второй команды из поиска игры! Свяжитесь с разработчиком бота!
                    return;
                }

                MTHD.getInstance().liveGamesChannel.updateLiveGamesMessages();
                MTHD.getInstance().database.setUnready(assistantId);
                createGame(gameId, firstTeamId, first_team_starter_id, secondTeamId, second_team_starter_id);
            }
        } catch (SQLException e) {
            // Критическая ошибка
            e.printStackTrace();
        }
    }

    /**
     * Создает игру
     */
    private void createGame(int gameId, int firstTeamId, int firstTeamStarterId, int secondTeamId, int secondTeamStarterId) {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Game-" + gameId, true);

        if(categories.isEmpty()) {
            String firstTeamName = MTHD.getInstance().database.getTeamName(firstTeamId);
            if(firstTeamName == null) {
                return;
            }

            String secondTeamName = MTHD.getInstance().database.getTeamName(secondTeamId);
            if(secondTeamName == null) {
                return;
            }

            Game game = new Game(gameId, firstTeamId, firstTeamName, firstTeamStarterId, secondTeamId, secondTeamName, secondTeamStarterId);
            gameCategories.add(new GameCategory(game));
        }
    }

    /**
     * Удаляет игру
     * @param gameId Id игры
     */
    public void deleteGame(int gameId) {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Game-" + gameId, true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            for(TextChannel textChannel : category.getTextChannels()) {
                textChannel.delete().queue();
            }
            category.delete().queue();
        }

        for(GameCategory gameCategory : gameCategories) {
            if(gameCategory.category.getName().equals("Game-" + gameId)) {
                gameCategories.remove(gameCategory);
                break;
            }
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
