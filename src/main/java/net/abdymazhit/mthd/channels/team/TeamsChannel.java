package net.abdymazhit.mthd.channels.team;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.info.TeamInfo;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Канал команды
 *
 * @version   22.10.2021
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
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Team Rating не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals("teams")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("teams").setPosition(0).setSlowmode(15)
                .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.AUTHORIZED.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    channel = textChannel;
                    updateTopMessage();
                    sendChannelMessage();
                });
    }

    /**
     * Отправляет сообщение о доступных командах для авторизованных пользователей
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Доступные команды");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
                Посмотреть информацию о команд
                `!team info <NAME>`""");
        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
        embedBuilder.clear();
    }

    /**
     * Обновляет информационное сообщение о лучших командах
     */
    public void updateTopMessage() {
        List<TeamInfo> teams = getTopTeams();

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
        for(TeamInfo teamInfo : teams) {
            teamsNamesString.append(teamInfo.name.replace("_", "\\_")).append("\n");
        }
        embedBuilder.addField("Name", teamsNamesString.toString(), true);

        StringBuilder teamsPointsString = new StringBuilder();
        for(TeamInfo teamInfo : teams) {
            teamsPointsString.append(teamInfo.points).append("\n");
        }
        embedBuilder.addField("Points", teamsPointsString.toString(), true);

        if(channelTopTeamsMessage == null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelTopTeamsMessage = message);
        } else {
            channel.editMessageEmbedsById(channelTopTeamsMessage.getId(), embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Получает список лучших команд
     * @return Список лучших команд
     */
    public List<TeamInfo> getTopTeams() {
        List<TeamInfo> teams = new ArrayList<>();
        try {
            ResultSet resultSet = MTHD.getInstance().database.getConnection().createStatement().executeQuery("""
                WITH TEAMS AS(SELECT *, RANK() OVER(ORDER BY points DESC) RATING FROM teams)
                SELECT id, name, points FROM TEAMS WHERE RATING <= 20 AND is_deleted IS NULL;""");
            while(resultSet.next()) {
                TeamInfo teamInfo = new TeamInfo(resultSet.getInt(1));
                teamInfo.name = resultSet.getString("name");
                teamInfo.points = resultSet.getInt("points");
                teams.add(teamInfo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
    }
}
