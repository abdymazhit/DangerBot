package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.TeamInGameSearch;
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
 * Канал поиска игры
 *
 * @version   12.09.2021
 * @author    Islam Abdymazhit
 */
public class FindGameChannel extends Channel {

    /** Информационное сообщение о доступных помощниках */
    public Message channelAvailableAssistantsMessage;

    /** Информационное сообщение о командах в поиске игры */
    public Message channelTeamsInGameSearchMessage;


    /**
     * Инициализирует канал поиска игры
     */
    public FindGameChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            deleteChannel(category, "find-game");

            try {
                ChannelAction<TextChannel> createAction = createChannel(category, "find-game", 1);
                createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(UserRole.LEADER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(UserRole.MEMBER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
                channel = createAction.submit().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            sendChannelMessage();
            updateTeamsInGameSearchMessage();
            updateAvailableAssistantsMessage();
        }
    }

    /**
     * Отправляет сообщение канала поиска игры
     */
    private void sendChannelMessage() {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Доступные команды");
            embedBuilder.setDescription("Доступные форматы игры: 4x2 , 6x2");
            embedBuilder.setColor(0xFF58B9FF);
            embedBuilder.addField("Добавить команду в поиск игры", "`!find game <FORMAT>`", false);
            embedBuilder.addField("Удалить команду из поиска игры", "`!find leave`", false);
            channelMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет информационное сообщение о доступных помощниках
     */
    private void sendAvailableAssistantsMessage(List<String> assistants) {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            String title = "```" +
                    "        ДОСТУПНЫЕ ПОМОЩНИКИ        " +
                    "```";
            embedBuilder.setTitle(title);
            embedBuilder.setColor(3092790);

            StringBuilder assistantsString = new StringBuilder();
            for(String assistant : assistants) {
                assistantsString.append(assistant).append("\n");
            }
            embedBuilder.addField("Name", assistantsString.toString(), true);

            if(channelAvailableAssistantsMessage == null) {
                channelAvailableAssistantsMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            } else {
                channelAvailableAssistantsMessage.editMessageEmbeds(embedBuilder.build()).queue();
            }

            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Обновляет список доступных помощников
     */
    public void updateAvailableAssistantsMessage() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT username FROM users WHERE id = (SELECT assistant_id FROM available_assistants);");
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            List<String> assistants = new ArrayList<>();
            while (resultSet.next()) {
                assistants.add(resultSet.getString("username"));
            }

            sendAvailableAssistantsMessage(assistants);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет информационное сообщение о командах в поиске игры
     */
    private void sendTeamsInGameSearchMessage(List<TeamInGameSearch> teamInGameSearchList) {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            String title = "```" +
                    "        КОМАНДЫ В ПОИСКЕ ИГРЫ        " +
                    "```";
            embedBuilder.setTitle(title);
            embedBuilder.setColor(3092790);

            StringBuilder teamsNamesString = new StringBuilder();
            StringBuilder formatsString = new StringBuilder();
            StringBuilder starterUsernamesString = new StringBuilder();

            for(TeamInGameSearch teamInGameSearch : teamInGameSearchList) {
                teamsNamesString.append(teamInGameSearch.teamName).append("\n");
                formatsString.append(teamInGameSearch.format).append("\n");
                starterUsernamesString.append(teamInGameSearch.starterUsername).append("\n");
            }

            embedBuilder.addField("Name", teamsNamesString.toString(), true);
            embedBuilder.addField("Format", formatsString.toString(), true);
            embedBuilder.addField("Starter Username", starterUsernamesString.toString(), true);

            if(channelTeamsInGameSearchMessage == null) {
                channelTeamsInGameSearchMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            } else {
                channelTeamsInGameSearchMessage.editMessageEmbeds(embedBuilder.build()).queue();
            }

            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Обновляет список команд в поиске игры
     */
    public void updateTeamsInGameSearchMessage() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT team_id, format, starter_id FROM teams_in_game_search;");
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            List<TeamInGameSearch> teamInGameSearchList = new ArrayList<>();
            while (resultSet.next()) {
                int teamId = resultSet.getInt("team_id");
                int starterId = resultSet.getInt("starter_id");

                String teamName = null;
                String format = resultSet.getString("format");
                String starterUsername = null;

                PreparedStatement teamNameStatement = connection.prepareStatement(
                        "SELECT name FROM teams WHERE id = ?;");
                teamNameStatement.setInt(1, teamId);
                ResultSet teamNameResultSet = teamNameStatement.executeQuery();
                teamNameStatement.close();

                if(teamNameResultSet.next()) {
                    teamName = teamNameResultSet.getString("name");
                }

                PreparedStatement starterNameStatement = connection.prepareStatement(
                        "SELECT username FROM users WHERE id = ?;");
                starterNameStatement.setInt(1, starterId);
                ResultSet starterNameResultSet = starterNameStatement.executeQuery();
                starterNameStatement.close();

                if(starterNameResultSet.next()) {
                    starterUsername = starterNameResultSet.getString("username");
                }

                teamInGameSearchList.add(new TeamInGameSearch(teamName, format, starterUsername));
            }

            sendTeamsInGameSearchMessage(teamInGameSearchList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}