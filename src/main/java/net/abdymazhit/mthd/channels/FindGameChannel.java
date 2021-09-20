package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
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
 * Канал поиска игры
 *
 * @version   20.09.2021
 * @author    Islam Abdymazhit
 */
public class FindGameChannel extends Channel {

    /** Id информационного сообщения о доступных помощниках */
    public String channelAvailableAssistantsMessageId;

    /**
     * Инициализирует канал поиска игры
     */
    public FindGameChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);

            for(GuildChannel channel : category.getChannels()) {
                if(channel.getName().equals("find-game")) {
                    channel.delete().queue();
                }
            }

            ChannelAction<TextChannel> createAction = category.createTextChannel("find-game").setPosition(2);
            createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(UserRole.LEADER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(UserRole.MEMBER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            createAction.queue(textChannel -> {
                channelId = textChannel.getId();
                updateTeamsInGameSearchCountMessage();
                updateAvailableAssistantsMessage();
            });
        }
    }

    /**
     * Отправляет сообщение канала поиска игры
     */
    private void sendChannelMessage(int teams4x2Count, int teams6x2Count) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Поиск игры");
        embedBuilder.setDescription(
                "Доступные форматы игры: 4x2 , 6x2\n" +
                "\n" +
                "Команд в поиске игры 4x2: `" + teams4x2Count + "`\n" +
                "Команд в поиске игры 6x2: `" + teams6x2Count + "`\n" +
                "\n" +
                "Войти в поиск игры\n" +
                "`!find game <FORMAT>`\n" +
                "\n" +
                "Выйти из поиска игры\n" +
                "`!find leave`\n"
        );
        embedBuilder.setColor(3092790);

        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(channel != null) {
            if(channelMessageId == null) {
                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
            } else {
                channel.editMessageEmbedsById(channelMessageId, embedBuilder.build()).queue();
            }
        }

        embedBuilder.clear();
    }

    /**
     * Обновляет количество команд в поиске игры
     */
    public void updateTeamsInGameSearchCountMessage() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();

            int count4x2 = 0;
            int count6x2 = 0;

            PreparedStatement prepared4x2Statement = connection.prepareStatement(
                    "SELECT COUNT(*) as count FROM teams_in_game_search WHERE format = '4x2';");
            ResultSet result4x2Set = prepared4x2Statement.executeQuery();
            if(result4x2Set.next()) {
                count4x2 = result4x2Set.getInt("count");
            }

            PreparedStatement prepared6x2Statement = connection.prepareStatement(
                    "SELECT COUNT(*) as count FROM teams_in_game_search WHERE format = '6x2';");
            ResultSet result6x2Set = prepared6x2Statement.executeQuery();
            if(result6x2Set.next()) {
                count6x2 = result6x2Set.getInt("count");
            }

            sendChannelMessage(count4x2, count6x2);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет информационное сообщение о доступных помощниках
     */
    private void sendAvailableAssistantsMessage(List<String> assistants) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        String title = "Доступные помощники";
        embedBuilder.setTitle(title);
        embedBuilder.setColor(3092790);

        StringBuilder assistantsString = new StringBuilder();
        if(assistants.isEmpty()) {
            assistantsString.append("Сейчас нет доступных помощников");
        } else {
            for(String assistant : assistants) {
                assistantsString.append("> ").append(assistant).append("\n");
            }
        }
        embedBuilder.setDescription(assistantsString);

        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(channel != null) {
            if(channelAvailableAssistantsMessageId == null) {
                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelAvailableAssistantsMessageId = message.getId());
            } else {
                channel.editMessageEmbedsById(channelAvailableAssistantsMessageId, embedBuilder.build()).queue();
            }
        }

        embedBuilder.clear();
    }

    /**
     * Обновляет список доступных помощников
     */
    public void updateAvailableAssistantsMessage() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT assistant_id FROM available_assistants;");
            ResultSet resultSet = preparedStatement.executeQuery();
            

            List<String> assistants = new ArrayList<>();
            while(resultSet.next()) {
                PreparedStatement usernameStatement = connection.prepareStatement(
                        "SELECT username FROM users WHERE id = ?;");
                usernameStatement.setInt(1, resultSet.getInt("assistant_id"));
                ResultSet usernameResultSet = usernameStatement.executeQuery();

                while(usernameResultSet.next()) {
                    assistants.add(usernameResultSet.getString("username"));   
                }
            }

            sendAvailableAssistantsMessage(assistants);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}