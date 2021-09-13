package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Канал активных игр
 *
 * @version   13.09.2021
 * @author    Islam Abdymazhit
 */
public class LiveGamesChannel extends Channel {

    /** Сообщения о активных играх */
    public Map<Game, Message> channelLiveGamesMessages;

    /**
     * Инициализирует канал активных игр
     */
    public LiveGamesChannel() {
        channelLiveGamesMessages = new HashMap<>();

        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            deleteChannel(category, "live-games");

            try {
                ChannelAction<TextChannel> createAction = createChannel(category, "live-games", 1);
                createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(UserRole.AUTHORIZED.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
                createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
                createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.MESSAGE_WRITE));
                channel = createAction.submit().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            updateLiveGamesMessages();
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
            preparedStatement.close();

            List<Game> games = new ArrayList<>();
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
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

            Map<Game, Message> channelLiveGamesMessages = new HashMap<>(this.channelLiveGamesMessages);

            for(Game liveGame : games) {
                boolean isSent = false;
                for(Game game : channelLiveGamesMessages.keySet()) {
                    if(liveGame.id == game.id) {
                        isSent = true;
                        channelLiveGamesMessages.remove(game);
                        break;
                    }
                }

                if(!isSent) {
                    sendLiveGamesMessage(liveGame);
                }
            }

            for(Game game : channelLiveGamesMessages.keySet()) {
                Message message = this.channelLiveGamesMessages.get(game);
                this.channelLiveGamesMessages.remove(game);
                message.delete().queue();
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

        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            String title = "```" +
                    "        " + game.firstTeamName + "   VS   " + game.secondTeamName + "        " +
                    "```";
            embedBuilder.setTitle(title);
            embedBuilder.setColor(3092790);

            embedBuilder.addField("Формат", game.format, true);
            embedBuilder.addField("Помощник", game.assistantName, true);

            Message message = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            channelLiveGamesMessages.put(game, message);

            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}