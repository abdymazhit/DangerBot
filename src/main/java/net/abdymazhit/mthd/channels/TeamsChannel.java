package net.abdymazhit.mthd.channels;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.EnumSet;
import java.util.List;

/**
 * Канал команды
 *
 * @version   22.09.2021
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
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Team Rating не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals("teams")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("teams").setPosition(0)
            .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
            .addPermissionOverride(UserRole.AUTHORIZED.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
            .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .queue(textChannel -> {
                channelId = textChannel.getId();
                updateTopMessage();
                sendChannelMessage(textChannel);
            });
    }

    /**
     * Отправляет сообщение о доступных командах для авторизованных пользователей
     * @param textChannel Канал команды
     */
    private void sendChannelMessage(TextChannel textChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Доступные команды");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
            Посмотреть информацию о команд
            `!team info <NAME>`""");
        textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
        embedBuilder.clear();
    }

    /**
     * Обновляет информационное сообщение о лучших командах
     */
    public void updateTopMessage() {
        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал teams не существует!");
            return;
        }

        List<Team> teams = getTopTeams();

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
            teamsNamesString.append(team.name.replace("_", "\\_")).append("\n");
        }
        embedBuilder.addField("Name", teamsNamesString.toString(), true);

        StringBuilder teamsPointsString = new StringBuilder();
        for(Team team : teams) {
            teamsPointsString.append(team.points).append("\n");
        }
        embedBuilder.addField("Points", teamsPointsString.toString(), true);

        if(channelTopTeamsMessageId == null) {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelTopTeamsMessageId = message.getId());
        } else {
            textChannel.editMessageEmbedsById(channelTopTeamsMessageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Получает лучше команды
     * @return Лучшие команды
     */
    public List<Team> getTopTeams() {
        List<Team> teams = new ArrayList<>();
        try {
            ResultSet resultSet = MTHD.getInstance().database.getConnection().createStatement().executeQuery("""
                WITH TEAMS AS(SELECT *, RANK() OVER(ORDER BY points DESC) RATING FROM teams)
                SELECT id, name, points FROM TEAMS WHERE RATING <= 20 AND is_deleted IS NULL;""");
            while(resultSet.next()) {
                Team team = new Team(resultSet.getInt(1));
                team.name = resultSet.getString("name");
                team.points = resultSet.getInt("points");
                teams.add(team);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
    }
}
