package net.abdymazhit.dangerbot.managers;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Game;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.GameState;
import net.abdymazhit.dangerbot.enums.Rating;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Менеджер Team Rating игр
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public record TeamGameManager(GameManager gameManager) {

    /**
     * Попытается начать игру
     */
    public void tryStartGame() {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT team_id, format, captain_id FROM teams_in_game_search;");
            ResultSet resultSet = preparedStatement.executeQuery();
            List<TeamInGameSearch> teams = new ArrayList<>();
            while(resultSet.next()) {
                int teamId = resultSet.getInt("team_id");
                String format = resultSet.getString("format");
                int captainId = resultSet.getInt("captain_id");
                teams.add(new TeamInGameSearch(teamId, format, captainId));
            }

            // Недостаточно команд для начала игры
            if(teams.size() < 2) return;

            List<TeamInGameSearch> teamsIn4x2Format = new ArrayList<>();
            List<TeamInGameSearch> teamsIn6x2Format = new ArrayList<>();

            for(TeamInGameSearch teamInGameSearch : teams) {
                if(teamInGameSearch.format.equals("4x2")) {
                    teamsIn4x2Format.add(teamInGameSearch);
                } else if(teamInGameSearch.format.equals("6x2")) {
                    teamsIn6x2Format.add(teamInGameSearch);
                }
            }

            if(teamsIn4x2Format.size() < 2) teamsIn4x2Format.clear();
            if(teamsIn6x2Format.size() < 2) teamsIn6x2Format.clear();

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

            // Недостаточно команд для начала игры
            if(firstTeam == null || secondTeam == null) return;

            PreparedStatement assistantsStatement = connection.prepareStatement("SELECT assistant_id FROM available_assistants;");
            ResultSet assistantsResultSet = assistantsStatement.executeQuery();
            if(assistantsResultSet.next()) {
                int assistantId = assistantsResultSet.getInt("assistant_id");
                startGame(firstTeam.id, firstTeam.captainId, secondTeam.id, secondTeam.captainId, firstTeam.format, assistantId);
            }
        } catch (SQLException e) {
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
    private void startGame(int firstTeamId, int first_team_captain_id, int secondTeamId, int second_team_captain_id, String format, int assistantId) {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("""
                INSERT INTO team_live_games (first_team_id, first_team_captain_id, second_team_id, second_team_captain_id,
                format, assistant_id, started_at, game_state) SELECT ?, ?, ?, ?, ?, ?, ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM team_live_games WHERE first_team_id = ? OR second_team_id = ?)""", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, firstTeamId);
            preparedStatement.setInt(2, first_team_captain_id);
            preparedStatement.setInt(3, secondTeamId);
            preparedStatement.setInt(4, second_team_captain_id);
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

                PreparedStatement deleteFirstTeamStatement = connection.prepareStatement("DELETE FROM teams_in_game_search WHERE team_id = ?;");
                deleteFirstTeamStatement.setInt(1, firstTeamId);
                deleteFirstTeamStatement.executeUpdate();

                PreparedStatement deleteSecondTeamStatement = connection.prepareStatement("DELETE FROM teams_in_game_search WHERE team_id = ?;");
                deleteSecondTeamStatement.setInt(1, secondTeamId);
                deleteSecondTeamStatement.executeUpdate();

                DangerBot.getInstance().database.setUnready(assistantId);
                createGame(gameId, firstTeamId, first_team_captain_id, secondTeamId, second_team_captain_id, format, assistantId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает игру
     */
    private void createGame(int gameId, int firstTeamId, int firstTeamCaptainId, int secondTeamId, int secondTeamCaptainId, String format, int assistantId) {
        List<Category> categories = DangerBot.getInstance().guild.getCategoriesByName("Game-" + gameId, true);
        if(categories.isEmpty()) {
            Game game = new Game(Rating.TEAM_RATING, gameId, format, firstTeamId, firstTeamCaptainId, secondTeamId, secondTeamCaptainId, assistantId);
            gameManager.gameCategories.add(new GameCategoryManager(game));
            DangerBot.getInstance().teamFindGameChannel.updateTeamsInGameSearchCountMessage();
            DangerBot.getInstance().teamFindGameChannel.updateAvailableAssistantsMessage();
            DangerBot.getInstance().singleFindGameChannel.updateAvailableAssistantsMessage();
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
                EmbedBuilder embedBuilder = new EmbedBuilder();
                if(game.firstTeamInfo.id == winnerTeamId) {
                    embedBuilder.setTitle("Победитель: " + game.firstTeamInfo.name);
                    embedBuilder.addField("Рейтинг", """
                            Рейтинг %winner%: +%winner_rating% (+%winner_rating_changes%)
                            Рейтинг %loser%: +%loser_rating% (%loser_rating_changes%)"""
                            .replace("%winner%", gameCategoryManager.game.firstTeamInfo.role.getAsMention())
                            .replace("%winner_rating%", String.valueOf(firstTeamRating))
                            .replace("%winner_rating_changes%", String.valueOf(firstTeamRating - game.firstTeamInfo.points))
                            .replace("%loser%", gameCategoryManager.game.secondTeamInfo.role.getAsMention())
                            .replace("%loser_rating%", String.valueOf(secondTeamRating))
                            .replace("%loser_rating_changes%", String.valueOf(secondTeamRating - game.secondTeamInfo.points)), true);
                } else if(game.secondTeamInfo.id == winnerTeamId) {
                    embedBuilder.setTitle("Победитель: " + game.secondTeamInfo.name);
                    embedBuilder.addField("Рейтинг", """
                            Рейтинг %winner%: +%winner_rating% (+%winner_rating_changes%)
                            Рейтинг %loser%: +%loser_rating% (%loser_rating_changes%)"""
                            .replace("%winner%", gameCategoryManager.game.secondTeamInfo.role.getAsMention())
                            .replace("%winner_rating%", String.valueOf(secondTeamRating))
                            .replace("%winner_rating_changes%", String.valueOf(secondTeamRating - game.secondTeamInfo.points))
                            .replace("%loser%", gameCategoryManager.game.firstTeamInfo.role.getAsMention())
                            .replace("%loser_rating%", String.valueOf(firstTeamRating))
                            .replace("%loser_rating_changes%", String.valueOf(firstTeamRating - game.firstTeamInfo.points)), true);
                }

                embedBuilder.setColor(3092790);
                embedBuilder.addField("Помощник", game.assistantAccount.username, false);

                gameCategoryManager.gameChannel.channel.editMessageEmbedsById(gameCategoryManager.gameChannel.channelMessage.getId(), embedBuilder.build()).queue();
                embedBuilder.clear();

                gameManager.deleteGame(game);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        gameManager.deleteGame(gameCategoryManager.category.getId());
                    }
                }, 30000);
            }
        }

        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement finishStatement = connection.prepareStatement("""
                INSERT INTO team_finished_games_history (first_team_id, first_team_captain_id, second_team_id,
                second_team_captain_id, format, map_name, match_id, winner_team_id, first_team_rating_changes,
                second_team_rating_changes, assistant_id, finished_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);""", Statement.RETURN_GENERATED_KEYS);
            finishStatement.setInt(1, game.firstTeamInfo.id);
            finishStatement.setInt(2, game.firstTeamInfo.captain.id);
            finishStatement.setInt(3, game.secondTeamInfo.id);
            finishStatement.setInt(4, game.secondTeamInfo.captain.id);
            finishStatement.setString(5, game.format);
            finishStatement.setString(6, game.gameMap.getName());
            finishStatement.setString(7, matchId);
            finishStatement.setInt(8, winnerTeamId);
            finishStatement.setInt(9, firstTeamRating - game.firstTeamInfo.points);
            finishStatement.setInt(10, secondTeamRating - game.secondTeamInfo.points);
            finishStatement.setInt(11, game.assistantAccount.id);
            finishStatement.setTimestamp(12, Timestamp.from(Instant.now()));
            finishStatement.executeUpdate();
            ResultSet createResultSet = finishStatement.getGeneratedKeys();
            if(createResultSet.next()) {
                int finishedGameId = createResultSet.getInt(1);

                List<Integer> firstTeamPlayersId = new ArrayList<>();
                for(UserAccount userAccount : game.firstTeamInfo.members) {
                    firstTeamPlayersId.add(userAccount.id);
                }

                for(int playerId : firstTeamPlayersId) {
                    PreparedStatement playersStatement = connection.prepareStatement(
                            "INSERT INTO team_finished_games_players_history (finished_game_id, team_id, player_id) VALUES (?, ?, ?);");
                    playersStatement.setInt(1, finishedGameId);
                    playersStatement.setInt(2, game.firstTeamInfo.id);
                    playersStatement.setInt(3, playerId);
                    playersStatement.executeUpdate();
                }

                List<Integer> secondTeamPlayersId = new ArrayList<>();
                for(UserAccount userAccount : game.secondTeamInfo.members) {
                    secondTeamPlayersId.add(userAccount.id);
                }

                for(int playerId : secondTeamPlayersId) {
                    PreparedStatement playersStatement = connection.prepareStatement(
                            "INSERT INTO team_finished_games_players_history (finished_game_id, team_id, player_id) VALUES (?, ?, ?);");
                    playersStatement.setInt(1, finishedGameId);
                    playersStatement.setInt(2, game.secondTeamInfo.id);
                    playersStatement.setInt(3, playerId);
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
     * @param wonBeds Количество выигранных кроватей команды
     * @param lostBeds Количество проигранных кроватей команды
     * @param id Id команды
     */
    public void finishGameTeam(int points, int games, int wins, int wonBeds, int lostBeds, int id) {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement playersStatement = connection.prepareStatement("""
                UPDATE teams SET points = ?, games = games + ?, wins = wins + ?, won_beds = won_beds + ?, lost_beds = lost_beds + ?
                WHERE id = ? AND is_deleted is null;""");
            playersStatement.setInt(1, points);
            playersStatement.setInt(2, games);
            playersStatement.setInt(3, wins);
            playersStatement.setInt(4, wonBeds);
            playersStatement.setInt(5, lostBeds);
            playersStatement.setInt(6, id);
            playersStatement.executeUpdate();
            DangerBot.getInstance().teamsChannel.updateTopMessage();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Представляет собой команду в поиске игры
     */
    private static class TeamInGameSearch {

        /** Id команды */
        public int id;

        /** Формат игры */
        public String format;

        /** Id начавшего поиск игры */
        public int captainId;

        /**
         * Инициализирует команду в поиске игры
         * @param id Id команды
         * @param format Формат игры
         * @param captainId Id начавшего поиск игры
         */
        public TeamInGameSearch(int id, String format, int captainId) {
            this.id = id;
            this.format = format;
            this.captainId = captainId;
        }
    }
}
