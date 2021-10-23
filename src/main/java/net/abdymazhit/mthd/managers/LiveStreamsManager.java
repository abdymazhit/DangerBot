package net.abdymazhit.mthd.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.abdymazhit.mthd.MTHD;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Менеджер активных трансляции
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class LiveStreamsManager {

    /** Список ссылок активных трансляции */
    private final List<String> liveStreamsLinks;

    /** Значение, начата ли проверка активных трансляции */
    private boolean isStartedChecking;

    /** Таймер проверки активных трансляции */
    private Timer timer;

    /**
     * Инициализирует менеджер активных трансляции
     */
    public LiveStreamsManager() {
        liveStreamsLinks = new ArrayList<>();
        isStartedChecking = false;
    }

    /**
     * Добавляет ссылку в список активных трансляции
     * @param link Ссылка
     */
    public void addLiveStream(String link) {
        if(!liveStreamsLinks.contains(link)) {
            liveStreamsLinks.add(link);
            MTHD.getInstance().activeBroadcastsChannel.updateLiveStreamsMessages();
        }

        if(!isStartedChecking) {
            startChecking();
        }
    }

    /**
     * Удаляет ссылку из списка активных трансляции
     * @param link Ссылка
     */
    public void removeLiveStream(String link) {
        liveStreamsLinks.remove(link);
        if(liveStreamsLinks.isEmpty()) {
            cancelChecking();
        }
        MTHD.getInstance().activeBroadcastsChannel.updateLiveStreamsMessages();
    }

    /**
     * Начинает проверку трансляции
     */
    private void startChecking() {
        isStartedChecking = true;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for(String link : new ArrayList<>(liveStreamsLinks)) {
                    String streamId = link.replace("https://www.youtube.com/watch?v=", "");
                    HttpGet request = new HttpGet("https://www.googleapis.com/youtube/v3/videos?id=%stream%&key=%api_key%&part=snippet"
                            .replace("%stream%", streamId)
                            .replace("%api_key%", MTHD.getInstance().config.youtubeApiKey));
                    try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request)) {
                        HttpEntity entity = response.getEntity();
                        String jsonString = EntityUtils.toString(entity);
                        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                        JsonObject itemObject = jsonObject.get("items").getAsJsonArray().get(0).getAsJsonObject();
                        JsonObject snippetObject = itemObject.get("snippet").getAsJsonObject();
                        String isLive = snippetObject.get("liveBroadcastContent").getAsString();
                        if(!isLive.equals("live")) {
                            int youtuberId = getYoutuberId(link);
                            if(youtuberId > 0) {
                                MTHD.getInstance().database.deleteStream(youtuberId, youtuberId);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 60000);
    }

    /**
     * Получает id ютубера
     * @param streamLink Ссылка на трансляцию
     * @return Id ютубера
     */
    private int getYoutuberId(String streamLink) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("""
                SELECT youtuber_id FROM streams WHERE link = ?;""", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, streamLink);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getInt("youtuber_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Заканчивает проверку трансляции
     */
    private void cancelChecking() {
        isStartedChecking = false;
        if(timer != null) {
            timer.cancel();
        }
    }
}
