package net.abdymazhit.mthd.channels.team;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.LiveGame;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Канал активных игр
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class TeamLiveGamesChannel extends Channel {

    /** Сообщения о активных играх */
    public Map<LiveGame, String> channelLiveGamesMessagesId;

    /**
     * Инициализирует канал активных игр
     */
    public TeamLiveGamesChannel() {
        channelLiveGamesMessagesId = new HashMap<>();

        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Team Rating не существует!");
        }

        Category category = categories.get(0);

        for(GuildChannel channel : category.getChannels()) {
            if(channel.getName().equals("live-games")) {
                channel.delete().queue();
            }
        }

        category.createTextChannel("live-games").setPosition(1)
                .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.AUTHORIZED.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE))
                .queue(textChannel -> {
                    channel = textChannel;
                    updateLiveGamesMessages();
                });
    }

    /**
     * Обновляет сообщения активных игр
     */
    public void updateLiveGamesMessages() {
        List<LiveGame> games = getLiveGames();

        Map<LiveGame, String> channelLiveGamesMessages = new HashMap<>(channelLiveGamesMessagesId);
        Map<LiveGame, String> liveGamesMessages = new HashMap<>(channelLiveGamesMessagesId);

        for(LiveGame liveGame : games) {
            boolean isSent = false;
            LiveGame neededGame = null;

            for(LiveGame game : channelLiveGamesMessages.keySet()) {
                if(liveGame.id == game.id) {
                    isSent = true;
                    neededGame = game;
                    liveGamesMessages.remove(game);
                }
            }

            if(!isSent) {
                sendLiveGamesMessage(liveGame, null);
            } else {
                sendLiveGamesMessage(liveGame, channelLiveGamesMessagesId.get(neededGame));
            }
        }

        for(LiveGame game : liveGamesMessages.keySet()) {
            String messageId = channelLiveGamesMessagesId.get(game);
            channelLiveGamesMessagesId.remove(game);
            channel.deleteMessageById(messageId).queue();
        }
    }

    /**
     * Отправляет информационное сообщение о активной игре
     * @param liveGame Активная игра
     * @param messageId Id сообщения
     */
    private void sendLiveGamesMessage(LiveGame liveGame, String messageId) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("""
                ```              %first_team%   vs   %second_team%              ```"""
                .replace("%first_team%", liveGame.firstTeamName)
                .replace("%second_team%", liveGame.secondTeamName));
        embedBuilder.setColor(3092790);
        embedBuilder.addField("Формат", liveGame.format, true);
        embedBuilder.addField("Помощник", liveGame.assistantName, true);
        embedBuilder.addField("Стадия игры", liveGame.gameState.getName(), true);

        if(messageId == null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelLiveGamesMessagesId.put(liveGame, message.getId()));
        } else {
            channel.editMessageEmbedsById(messageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Получает список активных игр
     * @return Список активных игр
     */
    public List<LiveGame> getLiveGames() {
        List<LiveGame> games = new ArrayList<>();
        try {
            ResultSet resultSet = MTHD.getInstance().database.getConnection().createStatement().executeQuery("""
                SELECT tlg.id, t1.name firstTeamName, t2.name secondTeamName, tlg.format, u.username as assistantName, tlg.game_state
                FROM team_live_games as tlg
                INNER JOIN teams as t1 ON t1.id = tlg.first_team_id
                INNER JOIN teams as t2 ON t2.id = tlg.second_team_id
                INNER JOIN users as u ON tlg.assistant_id = u.id;""");
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String firstTeamName = resultSet.getString("firstTeamName");
                String secondTeamName = resultSet.getString("secondTeamName");
                String format = resultSet.getString("format");
                String assistantName = resultSet.getString("assistantName");

                GameState gameState = null;
                for(GameState state : GameState.values()) {
                    if(state.getId() == resultSet.getInt("game_state")) {
                        gameState = state;
                    }
                }

                LiveGame liveGame = new LiveGame(id, firstTeamName, secondTeamName, format, assistantName, gameState);
                games.add(liveGame);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return games;
    }
}