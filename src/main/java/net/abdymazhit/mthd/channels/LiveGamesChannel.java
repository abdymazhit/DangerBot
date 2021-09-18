package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.*;
import java.util.*;

/**
 * Канал активных игр
 *
 * @version   18.09.2021
 * @author    Islam Abdymazhit
 */
public class LiveGamesChannel extends Channel {

    /** Сообщения о активных играх */
    public Map<Game, String> channelLiveGamesMessagesId;

    /**
     * Инициализирует канал активных игр
     */
    public LiveGamesChannel() {
        channelLiveGamesMessagesId = new HashMap<>();

        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);

            for(GuildChannel channel : category.getChannels()) {
                if(channel.getName().equals("live-games")) {
                    channel.delete().queue();
                }
            }

            ChannelAction<TextChannel> createAction = category.createTextChannel("live-games").setPosition(1);
            createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(UserRole.AUTHORIZED.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.MESSAGE_WRITE));
            createAction.queue(textChannel -> {
                channelId = textChannel.getId();
                updateLiveGamesMessages();
            });
        }
    }

    /**
     * Обновляет сообщения активных игр
     */
    public void updateLiveGamesMessages() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM live_games;");
            ResultSet resultSet = preparedStatement.executeQuery();
            

            List<Game> games = new ArrayList<>();
            while(resultSet.next()) {
                int id = resultSet.getInt(1);
                int firstTeamId = resultSet.getInt("first_team_id");
                int firstTeamStarterId = resultSet.getInt("first_team_starter_id");
                int secondTeamId = resultSet.getInt("second_team_id");
                int secondTeamStarterId = resultSet.getInt("second_team_starter_id");
                String format = resultSet.getString("format");
                int assistantId = resultSet.getInt("assistant_id");
                Timestamp startedAt = resultSet.getTimestamp("started_at");

                Game game = new Game(id, firstTeamId, firstTeamStarterId, secondTeamId, secondTeamStarterId,
                        format, assistantId, startedAt);
                games.add(game);
            }

            Map<Game, String> channelLiveGamesMessages = new HashMap<>(this.channelLiveGamesMessagesId);
            Map<Game, String> liveGamesMessages = new HashMap<>(this.channelLiveGamesMessagesId);

            for(Game liveGame : games) {
                boolean isSent = false;
                for(Game game : channelLiveGamesMessages.keySet()) {
                    if(liveGame.id == game.id) {
                        isSent = true;
                        liveGamesMessages.remove(game);
                    }
                }

                if(!isSent) {
                    sendLiveGamesMessage(liveGame);
                }
            }

            for(Game game : liveGamesMessages.keySet()) {
                String messageId = this.channelLiveGamesMessagesId.get(game);
                this.channelLiveGamesMessagesId.remove(game);
                TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
                if(channel != null) {
                    channel.deleteMessageById(messageId).queue();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет информационное сообщение о активных играх
     */
    private void sendLiveGamesMessage(Game game) {
        game.getData();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        String title = "```" +
                "        " + game.firstTeamName + "   VS   " + game.secondTeamName + "        " +
                "```";
        embedBuilder.setTitle(title);
        embedBuilder.setColor(3092790);

        embedBuilder.addField("Формат", game.format, true);
        embedBuilder.addField("Помощник", game.assistantName, true);

        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(channel != null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelLiveGamesMessagesId.put(game, message.getId()));
        }

        embedBuilder.clear();
    }
}