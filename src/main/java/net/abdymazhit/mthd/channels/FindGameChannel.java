package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
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
 * @version   13.09.2021
 * @author    Islam Abdymazhit
 */
public class FindGameChannel extends Channel {

    /** Информационное сообщение о доступных помощниках */
    public Message channelAvailableAssistantsMessage;

    /**
     * Инициализирует канал поиска игры
     */
    public FindGameChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            deleteChannel(category, "find-game");

            try {
                ChannelAction<TextChannel> createAction = createChannel(category, "find-game", 2);
                createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(UserRole.LEADER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(UserRole.MEMBER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
                channel = createAction.submit().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            updateTeamsInGameSearchCountMessage();
            updateAvailableAssistantsMessage();
        }
    }

    /**
     * Отправляет сообщение канала поиска игры
     */
    private void sendChannelMessage(int teamsCount) {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Доступные команды");
            embedBuilder.setDescription("Доступные форматы игры: 4x2 , 6x2\n" +
                    "Команд в поиске игры: " + teamsCount + "\n");
            embedBuilder.setColor(0xFF58B9FF);
            embedBuilder.addField("Войти в поиск игры", "`!find game <FORMAT>`", false);
            embedBuilder.addField("Выйти из поиска игры", "`!find leave`", false);

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
     * Обновляет количество команд в поиске игры
     */
    public void updateTeamsInGameSearchCountMessage() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT COUNT(*) as count FROM teams_in_game_search;");
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                sendChannelMessage(resultSet.getInt("count"));
            }
        } catch (SQLException e) {
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
            while(resultSet.next()) {
                assistants.add(resultSet.getString("username"));
            }

            sendAvailableAssistantsMessage(assistants);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}