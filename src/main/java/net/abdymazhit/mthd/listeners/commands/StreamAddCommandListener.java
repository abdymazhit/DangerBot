package net.abdymazhit.mthd.listeners.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
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

                String channelId = getChannelId(youtuberId);
                if(channelId == null) {
                    message.reply("Ошибка! Вы не владеете YouTube каналом!").queue();
                    return;
                }

                String streamId = streamLink.replace("https://www.youtube.com/watch?v=", "");
                HttpGet request = new HttpGet("https://www.googleapis.com/youtube/v3/videos?id=%stream%&key=%api_key%&part=snippet"
                        .replace("%stream%", streamId)
                        .replace("%api_key%", MTHD.getInstance().config.youtubeApiKey));
                try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request)) {
                    HttpEntity entity = response.getEntity();
                    String jsonString = EntityUtils.toString(entity);

                    JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                    JsonObject itemObject = jsonObject.get("items").getAsJsonArray().get(0).getAsJsonObject();
                    JsonObject snippetObject = itemObject.get("snippet").getAsJsonObject();

                    String youtubeChannelId = snippetObject.get("channelId").getAsString();
                    if(!channelId.equals(youtubeChannelId)) {
                        message.reply("Ошибка! Вы можете добавить только свою трансляцию!").queue();
                        return;
                    }

                    String title = snippetObject.get("title").getAsString();
                    if(!title.contains("DangerZone") && !title.contains("Danger Zone")) {
                        message.reply("Ошибка! В названии трансляции обязательно должно присутствовать название `DangerZone` или `Danger Zone`!").queue();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    message.reply("Ошибка! Произошла ошибка при запросе в YouTube API! Свяжитесь с разработчиком бота!").queue();
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
     * Получает id канала ютубера
     * @param youtuberId Id ютубера
     * @return Id канала ютубера
     */
    private String getChannelId(int youtuberId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("""
                SELECT channel_id FROM youtubers WHERE youtuber_id = ?;""", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, youtuberId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getString("channel_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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

