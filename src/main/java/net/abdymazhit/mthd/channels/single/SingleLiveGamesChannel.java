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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Канал активных игр Single рейтинга
 *
 * @version   21.10.2021
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
     * @param messageId Id сообщения активных игр
     */
    private void sendLiveGamesMessage(LiveGame liveGame, String messageId) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if(liveGame.gameState.equals(GameState.READY)) {
            embedBuilder.setTitle("""
                    ```           team_XXX   vs   team_XXX           ```""");
        } else {
            embedBuilder.setTitle("""
                    ```           team_%first_team_captain%   vs   team_%second_team_captain%           ```"""
                    .replace("%first_team_captain%", liveGame.firstTeamCaptainName)
                    .replace("%second_team_captain%", liveGame.secondTeamCaptainName));
        }
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
                SELECT id, first_team_captain_id, second_team_captain_id,
                format, assistant_id, game_state FROM single_live_games""");
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String format = resultSet.getString("format");
                int firstTeamCaptainId = resultSet.getInt("first_team_captain_id");
                int secondTeamCaptainId = resultSet.getInt("second_team_captain_id");
                int assistantId = resultSet.getInt("assistant_id");

                String firstTeamCaptainName = MTHD.getInstance().database.getUserName(firstTeamCaptainId);
                String secondTeamCaptainName = MTHD.getInstance().database.getUserName(secondTeamCaptainId);
                String assistantName = MTHD.getInstance().database.getUserName(assistantId);

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