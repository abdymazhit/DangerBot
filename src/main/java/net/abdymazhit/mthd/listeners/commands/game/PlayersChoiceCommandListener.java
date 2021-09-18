package net.abdymazhit.mthd.listeners.commands.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.game.GameCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.*;

/**
 * Команда выбора игроков на игру
 *
 * @version   18.09.2021
 * @author    Islam Abdymazhit
 */
public class PlayersChoiceCommandListener extends ListenerAdapter {

    /**
     * Событие получения команды
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member starter = event.getMember();

        if(starter == null) return;
        if(event.getAuthor().isBot()) return;

        for(GameCategory gameCategory : MTHD.getInstance().gameManager.getGameCategories()) {
            game(gameCategory, messageChannel, message, starter);
        }
    }

    private void game(GameCategory gameCategory, MessageChannel messageChannel, Message message, Member starter) {
        if(gameCategory.playersChoiceChannel == null) return;
        if(gameCategory.playersChoiceChannel.channelId == null) return;

        if(gameCategory.playersChoiceChannel.channelId.equals(messageChannel.getId())) {
            String contentRaw = message.getContentRaw();
            if(contentRaw.startsWith("!add")) {
                String[] command = contentRaw.split(" ");

                if(command.length == 1) {
                    message.reply("Ошибка! Укажите имя игрока!").queue();
                    return;
                }

                if(command.length > 2) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                int starterId = MTHD.getInstance().database.getUserId(starter.getId());
                if(starterId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                if(isNotStarter(starterId)) {
                    message.reply("Ошибка! Только начавший игру может устанавливать игроков на игру!").queue();
                    return;
                }

                if(!starter.getRoles().contains(gameCategory.firstTeamRole) &&
                        !starter.getRoles().contains(gameCategory.secondTeamRole)) {
                    message.reply("Ошибка! Вы не являетесь участником или лидером участвующей в игре команды!").queue();
                    return;
                }

                String playerName = command[1];

                int starterTeamId = MTHD.getInstance().database.getUserTeamId(starterId);
                if(starterTeamId < 0) {
                    message.reply("Ошибка! Вы не являетесь участником или лидером какой-либо команды!").queue();
                    return;
                }

                UserAccount playerAccount = MTHD.getInstance().database.getUserIdAndDiscordId(playerName);
                if(playerAccount == null) {
                    message.reply("Ошибка! Игрок не зарегистрированы на сервере!").queue();
                    return;
                }

                int playerTeamId = MTHD.getInstance().database.getUserTeamId(playerAccount.getId());
                if(playerTeamId < 0) {
                    message.reply("Ошибка! Игрок не является участником или лидером какой-либо команды!").queue();
                    return;
                }

                if(playerTeamId == gameCategory.game.firstTeamId) {
                    if(gameCategory.game.format.equals("4x2")) {
                        if(gameCategory.game.firstTeamPlayers.size() > 3) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    } else if(gameCategory.game.format.equals("6x2")) {
                        if(gameCategory.game.firstTeamPlayers.size() > 5) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    }
                } else if(playerTeamId == gameCategory.game.secondTeamId) {
                    if(gameCategory.game.format.equals("4x2")) {
                        if(gameCategory.game.secondTeamPlayers.size() > 3) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    } else if(gameCategory.game.format.equals("6x2")) {
                        if(gameCategory.game.secondTeamPlayers.size() > 5) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    }
                }

                if(starterTeamId != playerTeamId) {
                    message.reply("Ошибка! Вы можете добавлять только игроков своей команды!").queue();
                    return;
                }

                if(!gameCategory.game.gameState.equals(GameState.PLAYERS_CHOICE)) {
                    message.reply("Ошибка! Стадия выбора игроков закончена!").queue();
                    return;
                }

                String errorMessage = addPlayerToGame(playerTeamId, playerAccount.getId());
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                message.reply("Вы успешно добавили игрока в игру!").queue();
                gameCategory.playersChoiceChannel.updateGamePlayersMessage();
            } else if(contentRaw.startsWith("!delete")) {
                String[] command = contentRaw.split(" ");

                if(command.length == 1) {
                    message.reply("Ошибка! Укажите имя игрока!").queue();
                    return;
                }

                if(command.length > 2) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                int starterId = MTHD.getInstance().database.getUserId(starter.getId());
                if(starterId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                if(isNotStarter(starterId)) {
                    message.reply("Ошибка! Только начавший игру может устанавливать игроков на игру!").queue();
                    return;
                }

                if(!starter.getRoles().contains(gameCategory.firstTeamRole) &&
                        !starter.getRoles().contains(gameCategory.secondTeamRole)) {
                    message.reply("Ошибка! Вы не являетесь участником или лидером участвующей в игре команды!").queue();
                    return;
                }

                String playerName = command[1];

                int starterTeamId = MTHD.getInstance().database.getUserTeamId(starterId);
                if(starterTeamId < 0) {
                    message.reply("Ошибка! Вы не являетесь участником или лидером какой-либо команды!").queue();
                    return;
                }

                UserAccount playerAccount = MTHD.getInstance().database.getUserIdAndDiscordId(playerName);
                if(playerAccount == null) {
                    message.reply("Ошибка! Игрок не зарегистрированы на сервере!").queue();
                    return;
                }

                int playerTeamId = MTHD.getInstance().database.getUserTeamId(playerAccount.getId());
                if(playerTeamId < 0) {
                    message.reply("Ошибка! Игрок не является участником или лидером какой-либо команды!").queue();
                    return;
                }

                if(starterTeamId != playerTeamId) {
                    message.reply("Ошибка! Вы можете удалять только игроков своей команды!").queue();
                    return;
                }

                if(!gameCategory.game.gameState.equals(GameState.PLAYERS_CHOICE)) {
                    message.reply("Ошибка! Стадия выбора игроков закончена!").queue();
                    return;
                }

                String errorMessage = removePlayerFromGame(playerTeamId, playerAccount.getId());
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                message.reply("Вы успешно удалили игрока из игры!").queue();
                gameCategory.playersChoiceChannel.updateGamePlayersMessage();
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }

    /**
     * Проверяет, является ли пользователь не начавшим игру
     * @param starterId Id начавшего игру
     * @return Значение, является ли пользователь не начавшим игру
     */
    private boolean isNotStarter(int starterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT 1 FROM live_games WHERE first_team_starter_id = ? OR second_team_starter_id = ?;");
            preparedStatement.setInt(1, starterId);
            preparedStatement.setInt(2, starterId);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            return !resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Добавляет игрока в список участвующих в игре игроков
     * @param teamId Id команды
     * @param playerId Id игрока
     * @return Текст ошибки добавления
     */
    private String addPlayerToGame(int teamId, int playerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement createStatement = connection.prepareStatement(
                    "INSERT INTO live_games_players (team_id, player_id) SELECT ?, ? " +
                            "WHERE NOT EXISTS (SELECT 1 FROM live_games_players WHERE team_id = ? AND player_id = ?);", Statement.RETURN_GENERATED_KEYS);
            createStatement.setInt(1, teamId);
            createStatement.setInt(2, playerId);
            createStatement.setInt(3, teamId);
            createStatement.setInt(4, playerId);
            createStatement.executeUpdate();
            ResultSet createResultSet = createStatement.getGeneratedKeys();

            if(createResultSet.next()) {
                // Вернуть значение, что игрок успешно добавлен в список участвующих в игре игроков
                return null;
            } else {
                return "Ошибка! Игрок уже участвует в игре!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении Вас в список участвующих в игре игроков! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Удаляет игрока из списка участвующих в игре игроков
     * @param teamId Id команды
     * @param playerId Id игрока
     * @return Текст ошибки удаления
     */
    private String removePlayerFromGame(int teamId, int playerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement createStatement = connection.prepareStatement(
                    "DELETE FROM live_games_players WHERE team_id = ? AND player_id = ? " +
                            "AND EXISTS (SELECT 1 FROM live_games_players WHERE team_id = ? AND player_id = ?);", Statement.RETURN_GENERATED_KEYS);
            createStatement.setInt(1, teamId);
            createStatement.setInt(2, playerId);
            createStatement.setInt(3, teamId);
            createStatement.setInt(4, playerId);
            createStatement.executeUpdate();
            ResultSet createResultSet = createStatement.getGeneratedKeys();

            if(createResultSet.next()) {
                // Вернуть значение, что игрок успешно удален из списока участвующих в игре игроков
                return null;
            } else {
                return "Ошибка! Вы не участвуете в игре!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении Вас из списка участвующих в игре игроков! Свяжитесь с разработчиком бота!";
        }
    }
}
