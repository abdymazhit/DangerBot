package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал выбора игроков на игру
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class PlayersChoiceChannel extends Channel {

    /** Категория игры */
    private final GameCategory gameCategory;

    /** Сообщение о игроках игры */
    public Message channelGamePlayersMessage;

    /**
     * Инициализирует канал выбора игроков на игру
     * @param gameCategory Категория игры
     */
    public PlayersChoiceChannel(GameCategory gameCategory) {
        this.gameCategory = gameCategory;
        try {
            ChannelAction<TextChannel> createAction = createChannel(gameCategory.category, "players-choice", 2);

            Member firstTeamStarter = MTHD.getInstance().guild
                    .retrieveMemberById(gameCategory.game.firstTeamStarterDiscordId).submit().get();

            Member secondTeamStarter = MTHD.getInstance().guild
                    .retrieveMemberById(gameCategory.game.secondTeamStarterDiscordId).submit().get();

            if(firstTeamStarter == null || secondTeamStarter == null) {
                return;
            }

            createAction = createAction.addPermissionOverride(firstTeamStarter,
                    EnumSet.of(Permission.MESSAGE_WRITE), null);
            createAction = createAction.addPermissionOverride(secondTeamStarter,
                    EnumSet.of(Permission.MESSAGE_WRITE), null);

            createAction = createAction.addPermissionOverride(gameCategory.firstTeamRole,
                    EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
            createAction = createAction.addPermissionOverride(gameCategory.secondTeamRole,
                    EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(),
                    null, EnumSet.of(Permission.VIEW_CHANNEL));
            channel = createAction.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        AtomicInteger time = new AtomicInteger(10);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendChannelMessage(time.get());
                if(time.get() <= 0) {
                    cancel();
                    gameCategory.createMapsChoiceChannel();
                }
                time.getAndDecrement();
            }
        }, 0, 1000);

        updateGamePlayersMessage();
    }

    /**
     * Отправляет сообщение канала выбора игроков на игру
     */
    private void sendChannelMessage(int time) {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Первая стадия игры - Выбор игроков на игру");
            embedBuilder.setDescription("Начавшие поиск игры команд (" + gameCategory.firstTeamRole.getAsMention() + " и "
                    + gameCategory.secondTeamRole.getAsMention() + ") должны решить, кто из игроков будет играть в этой игре!\n" +
                    "У вас есть %time% сек. для установки игроков на игру!".replace("%time%", String.valueOf(time)));
            embedBuilder.setColor(0xFF58B9FF);
            embedBuilder.addField("Добавить участника в игру", "`!add <NAME>`", false);
            embedBuilder.addField("Удалить участника из игры", "`!delete <NAME>`", false);

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
     * Отправляет сообщение о игроках игры
     * @param firstTeamPlayers Игроки первой команды
     * @param secondTeamPlayers Игроки второй команды
     */
    private void sendGamePlayersMessage(List<String> firstTeamPlayers, List<String> secondTeamPlayers) {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Список участвующих в игре игроков");
            embedBuilder.setColor(0xFF58B9FF);

            StringBuilder firstTeamPlayersNames = new StringBuilder();
            for(String name : firstTeamPlayers) {
                firstTeamPlayersNames.append(name).append("\n");
            }
            embedBuilder.addField(gameCategory.game.firstTeamName, firstTeamPlayersNames.toString(), true);

            StringBuilder secondTeamPlayersNames = new StringBuilder();
            for(String name : secondTeamPlayers) {
                secondTeamPlayersNames.append(name).append("\n");
            }
            embedBuilder.addField(gameCategory.game.secondTeamName, secondTeamPlayersNames.toString(), true);

            if(channelGamePlayersMessage == null) {
                channelGamePlayersMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            } else {
                channelGamePlayersMessage.editMessageEmbeds(embedBuilder.build()).queue();
            }

            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Обновляет сообщение о игроках игры
     */
    public void updateGamePlayersMessage() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement firstStatement = connection.prepareStatement(
                    "SELECT player_id FROM live_games_players WHERE team_id = ?;");
            firstStatement.setInt(1, gameCategory.game.firstTeamId);
            ResultSet firstResultSet = firstStatement.executeQuery();
            firstStatement.close();

            List<Integer> firstTeamPlayersId = new ArrayList<>();
            while(firstResultSet.next()) {
                firstTeamPlayersId.add(firstResultSet.getInt("player_id"));
            }

            PreparedStatement secondStatement = connection.prepareStatement(
                    "SELECT player_id FROM live_games_players WHERE team_id = ?;");
            secondStatement.setInt(1, gameCategory.game.secondTeamId);
            ResultSet secondResultSet = secondStatement.executeQuery();
            secondStatement.close();

            List<Integer> secondTeamPlayersId = new ArrayList<>();
            while(secondResultSet.next()) {
                secondTeamPlayersId.add(secondResultSet.getInt("player_id"));
            }

            sendGamePlayersMessage(getTeamPlayersNames(firstTeamPlayersId), getTeamPlayersNames(secondTeamPlayersId));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает названий игроков команды
     * @param teamPlayersId Список игроков по id
     * @return Список названий игроков команды
     */
    private List<String> getTeamPlayersNames(List<Integer> teamPlayersId) {
        List<String> teamPlayersNames = new ArrayList<>();
        for(int userId : teamPlayersId) {
            teamPlayersNames.add(MTHD.getInstance().database.getUserName(userId));
        }
        return teamPlayersNames;
    }
}
