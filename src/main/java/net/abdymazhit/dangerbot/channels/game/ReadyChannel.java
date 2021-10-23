package net.abdymazhit.dangerbot.channels.game;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Channel;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.GameState;
import net.abdymazhit.dangerbot.managers.GameCategoryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал готовности к игре
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class ReadyChannel extends Channel {

    /** Менеджер категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Информационное сообщение готовности к игре игроков */
    public Message channelReadyMessage;

    /** Время для подготовки к игре игроков */
    private static final int readyTime = 120;

    /** Оставшееся время для подготовки */
    private final AtomicInteger time;

    /** Таймер обратного отсчета */
    public Timer timer;

    /** Список имен неготовых к игре игроков */
    public List<String> unreadyList;

    /** Список имен готовых к игре игроков */
    public List<String> readyList;

    /**
     * Инициализирует канал готовности к игре
     * @param gameCategoryManager Менеджер категория игры
     */
    public ReadyChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;
        time = new AtomicInteger(readyTime);

        unreadyList = new ArrayList<>();
        unreadyList.add(gameCategoryManager.game.firstTeamInfo.captain.username);
        unreadyList.add(gameCategoryManager.game.secondTeamInfo.captain.username);
        for(UserAccount playerAccount : gameCategoryManager.game.playersAccounts) {
            unreadyList.add(playerAccount.username);
        }
        readyList = new ArrayList<>();

        ChannelAction<TextChannel> createAction = gameCategoryManager.category.createTextChannel("ready").setPosition(2).setSlowmode(5)
                .addPermissionOverride(DangerBot.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(gameCategoryManager.game.firstTeamInfo.captain.member, EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(gameCategoryManager.game.secondTeamInfo.captain.member, EnumSet.of(Permission.VIEW_CHANNEL), null);
        for(UserAccount playerAccount : gameCategoryManager.game.playersAccounts) {
            createAction = createAction.addPermissionOverride(playerAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), null);
        }

        createAction.addPermissionOverride(gameCategoryManager.game.assistantAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue(textChannel -> {
            channel = textChannel;

            // Отправляет игрокам личное сообщение о найденности игры
            MessageEmbed messageEmbed = getPrivateMessage();
            gameCategoryManager.game.firstTeamInfo.captain.member.getUser().openPrivateChannel().queue(privateChannel ->
                    privateChannel.sendMessageEmbeds(messageEmbed).queue());
            gameCategoryManager.game.secondTeamInfo.captain.member.getUser().openPrivateChannel().queue(privateChannel ->
                    privateChannel.sendMessageEmbeds(messageEmbed).queue());
            for(UserAccount userAccount : gameCategoryManager.game.playersAccounts) {
                userAccount.member.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(messageEmbed).queue());
            }

            // Отправляет главное сообщение канала
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Первая стадия игры - Готовность к игре");
            embedBuilder.setColor(3092790);
            embedBuilder.setDescription("""
                    Все игроки должны подтвердить, что они готовы к игре!
                
                    Обратите внимание, как только Вы станете готовым к игре, мы имеем полное право выдать Вам предупреждение, если Вы не сыграете эту игру!
                            
                    Также, игроки, которые не стали готовными к игре - получают блокировку аккаунта на 10 минут.
  
                    Стать готовым к игре
                    `/ready`""");
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
            embedBuilder.clear();

            updateReadyMessage();
        });
    }

    /**
     * Обновляет сообщение о готовности к игре игроков
     */
    public void updateReadyMessage() {
        if(timer != null) {
            timer.cancel();
        }

        if(unreadyList.size() == 0) {
            gameCategoryManager.setGameState(GameState.PLAYERS_CHOICE);
            channel.editMessageEmbedsById(channelReadyMessage.getId(), getReadyMessage(-1, true)).queue(message -> new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    gameCategoryManager.createPlayersPickChannel();
                }
            }, 7000));
        } else {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(time.get() % 2 == 0) {
                        if(channelReadyMessage == null) {
                            channel.sendMessageEmbeds(getReadyMessage(time.get(), false)).queue(message -> channelReadyMessage = message);
                        } else {
                            if(time.get() != 0) {
                                channel.editMessageEmbedsById(channelReadyMessage.getId(), getReadyMessage(time.get(), false)).queue();
                            } else {
                                channel.editMessageEmbedsById(channelReadyMessage.getId(), getReadyMessage(-1, false)).queue();
                            }
                        }
                    }

                    if(time.get() == 0) {
                        DangerBot.getInstance().gameManager.deleteGame(gameCategoryManager.game);

                        for(String playerName : unreadyList) {
                            int userId = DangerBot.getInstance().database.getUserIdByUsername(playerName);
                            if(userId > 0) {
                                DangerBot.getInstance().database.banPlayer(null, userId, 10);
                            }
                        }

                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                DangerBot.getInstance().gameManager.deleteGame(gameCategoryManager.category.getId());
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
     * Получает сообщение о готовности к игре игроков
     * @param time Время до конца готовности
     * @param isReady Значение готовности к игре
     * @return Сообщение о готовности к игре игроков
     */
    private MessageEmbed getReadyMessage(int time, boolean isReady) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(3092790);

        String description = """
                Готовых к игре игроков: `%ready_count%`
                Неготовых к игре игроков: `%unready_count%`
                      
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

    /**
     * Получает отправляемое личное сообщение
     * @return Отправляемое личное сообщение
     */
    private MessageEmbed getPrivateMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("У вас найдена игра!");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
                Просим Вас написать команду `/ready` для подтверждения готовности к игре!
                                                                                                                 
                Если Вы не станете готовым к игре в течении `2 минут`, то получите блокировку аккаунта на `10 минут`!""");
        embedBuilder.addField("Id игры", gameCategoryManager.category.getName(), true);
        embedBuilder.addField("Помощник", gameCategoryManager.game.assistantAccount.username, true);

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}
