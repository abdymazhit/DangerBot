package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
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
 * Команда персонала
 *
 * @version   11.09.2021
 * @author    Islam Abdymazhit
 */
public class StaffCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщений
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member assistant = event.getMember();

        if(assistant == null) return;
        if(event.getAuthor().isBot()) return;

        if(MTHD.getInstance().staffChannel.channel.equals(messageChannel)) {
            String contentRaw = message.getContentRaw();

            if(!assistant.getRoles().contains(UserRole.ASSISTANT.getRole()) &&
                    !assistant.getRoles().contains(UserRole.ADMIN.getRole()) ) {
                message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                return;
            }

            if(!assistant.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
                message.reply("Ошибка! Вы не авторизованы!").queue();
                return;
            }

            int assistantId = MTHD.getInstance().database.getUserId(assistant.getId());
            if(assistantId < 0) {
                message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                return;
            }

            if (contentRaw.equals("!ready")) {
                String errorMessage = setReady(assistantId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }
                message.reply("Вы успешно добавлены в таблицу доступных помощников!").queue();
            } else if (contentRaw.equals("!unready")) {
                String errorMessage = setUnready(assistantId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }
                message.reply("Вы успешно удалены из таблицы доступных помощников!").queue();
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }

    /**
     * Добавляет помощника в таблицу доступных помощников
     * @param assistantId Id помощника
     * @return Текст ошибки добавления
     */
    private String setReady(int assistantId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO available_assistants (assistant_id) SELECT ? " +
                            "WHERE NOT EXISTS (SELECT assistant_id FROM available_assistants WHERE assistant_id = ?) " +
                            "RETURNING id;");
            preparedStatement.setInt(1, assistantId);
            preparedStatement.setInt(2, assistantId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                // Вернуть значение, что помощник успешно добавлен в таблицу доступных помощников
                return null;
            } else {
                return "Ошибка! Вы уже в списке доступных помощников!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении в таблицу доступных помощников! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Удаляет помощника из таблицы доступных помощников
     * @param assistantId Id помощника
     * @return Текст ошибки удаления
     */
    private String setUnready(int assistantId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "DELETE FROM available_assistants WHERE assistant_id = ? RETURNING id;");
            preparedStatement.setInt(1, assistantId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                // Вернуть значение, что помощник успешно удален из таблицы доступных помощников
                return null;
            } else {
                return "Ошибка! Вас нет в списке доступных помощников!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении из таблицы доступных помощников! Свяжитесь с разработчиком бота!";
        }
    }
}
