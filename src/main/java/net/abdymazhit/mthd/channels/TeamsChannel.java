package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Канал команды
 *
 * @version   12.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamsChannel extends Channel {

    /** Информационное сообщение о лучших командах */
    public Message channelTopTeamsMessage;

    /**
     * Инициализирует канал команды
     */
    public TeamsChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            deleteChannel(category, "teams");

            try {
                ChannelAction<TextChannel> createAction = createChannel(category, "teams", 0);
                createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(UserRole.AUTHORIZED.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
                channel = createAction.submit().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            updateTop();
            sendChannelMessage();
        }
    }

    /**
     * Отправляет информационное сообщение о лучших командах
     */
    private void sendTopTeamsMessage(List<Team> teams) {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            String title = "```" +
                    "                      TEAMS RATING                      " +
                    "```";
            embedBuilder.setTitle(title);
            embedBuilder.setColor(3092790);

            StringBuilder teamsNamesString = new StringBuilder();
            for(Team team : teams) {
                teamsNamesString.append("> ").append(team.name).append("\n");
            }
            embedBuilder.addField("Name", teamsNamesString.toString(), true);

            StringBuilder teamsPointsString = new StringBuilder();
            for(Team team : teams) {
                teamsPointsString.append(team.points).append("\n");
            }
            embedBuilder.addField("Points", teamsPointsString.toString(), true);

            StringBuilder teamsPlaceString = new StringBuilder();
            for(int i = 1; i <= teams.size(); i++) {
                teamsPlaceString.append(i).append("\n");
            }
            embedBuilder.addField("Place", teamsPlaceString.toString(), true);

            if(channelMessage == null) {
                channelTopTeamsMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            } else {
                channelTopTeamsMessage.editMessageEmbeds(embedBuilder.build()).queue();
            }

            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет сообщение канала команды
     */
    private void sendChannelMessage() {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Доступные команды");
            embedBuilder.setColor(0xFF58B9FF);
            embedBuilder.addField("Посмотреть информацию о команде",
                    "`!team info <NAME>`", false);
            channelMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
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
                            " SELECT id, name, points FROM TEAMS WHERE RATING <= 20 AND is_deleted IS NULL;");
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            List<Team> teams = new ArrayList<>();
            while (resultSet.next()) {
                Team team = new Team(resultSet.getInt("id"));
                team.name = resultSet.getString("name");
                team.points = resultSet.getInt("points");
                teams.add(team);
            }

            sendTopTeamsMessage(teams);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
