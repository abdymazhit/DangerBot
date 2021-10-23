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
 * Команда добавления трансляции
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class StreamAddCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщений
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member youtuber = event.getMember();

        if(youtuber == null) return;
        if(event.getAuthor().isBot()) return;

        if(MTHD.getInstance().streamsChannel.channel.equals(messageChannel)) {
            String contentRaw = message.getContentRaw();

            if(!youtuber.getRoles().contains(UserRole.YOUTUBE.getRole())
               && !youtuber.getRoles().contains(UserRole.ADMIN.getRole()) ) {
                message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                return;
            }

            if(!youtuber.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
                message.reply("Ошибка! Вы не авторизованы!").queue();
                return;
            }

            int youtuberId = MTHD.getInstance().database.getUserId(youtuber.getId());
            if(youtuberId < 0) {
                message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                return;
            }

            if(contentRaw.startsWith("!stream add")) {
                String[] command = message.getContentRaw().split(" ");

                if(command.length == 2) {
                    message.reply("Ошибка! Укажите ссылку на трансляцию!").queue();
                    return;
                }

                if(command.length > 3) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                String streamLink = command[2];

                if(!streamLink.contains("https://www.youtube.com/watch?v=")) {
                    message.reply("Ошибка! Такой трансляции не существует!").queue();
                    return;
                }

                String errorMessage = addStream(youtuberId, streamLink, youtuberId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                MTHD.getInstance().liveStreamsManager.addLiveStream(streamLink);

                message.reply("Трансляция успешно добавлена! Ссылка на трансляцию: %stream_link%"
                        .replace("%stream_link%", streamLink)).queue();
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }

    /**
     * Добавляет трансляцию
     * @param youtuberId Id ютубера
     * @param link Ссылка на трансляцию
     * @param adderId Id добавляющего
     * @return Текст ошибки добавления трансляции
     */
    private String addStream(int youtuberId, String link, int adderId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement addStatement = connection.prepareStatement("""
                INSERT INTO streams (youtuber_id, link) SELECT ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM streams WHERE youtuber_id = ?);""",
                    Statement.RETURN_GENERATED_KEYS);
            addStatement.setInt(1, youtuberId);
            addStatement.setString(2, link);
            addStatement.setInt(3, youtuberId);
            addStatement.executeUpdate();

            ResultSet createResultSet = addStatement.getGeneratedKeys();
            if(createResultSet.next()) {
                PreparedStatement historyStatement = connection.prepareStatement(
                        "INSERT INTO streams_addition_history (youtuber_id, link, adder_id, added_at) VALUES (?, ?, ?, ?);");
                historyStatement.setInt(1, youtuberId);
                historyStatement.setString(2, link);
                historyStatement.setInt(3, adderId);
                historyStatement.setTimestamp(4, Timestamp.from(Instant.now()));
                historyStatement.executeUpdate();

                // Вернуть значение, что трансляция успешно добавлена
                return null;
            } else {
                return "Ошибка! Вы уже ведете трансляцию!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении трансляции! Свяжитесь с разработчиком бота!";
        }
    }
}

