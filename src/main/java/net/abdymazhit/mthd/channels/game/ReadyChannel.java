package net.abdymazhit.mthd.channels.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.Rating;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал готовности к игре
 *
 * @version   13.10.2021
 * @author    Islam Abdymazhit
 */
public class ReadyChannel extends Channel {

    /** Категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Сообщение подготовленных */
    public String channelReadyMessageId;

    /** Таймер обратного отсчета */
    public Timer timer;

    /** Время для подготовки к игре */
    private static final int readyTime = 120;

    /** Оставшееся время для подготовки */
    private final AtomicInteger time;

    /** Неготовые к игре */
    public List<String> unreadyList;

    /** Готовые к игре */
    public List<String> readyList;

    /**
     * Инициализирует канал готовности к игре
     * @param gameCategoryManager Категория игры
     */
    public ReadyChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;
        time = new AtomicInteger(readyTime);
        readyList = new ArrayList<>();

        Category category = MTHD.getInstance().guild.getCategoryById(gameCategoryManager.categoryId);
        if(category == null) {
            System.out.println("Критическая ошибка! Категория Game не существует!");
            return;
        }

        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            unreadyList = new ArrayList<>();
            unreadyList.add(gameCategoryManager.game.firstTeam.name);
            unreadyList.add(gameCategoryManager.game.secondTeam.name);
        } else {
            gameCategoryManager.game.players = new ArrayList<>();
            gameCategoryManager.game.firstTeamPlayers = new ArrayList<>();
            gameCategoryManager.game.firstTeamPlayers.add(gameCategoryManager.game.firstTeamCaptain.username);
            gameCategoryManager.game.secondTeamPlayers = new ArrayList<>();
            gameCategoryManager.game.secondTeamPlayers.add(gameCategoryManager.game.secondTeamCaptain.username);


            unreadyList = new ArrayList<>();

            try {
                PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    SELECT u.username as username FROM users as u
                    INNER JOIN single_live_games_players as slgp ON slgp.live_game_id = ? AND u.id = slgp.player_id;""");
                preparedStatement.setInt(1, gameCategoryManager.game.id);
                ResultSet resultSet = preparedStatement.executeQuery();
                while(resultSet.next()) {
                    String username = resultSet.getString("username");
                    gameCategoryManager.game.players.add(username);
                    unreadyList.add(username);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    SELECT u.username as username FROM users as u
                    INNER JOIN single_live_games as slg ON slg.id = ? AND u.id = slg.first_team_captain_id;""");
                preparedStatement.setInt(1, gameCategoryManager.game.id);
                ResultSet resultSet = preparedStatement.executeQuery();
                while(resultSet.next()) {
                    String username = resultSet.getString("username");
                    unreadyList.add(username);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    SELECT u.username as username FROM users as u
                    INNER JOIN single_live_games as slg ON slg.id = ? AND u.id = slg.second_team_captain_id;""");
                preparedStatement.setInt(1, gameCategoryManager.game.id);
                ResultSet resultSet = preparedStatement.executeQuery();
                while(resultSet.next()) {
                    String username = resultSet.getString("username");
                    unreadyList.add(username);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        List<Member> members = MTHD.getInstance().guild.retrieveMembersByIds(
                gameCategoryManager.game.firstTeamCaptain.discordId,
                gameCategoryManager.game.secondTeamCaptain.discordId).get();
        Member firstTeamCaptain = members.get(0);
        Member secondTeamCaptain = members.get(1);
        if(firstTeamCaptain == null || secondTeamCaptain == null) {
            System.out.println("Критическая ошибка! Не удалось получить роли капитанов первой и второй команды!");
            return;
        }

        MTHD.getInstance().guild.retrieveMemberById(gameCategoryManager.game.assistantAccount.discordId).queue(assistant -> {
            ChannelAction<TextChannel> createAction = category.createTextChannel("ready").setPosition(2).setSlowmode(5)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
                createAction = createAction.addPermissionOverride(firstTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE), null)
                        .addPermissionOverride(secondTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE), null)
                        .addPermissionOverride(gameCategoryManager.firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                        .addPermissionOverride(gameCategoryManager.secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
            } else {
                createAction = createAction.addPermissionOverride(firstTeamCaptain, EnumSet.of(Permission.VIEW_CHANNEL), null)
                        .addPermissionOverride(secondTeamCaptain, EnumSet.of(Permission.VIEW_CHANNEL), null);
                for(Member member : gameCategoryManager.players) {
                    createAction = createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null);
                }
            }

            createAction.addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL), null).queue(textChannel -> {
                channelId = textChannel.getId();
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Первая стадия игры - Готовность к игре");
                embedBuilder.setColor(3092790);
                embedBuilder.setDescription("""
                            Все игроки должны подтвердить, что они готовы к игре!
                
                            Обратите внимание, как только Вы станете готовым к игре, мы имеем полное право выдать Вам предупреждение, если Вы не сыграете эту игру!
  
                            Стать готовым к игре
                            `/ready`""");
                textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                embedBuilder.clear();
                updateReadyMessage(textChannel);
            });
        });
    }

    /**
     * Обновляет сообщение о готовности к игре
     */
    public void updateReadyMessage(TextChannel textChannel) {
        if(timer != null) {
            timer.cancel();
        }

        if(unreadyList.size() == 0) {
            gameCategoryManager.setGameState(GameState.PLAYERS_CHOICE);

            if(channelReadyMessageId == null) {
                textChannel.sendMessageEmbeds(getReadyMessage(-1, true)).queue(message -> new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        channelReadyMessageId = message.getId();
                        if(gameCategoryManager.game.rating.equals(Rating.SINGLE_RATING)) {
                            gameCategoryManager.createPlayersPickChannel();
                        } else {
                            gameCategoryManager.createPlayersChoiceChannel();
                        }
                    }
                }, 7000));
            } else {
                textChannel.editMessageEmbedsById(channelReadyMessageId, getReadyMessage(-1, true)).queue(message -> new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(gameCategoryManager.game.rating.equals(Rating.SINGLE_RATING)) {
                            gameCategoryManager.createPlayersPickChannel();
                        } else {
                            gameCategoryManager.createPlayersChoiceChannel();
                        }
                    }
                }, 7000));
            }
        } else {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(time.get() % 2 == 0) {
                        if(channelReadyMessageId == null) {
                            textChannel.sendMessageEmbeds(getReadyMessage(time.get(), false)).queue(message -> channelReadyMessageId = message.getId());
                        } else {
                            if(time.get() == 0) {
                                textChannel.editMessageEmbedsById(channelReadyMessageId, getReadyMessage(-1, false)).queue();
                            } else {
                                textChannel.editMessageEmbedsById(channelReadyMessageId, getReadyMessage(time.get(), false)).queue();
                            }
                        }
                    }

                    if(time.get() <= 0) {
                        MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.game);
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.categoryId);
                            }
                        }, 7000);
                        cancel();
                    }
                    time.getAndDecrement();
                }
            }, 0, 1000);
        }
    }

    /**
     * Получает сообщение о готовности к игре
     * @param time Время до конца готовности
     * @param isReady Готов к игре
     * @return Сообщение о готовности к игре
     */
    private MessageEmbed getReadyMessage(int time, boolean isReady) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(3092790);

        String description = """
                    Готовых игроков к игре: `%ready_count%`
                    Неготовых игроков к игре: `%unready_count%`
                      
                    Оставшееся время для подготовки: `%time% сек.`""";
        description = description.replace("%ready_count%", String.valueOf(readyList.size()));
        description = description.replace("%unready_count%", String.valueOf(unreadyList.size()));

        if(time >= 0) {
            embedBuilder.setTitle("Готовность к игре");
            description = description.replace("%time%", String.valueOf(time));
        } else {
            if(isReady) {
                embedBuilder.setTitle("Начало игры...");
            } else {
                embedBuilder.setTitle("Отмена игры...");
            }
            description = description.replace("%time%", String.valueOf(0));
        }
        embedBuilder.setDescription(description);

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}
