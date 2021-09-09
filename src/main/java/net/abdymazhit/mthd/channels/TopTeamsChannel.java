package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.Team;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Канал TOP 100 команд
 *
 * @version   09.09.2021
 * @author    Islam Abdymazhit
 */
public class TopTeamsChannel extends Channel {

    /**
     * Инициализирует канал TOP 100 команд
     */
    public TopTeamsChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            deleteChannel(category, "top");
            createChannel(category, "top", 2);
        }
        updateTop();
    }

    /**
     * Отправляет сообщение канала TOP 100 команд
     */
    private void sendChannelMessage(List<Team> teams) {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("TOP 100 команд");
            embedBuilder.setColor(0xFF58B9FF);

            StringBuilder description = new StringBuilder();
            for(Team team : teams) {
                description.append(team.name).append(" => ").append(team.points).append("\n");
            }

            embedBuilder.setDescription(description);

            if(channelMessage == null) {
                channelMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            } else {
                channelMessage.editMessageEmbeds(embedBuilder.build()).queue();
            }

            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Обновляет список лучших игроков
     */
    public void updateTop() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "WITH TEAMS AS(SELECT *, RANK() OVER(ORDER BY points DESC) RATING FROM teams)" +
                            " SELECT id, name, points FROM TEAMS WHERE RATING <= 100 AND is_deleted IS NULL;");
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            List<Team> teams = new ArrayList<>();
            while (resultSet.next()) {
                Team team = new Team(resultSet.getInt("id"));
                team.name = resultSet.getString("name");
                team.points = resultSet.getInt("points");
                teams.add(team);
            }

            sendChannelMessage(teams);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
