package net.abdymazhit.dangerbot.channels;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Channel;
import net.abdymazhit.dangerbot.customs.LiveStream;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Канал активных трансляции
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class ActiveBroadcastsChannel extends Channel {

    /** Сообщения о активных трансляциях */
    public Map<LiveStream, String> channelLiveStreamsMessagesId;

    /**
     * Инициализирует канал активных трансляции
     */
    public ActiveBroadcastsChannel() {
        channelLiveStreamsMessagesId = new HashMap<>();

        List<Category> categories = DangerBot.getInstance().guild.getCategoriesByName("Danger Zone", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Danger Zone не существует!");
        }

        Category category = categories.get(0);

        for(GuildChannel channel : category.getChannels()) {
            if(channel.getName().equals("active-broadcasts")) {
                channel.delete().queue();
            }
        }

        category.createTextChannel("active-broadcasts").setPosition(0)
                .addPermissionOverride(UserRole.AUTHORIZED.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(DangerBot.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE))
                .queue(textChannel -> {
                    channel = textChannel;
                    getLiveStreams();
                });
    }

    /**
     * Обновляет сообщения активных трансляции
     */
    public void updateLiveStreamsMessages() {
        List<LiveStream> streams = getLiveStreams();

        Map<LiveStream, String> channelLiveStreamsMessages = new HashMap<>(channelLiveStreamsMessagesId);
        Map<LiveStream, String> liveStreamsMessages = new HashMap<>(channelLiveStreamsMessagesId);

        for(LiveStream liveStream : streams) {
            boolean isSent = false;
            LiveStream neededStream = null;

            for(LiveStream stream : channelLiveStreamsMessages.keySet()) {
                if(liveStream.id.equals(stream.id)) {
                    isSent = true;
                    neededStream = stream;
                    liveStreamsMessages.remove(stream);
                }
            }

            if(!isSent) {
                sendLiveStreamsMessage(liveStream, null);
            } else {
                sendLiveStreamsMessage(liveStream, channelLiveStreamsMessagesId.get(neededStream));
            }
        }

        for(LiveStream stream : liveStreamsMessages.keySet()) {
            String messageId = channelLiveStreamsMessagesId.get(stream);
            channelLiveStreamsMessagesId.remove(stream);
            channel.deleteMessageById(messageId).queue();
        }
    }

    /**
     * Отправляет информационное сообщение о активной трансляции
     * @param liveStream Активная трансляция
     * @param messageId Id сообщения
     */
    private void sendLiveStreamsMessage(LiveStream liveStream, String messageId) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Активная трансляция");
        embedBuilder.setDescription("""
                Прямо сейчас, `%youtuber%`
                ведет прямую трансляцию на нашем проекте!
                
                %link%"""
                .replace("%youtuber%", liveStream.username)
                .replace("%link%", liveStream.link));
        embedBuilder.setColor(3092790);
        embedBuilder.setThumbnail("http://skin.vimeworld.ru/helm/3d/%username%.png"
                .replace("%username%", liveStream.username));
        if(messageId == null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelLiveStreamsMessagesId.put(liveStream, message.getId()));
        } else {
            channel.editMessageEmbedsById(messageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Получает список активных трансляции
     * @return Список активных трансляции
     */
    public List<LiveStream> getLiveStreams() {
        List<LiveStream> streams = new ArrayList<>();
        try {
            ResultSet resultSet = DangerBot.getInstance().database.getConnection().createStatement().executeQuery("""
                SELECT u.id, u.username, link FROM streams as s
                INNER JOIN users AS u ON u.id = s.youtuber_id""");
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String link = resultSet.getString("link");

                String streamId = link.replace("https://www.youtube.com/watch?v=", "");
                String image = "https://img.youtube.com/vi/" + streamId + "/maxresdefault.jpg";
                String title = getTitle(streamId);
                LiveStream stream = new LiveStream(id, username, link, image, title);
                streams.add(stream);

                DangerBot.getInstance().liveStreamsManager.addLiveStream(link);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return streams;
    }

    /**
     * Получает название трансляции
     * @param streamId Id трансляции
     * @return Название трансляции
     */
    public static String getTitle(String streamId) {
        HttpGet request = new HttpGet("https://www.googleapis.com/youtube/v3/videos?id=%stream%&key=%api_key%&part=snippet"
                .replace("%stream%", streamId)
                .replace("%api_key%", DangerBot.getInstance().config.youtubeApiKey));
        try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String jsonString = EntityUtils.toString(entity);
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            JsonObject itemObject = jsonObject.get("items").getAsJsonArray().get(0).getAsJsonObject();
            JsonObject snippetObject = itemObject.get("snippet").getAsJsonObject();
            return snippetObject.get("title").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}