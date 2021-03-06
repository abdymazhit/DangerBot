package net.abdymazhit.dangerbot.listeners.commands.single;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Команда поиск игры
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class SingleFindGameCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщений
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member member = event.getMember();

        if(!DangerBot.getInstance().singleFindGameChannel.channel.equals(messageChannel)) return;
        if(member == null) return;
        if(event.getAuthor().isBot()) return;

        String contentRaw = message.getContentRaw();
        String[] command = contentRaw.split(" ");

        if(contentRaw.startsWith("!find")) {
            if(!member.getRoles().contains(UserRole.SINGLE_RATING.getRole())) {
                message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                return;
            }

            if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
                message.reply("Ошибка! Вы не авторизованы!").queue();
                return;
            }

            if(contentRaw.startsWith("!find game")) {
                if(command.length == 2) {
                    message.reply("Ошибка! Укажите формат игры!").queue();
                    return;
                }

                if(command.length > 3) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                String format = command[2];

                if(!format.equals("4x2") && !format.equals("6x2")) {
                    message.reply("Ошибка! Неверный формат игры!").queue();
                    return;
                }

                int playerId = DangerBot.getInstance().database.getUserId(member.getId());
                if(playerId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                if(DangerBot.getInstance().database.hasNotSingleRating(playerId)) {
                    message.reply("Ошибка! Вы не владеете статусом Single Rating!").queue();
                    return;
                }

                List<Integer> playersInLiveGames = getPlayersInLiveGames();
                if(playersInLiveGames.contains(playerId)) {
                    message.reply("Ошибка! Вы уже участвуете в игре!").queue();
                    return;
                }

                if(member.getRoles().contains(UserRole.ASSISTANT.getRole()) || member.getRoles().contains(UserRole.ADMIN.getRole())) {
                    List<Integer> assistantsInLiveGames = DangerBot.getInstance().database.getAssistantsInLiveGames();
                    if(assistantsInLiveGames.contains(playerId)) {
                        message.reply("Ошибка! Вы сейчас проводите игру!").queue();
                        return;
                    }
                }

                try {
                    Connection connection = DangerBot.getInstance().database.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "SELECT finished_at FROM players_bans WHERE player_id = ?;");
                    preparedStatement.setInt(1, playerId);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if(resultSet.next()) {
                        Timestamp finishedAt = resultSet.getTimestamp("finished_at");
                        Timestamp now = Timestamp.from(Instant.now());

                        int minutes = (int) (finishedAt.getTime() - now.getTime()) / 60000;

                        if(minutes > 0) {
                            message.reply("Вы заблокированы! До окончания бана: `%minutes% мин.`"
                                    .replace("%minutes%", String.valueOf(minutes))).queue();
                            return;
                        } else {
                            PreparedStatement deleteStatement = connection.prepareStatement(
                                    "DELETE FROM players_bans WHERE player_id = ?");
                            deleteStatement.setInt(1, playerId);
                            deleteStatement.executeUpdate();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                String errorMessage = addPlayerToPlayersInGameSearch(playerId, format);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                message.reply("Вы успешно добавлены в поиск игры!").queue();
                DangerBot.getInstance().singleFindGameChannel.updatePlayersInGameSearchCountMessage();
                DangerBot.getInstance().gameManager.singleGameManager.tryStartGame();
            } else if(contentRaw.equals("!find leave")) {
                int playerId = DangerBot.getInstance().database.getUserId(member.getId());
                if(playerId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                if(DangerBot.getInstance().database.hasNotSingleRating(playerId)) {
                    message.reply("Ошибка! Вы не владеете статусом Single Rating!").queue();
                    return;
                }

                String errorMessage = removePlayerFromPlayersInGameSearch(playerId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                message.reply("Вы успешно удалены из поиска игры!").queue();
                DangerBot.getInstance().singleFindGameChannel.updatePlayersInGameSearchCountMessage();
                DangerBot.getInstance().gameManager.singleGameManager.tryStartGame();
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        } else {
            message.reply("Ошибка! Неверная команда!").queue();
        }
    }

    /**
     * Добавляет игрока в поиск игры
     * @param playerId Id игрока
     * @param format Формат игры
     * @return Текст ошибки добавления игрока
     */
    private String addPlayerToPlayersInGameSearch(int playerId, String format) {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO players_in_game_search (player_id, format, joined_at) SELECT ?, ?, ? " +
                    "WHERE NOT EXISTS (SELECT 1 FROM players_in_game_search WHERE player_id = ?);",
                    Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, playerId);
            preparedStatement.setString(2, format);
            preparedStatement.setTimestamp(3, Timestamp.from(Instant.now()));
            preparedStatement.setInt(4, playerId);
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if(resultSet.next()) {
                // Вернуть значение, что игрок успешно добавлен в поиск игры
                return null;
            } else {
                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE players_in_game_search SET joined_at = ? WHERE player_id = ? AND format = ?");
                updateStatement.setTimestamp(1, Timestamp.from(Instant.now()));
                updateStatement.setInt(2, playerId);
                updateStatement.setString(3, format);
                updateStatement.executeUpdate();
                return "Ваше время захода в поиск игры обновлено!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении вас в поиск игры! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Удаляет игрока из поиска игры
     * @param playerId Id игрока
     * @return Текст ошибки удаления игрока
     */
    private String removePlayerFromPlayersInGameSearch(int playerId) {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "DELETE FROM players_in_game_search WHERE player_id = ?", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, playerId);
            preparedStatement.executeUpdate();

            // Вернуть значение, что игрок успешно удален из поиска игры
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении вас из поиска игры! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Получает список игроков в активных играх
     * @return Список игроков в активных играх
     */
    private List<Integer> getPlayersInLiveGames() {
        List<Integer> playersInLiveGames = new ArrayList<>();
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT first_team_captain_id, second_team_captain_id FROM single_live_games;");
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                playersInLiveGames.add(resultSet.getInt("first_team_captain_id"));
                playersInLiveGames.add(resultSet.getInt("second_team_captain_id"));
            }

            PreparedStatement playersStatement = connection.prepareStatement(
                    "SELECT player_id FROM single_live_games_players;");
            ResultSet playersResultSet = playersStatement.executeQuery();
            while(playersResultSet.next()) {
                playersInLiveGames.add(playersResultSet.getInt("player_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playersInLiveGames;
    }
}
