package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Канал команды
 *
 * @version   20.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamsChannel extends Channel {

    /** Id информационного сообщения о лучших командах */
    public String channelTopTeamsMessageId;

    /**
     * Инициализирует канал команды
     */
    public TeamsChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);

            for(GuildChannel channel : category.getChannels()) {
                if(channel.getName().equals("teams")) {
                    channel.delete().queue();
                }
            }

            ChannelAction<TextChannel> createAction = category.createTextChannel("teams").setPosition(0);
            createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(UserRole.AUTHORIZED.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            createAction.queue(textChannel -> {
                channelId = textChannel.getId();

                updateTopMessage();

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Доступные команды");
                embedBuilder.setColor(3092790);
                embedBuilder.setDescription(
                        "Посмотреть информацию о команде\n" +
                        "`!team info <NAME>`"
                );

                TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
                if(channel != null) {
                    channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                }

                embedBuilder.clear();
            });
        }
    }

    /**
     * Отправляет информационное сообщение о лучших командах
     */
    private void sendTopTeamsMessage(List<Team> teams) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        String title = "Топ 20 команд";
        embedBuilder.setTitle(title);
        embedBuilder.setColor(3092790);

        StringBuilder teamsPlaceString = new StringBuilder();
        for(int i = 1; i <= teams.size(); i++) {
            teamsPlaceString.append("> ").append(i).append("\n");
        }
        embedBuilder.addField("Place", teamsPlaceString.toString(), true);

        StringBuilder teamsNamesString = new StringBuilder();
        for(Team team : teams) {
            teamsNamesString.append(team.name).append("\n");
        }
        embedBuilder.addField("Name", teamsNamesString.toString(), true);

        StringBuilder teamsPointsString = new StringBuilder();
        for(Team team : teams) {
            teamsPointsString.append(team.points).append("\n");
        }
        embedBuilder.addField("Points", teamsPointsString.toString(), true);

        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(channel != null) {
            if(channelTopTeamsMessageId == null) {
                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelTopTeamsMessageId = message.getId());
            } else {
                channel.editMessageEmbedsById(channelTopTeamsMessageId, embedBuilder.build()).queue();
            }
        }

        embedBuilder.clear();
    }

    /**
     * Обновляет список лучших игроков
     */
    public void updateTopMessage() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "WITH TEAMS AS(SELECT *, RANK() OVER(ORDER BY points DESC) RATING FROM teams)" +
                            " SELECT id, name, points FROM TEAMS WHERE RATING <= 20 AND is_deleted IS NULL;");
            ResultSet resultSet = preparedStatement.executeQuery();
            

            List<Team> teams = new ArrayList<>();
            while(resultSet.next()) {
                Team team = new Team(resultSet.getInt(1));
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
