package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.*;
import java.time.Instant;

/**
 * Команда поиск игры
 *
 * @version   12.09.2021
 * @author    Islam Abdymazhit
 */
public class FindGameCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщений
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member member = event.getMember();

        if(!MTHD.getInstance().findGameChannel.channel.equals(messageChannel)) return;
        if(member == null) return;
        if(event.getAuthor().isBot()) return;

        String contentRaw = message.getContentRaw();
        String[] command = contentRaw.split(" ");

        if (contentRaw.startsWith("!find")) {
            if(!member.getRoles().contains(UserRole.LEADER.getRole()) &&
                    !member.getRoles().contains(UserRole.MEMBER.getRole()) ) {
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

                int memberId = MTHD.getInstance().database.getUserId(member.getId());
                if(memberId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                int teamId = MTHD.getInstance().database.getUserTeamId(memberId);
                if(teamId < 0) {
                    message.reply("Ошибка! Вы не состоите в команде!").queue();
                    return;
                }

                String errorMessage = addTeamToTeamsInGameSearch(teamId, format, memberId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                message.reply("Ваша команда успешно добавлена в поиск игры!").queue();
                MTHD.getInstance().findGameChannel.updateTeamsInGameSearchMessage();
            } else if (contentRaw.equals("!find leave")) {
                int memberId = MTHD.getInstance().database.getUserId(member.getId());
                if(memberId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                int teamId = MTHD.getInstance().database.getUserTeamId(memberId);
                if(teamId < 0) {
                    message.reply("Ошибка! Вы не состоите в команде!").queue();
                    return;
                }

                String errorMessage = removeTeamFromTeamsInGameSearch(teamId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                message.reply("Ваша команда успешно удалена из поиска игры!").queue();
                MTHD.getInstance().findGameChannel.updateTeamsInGameSearchMessage();
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        } else {
            message.reply("Ошибка! Неверная команда!").queue();
        }
    }

    /**
     * Добавляет команду в поиск игры
     * @param teamId Id команды
     * @param format Формат игры
     * @param starterId Id начавшего
     * @return Текст ошибки добавления команды
     */
    private String addTeamToTeamsInGameSearch(int teamId, String format, int starterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO teams_in_game_search (team_id, format, starter_id, started_at) SELECT ?, ?, ?, ? " +
                            "WHERE NOT EXISTS (SELECT 1 FROM teams_in_game_search WHERE team_id = ?) " +
                            "RETURNING id;");
            preparedStatement.setInt(1, teamId);
            preparedStatement.setString(2, format);
            preparedStatement.setInt(3, starterId);
            preparedStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            preparedStatement.setInt(5, teamId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                // Вернуть значение, что команда успешно добавлена в поиск игры
                return null;
            } else {
                return "Ошибка! Ваша команда уже находится в поиске игры!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении вашей команды в поиск игры! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Удаляет команду из поиска игры
     * @param teamId Id команды
     * @return Текст ошибки удаления команды
     */
    private String removeTeamFromTeamsInGameSearch(int teamId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "DELETE FROM teams_in_game_search WHERE team_id = ? RETURNING id");
            preparedStatement.setInt(1, teamId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                // Вернуть значение, что команда успешно удалена из поиска игры
                return null;
            } else {
                return "Ошибка! Ваша команда не находится в поиске игры!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении вашей команды из поиска игры! Свяжитесь с разработчиком бота!";
        }
    }
}
