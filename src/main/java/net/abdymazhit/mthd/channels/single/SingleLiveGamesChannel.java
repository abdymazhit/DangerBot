package net.abdymazhit.mthd.channels.single;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.LiveGame;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Канал активных игр Single рейтинга
 *
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class SingleLiveGamesChannel extends Channel {

    /** Сообщения о активных играх */
    public Map<LiveGame, String> channelLiveGamesMessagesId;

    /**
     * Инициализирует канал активных игр
     */
    public SingleLiveGamesChannel() {
        channelLiveGamesMessagesId = new HashMap<>();

        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Single Rating", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Single Rating не существует!");
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
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.MESSAGE_WRITE))
                .queue(textChannel -> {
                    channelId = textChannel.getId();
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

        if(channelId == null) return;

        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал live-games не существует!");
            return;
        }

        for(LiveGame game : liveGamesMessages.keySet()) {
            String messageId = this.channelLiveGamesMessagesId.get(game);
            this.channelLiveGamesMessagesId.remove(game);
            textChannel.deleteMessageById(messageId).queue();
        }
    }

    /**
     * Отправляет информационное сообщение о активной игре
     * @param liveGame Активная игра
     */
    private void sendLiveGamesMessage(LiveGame liveGame, String messageId) {
        if(channelId == null) return;

        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал live-games не существует!");
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("""
            ```              %first_team_captain%   vs   %second_team_captain%              ```"""
                .replace("%first_team_captain%", liveGame.firstTeamCaptainName)
                .replace("%second_team_captain%", liveGame.secondTeamCaptainName));
        embedBuilder.setColor(3092790);
        embedBuilder.addField("Формат", liveGame.format, true);
        embedBuilder.addField("Помощник", liveGame.assistantName, true);
        embedBuilder.addField("Стадия игры", liveGame.gameState.getName(), true);

        if(messageId == null) {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelLiveGamesMessagesId.put(liveGame, message.getId()));
        } else {
            textChannel.editMessageEmbedsById(messageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Получает активные игры
     * @return Активные игры
     */
    public List<LiveGame> getLiveGames() {
        List<LiveGame> games = new ArrayList<>();
        try {
            ResultSet resultSet = MTHD.getInstance().database.getConnection().createStatement().executeQuery("""
                    SELECT slg.id, u1.username firstTeamCaptainName, u2.username secondTeamCaptainName, slg.format, u.username as assistantName, slg.game_state
                    FROM single_live_games as slg
                    INNER JOIN players as p1 ON p1.player_id = slg.first_team_captain_id
                    INNER JOIN players as p2 ON p2.player_id = slg.second_team_captain_id
                    INNER JOIN users as u1 ON u1.id = slg.first_team_captain_id
                    INNER JOIN users as u2 ON u1.id = slg.second_team_captain_id
                    INNER JOIN users as u ON u.id = slg.assistant_id;""");
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String firstTeamCaptainName = resultSet.getString("firstTeamCaptainName");
                String secondTeamCaptainName = resultSet.getString("secondTeamCaptainName");
                String format = resultSet.getString("format");
                String assistantName = resultSet.getString("assistantName");

                GameState gameState = null;
                for(GameState state : GameState.values()) {
                    if(state.getId() == resultSet.getInt("game_state")) {
                        gameState = state;
                    }
                }

                LiveGame liveGame = new LiveGame(id, gameState, firstTeamCaptainName, secondTeamCaptainName, format, assistantName);
                games.add(liveGame);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return games;
    }
}