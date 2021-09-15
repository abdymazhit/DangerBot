package net.abdymazhit.mthd.listeners.commands.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.game.GameCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Команда выбора игроков на игру
 *
 * @version   15.09.2021
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
            if(gameCategory.playersChoiceChannel == null) return;

            if(gameCategory.playersChoiceChannel.channel.equals(messageChannel)) {
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

                    if(starterTeamId != playerTeamId) {
                        message.reply("Ошибка! Вы можете добавлять только игроков своей команды!").queue();
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
                break;
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
            preparedStatement.close();
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
                            "WHERE NOT EXISTS (SELECT 1 FROM live_games_players WHERE team_id = ? AND player_id = ?)" +
                            "RETURNING id;");
            createStatement.setInt(1, teamId);
            createStatement.setInt(2, playerId);
            createStatement.setInt(3, teamId);
            createStatement.setInt(4, playerId);
            ResultSet createResultSet = createStatement.executeQuery();
            createStatement.close();

            if(createResultSet.next()) {
                // Вернуть значение, что игрок успешно добавлен в список участвующих в игре игроков
                return null;
            } else {
                return "Ошибка! Вы уже участвуете в игре!";
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
                            "AND EXISTS (SELECT 1 FROM live_games_players WHERE team_id = ? AND player_id = ?)" +
                            "RETURNING id;");
            createStatement.setInt(1, teamId);
            createStatement.setInt(2, playerId);
            createStatement.setInt(3, teamId);
            createStatement.setInt(4, playerId);

            ResultSet createResultSet = createStatement.executeQuery();
            createStatement.close();

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
