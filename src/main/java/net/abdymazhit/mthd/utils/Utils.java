package net.abdymazhit.mthd.utils;

import com.google.gson.JsonArray;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.customs.UserAccount;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Представляет собой инструменты для упрощения работы
 *
 * @version   08.09.2021
 * @author    Islam Abdymazhit
 */
public class Utils {

    /** Объект, отвечает за создание embed сообщений */
    private final EmbedBuilder embedBuilder;

    /**
     * Инициализирует инструменты
     */
    public Utils() {
        embedBuilder = new EmbedBuilder();
    }

    /**
     * Отправляет GET запрос по URL
     * @param url URL
     * @return Результат запроса в типе String
     */
    public String sendGetRequest(String url) {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Отправляет POST запрос по URL
     * @param url URL
     * @param jsonArray JSON массив
     * @return Результат запроса в типе String
     */
    public String sendPostRequest(String url, JsonArray jsonArray) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(jsonArray.toString(), ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает информационное embed сообщение команды
     * @param team Команда
     * @return Информационное embed сообщение команды
     */
    public MessageEmbed getTeamInfoMessageEmbed(Team team) {
        String message = """
                **=== %team_name% ===**
                
                Лидер:
                %leader%
                
                Участники:
                %members%
               
                Очков рейтинга: %points%
                Сыгранных игр: %games%
                Выигранных игр: %wins%
                Количество убийств: %kills%
                Количество смертей: %deaths%
                Выигранных кроватей: %won_beds%
                Проигранных кроватей: %lost_beds%
                """;

        StringBuilder leaderString = new StringBuilder();
        if(team.getLeader().isVimeOnline()) {
            leaderString.append("<:emote:884826184729366538> ");
        } else {
            leaderString.append("<:emote:884826184641294346> ");
        }

        if(team.getLeader().isDiscordOnline()) {
            leaderString.append("<:emote:884825784857010196> ");
        } else {
            leaderString.append("<:emote:884825362863910962> ");
        }
        leaderString.append(team.getLeader().getUsername());

        StringBuilder membersString = new StringBuilder();
        for(UserAccount user : team.getMembers()) {
            if(user.isVimeOnline()) {
                membersString.append("<:emote:884826184729366538> ");
            } else {
                membersString.append("<:emote:884826184641294346> ");
            }

            if(user.isDiscordOnline()) {
                membersString.append("<:emote:884825784857010196> ");
            } else {
                membersString.append("<:emote:884825362863910962> ");
            }
            membersString.append(user.getUsername());
        }

        message = message.replace("%team_name%", team.getName());
        message = message.replace("%leader%", leaderString.toString());
        message = message.replace("%members%", membersString.toString());
        message = message.replace("%points%", String.valueOf(team.getPoints()));
        message = message.replace("%games%", String.valueOf(team.getGames()));
        message = message.replace("%wins%", String.valueOf(team.getWins()));
        message = message.replace("%kills%", String.valueOf(team.getKills()));
        message = message.replace("%deaths%", String.valueOf(team.getDeaths()));
        message = message.replace("%won_beds%", String.valueOf(team.getWon_beds()));
        message = message.replace("%lost_beds%", String.valueOf(team.getLost_beds()));

        embedBuilder.setColor(3092790);
        embedBuilder.setDescription(message);
        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}