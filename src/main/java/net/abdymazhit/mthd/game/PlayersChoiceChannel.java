package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.GameState;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал выбора игроков на игру
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public class PlayersChoiceChannel extends Channel {

    /** Категория игры */
    private final GameCategory gameCategory;

    /** Сообщение о игроках игры */
    public String channelGamePlayersMessageId;

    /** Сообщение отмены игры */
    public String channelGameCancelMessageId;

    /**
     * Инициализирует канал выбора игроков на игру
     * @param gameCategory Категория игры
     */
    public PlayersChoiceChannel(GameCategory gameCategory) {
        this.gameCategory = gameCategory;

        Category category = MTHD.getInstance().guild.getCategoryById(gameCategory.categoryId);
        if(category == null) return;

        List<Member> members = MTHD.getInstance().guild.retrieveMembersByIds(gameCategory.game.firstTeamStarterDiscordId,
                gameCategory.game.secondTeamStarterDiscordId).get();

        Member firstTeamStarter = members.get(0);
        Member secondTeamStarter = members.get(1);

        if(firstTeamStarter == null || secondTeamStarter == null) {
            return;
        }

        ChannelAction<TextChannel> createAction = category.createTextChannel("players-choice").setPosition(2);
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

        createAction.queue(textChannel -> {
            channelId = textChannel.getId();

            // Изменить
            AtomicInteger time = new AtomicInteger(120);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if(time.get() <= 0) {
                        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
                        if(channel != null) {
                            boolean isCancelling = false;
                            if(gameCategory.game.format.equals("4x2")) {
                                if(gameCategory.game.firstTeamPlayers.size() < 4 ||
                                        gameCategory.game.secondTeamPlayers.size() < 4) {
                                    isCancelling = true;
                                    channel.sendMessage("Недостаточно игроков для начала игры! Игра отменяется...")
                                            .queue(message -> channelGameCancelMessageId = message.getId());

                                    MTHD.getInstance().gameManager.deleteGame(gameCategory.game);
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            Category category1 = MTHD.getInstance().guild.getCategoryById(gameCategory.categoryId);
                                            if(category1 != null) {
                                                MTHD.getInstance().gameManager.deleteGame(category1.getId());
                                            }
                                        }
                                    }, 7000);
                                }
                            } else if(gameCategory.game.format.equals("6x2")) {
                                if(gameCategory.game.firstTeamPlayers.size() < 6 ||
                                        gameCategory.game.secondTeamPlayers.size() < 6) {
                                    isCancelling = true;
                                    channel.sendMessage("Недостаточно игроков для начала игры! Игра отменяется...")
                                            .queue(message -> channelGameCancelMessageId = message.getId());

                                    MTHD.getInstance().gameManager.deleteGame(gameCategory.game);
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            Category category1 = MTHD.getInstance().guild.getCategoryById(gameCategory.categoryId);
                                            if(category1 != null) {
                                                MTHD.getInstance().gameManager.deleteGame(category1.getId());
                                            }
                                        }
                                    }, 7000);
                                }
                            }

                            cancel();
                            if (!isCancelling) {
                                channel.sendMessage("Игроки успешно выбраны для игры. Переход к выбору карт...").queue();
                                gameCategory.setGameState(GameState.MAP_CHOICE);
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        gameCategory.createMapsChoiceChannel();
                                    }
                                }, 7000);
                            }
                        }
                    }

                    if(time.get() % 2 == 0) {
                        sendChannelMessage(time.get());
                    }

                    time.getAndDecrement();
                }
            }, 0, 1000);

            updateGamePlayersMessage();
        });
    }

    /**
     * Отправляет сообщение канала выбора игроков на игру
     */
    private void sendChannelMessage(int time) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Первая стадия игры - Выбор игроков на игру");
        embedBuilder.setDescription("Начавшие поиск игры команд (" + gameCategory.firstTeamRole.getAsMention() + " и "
                + gameCategory.secondTeamRole.getAsMention() + ") должны решить, кто из игроков будет играть в этой игре!\n" +
                "У вас есть %time% сек. для установки игроков на игру!".replace("%time%", String.valueOf(time)));
        embedBuilder.setColor(0xFF58B9FF);
        embedBuilder.addField("Добавить участника в игру", "`!add <NAME>`", false);
        embedBuilder.addField("Удалить участника из игры", "`!delete <NAME>`", false);

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
     * Отправляет сообщение о игроках игры
     */
    private void sendGamePlayersMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Список участвующих в игре игроков");
        embedBuilder.setColor(0xFF58B9FF);

        StringBuilder firstTeamPlayersNames = new StringBuilder();
        for(String name : gameCategory.game.firstTeamPlayers) {
            firstTeamPlayersNames.append(name).append("\n");
        }
        embedBuilder.addField(gameCategory.game.firstTeamName, firstTeamPlayersNames.toString(), true);

        StringBuilder secondTeamPlayersNames = new StringBuilder();
        for(String name : gameCategory.game.secondTeamPlayers) {
            secondTeamPlayersNames.append(name).append("\n");
        }
        embedBuilder.addField(gameCategory.game.secondTeamName, secondTeamPlayersNames.toString(), true);

        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(channel != null) {
            if(channelGamePlayersMessageId == null) {
                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelGamePlayersMessageId = message.getId());
            } else {
                channel.editMessageEmbedsById(channelGamePlayersMessageId, embedBuilder.build()).queue();
            }
        }

        embedBuilder.clear();
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

            gameCategory.game.firstTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(firstTeamPlayersId);
            gameCategory.game.secondTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(secondTeamPlayersId);

            sendGamePlayersMessage();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
