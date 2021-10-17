package net.abdymazhit.mthd.channels.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.UserAccount;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал готовности к игре
 *
 * @version   17.10.2021
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

        unreadyList = new ArrayList<>();
        unreadyList.addAll(gameCategoryManager.game.players);
        unreadyList.add(gameCategoryManager.game.firstTeamCaptain.username);
        unreadyList.add(gameCategoryManager.game.secondTeamCaptain.username);

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
            createAction = createAction.addPermissionOverride(firstTeamCaptain, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(secondTeamCaptain, EnumSet.of(Permission.VIEW_CHANNEL), null);
            for(Member member : gameCategoryManager.players) {
                createAction = createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null);
            }

            createAction.addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL), null).queue(textChannel -> {
                channelId = textChannel.getId();

                MessageEmbed messageEmbed = getPrivateMessage(category.getName(), gameCategoryManager.game.assistantAccount.username);
                firstTeamCaptain.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(messageEmbed).queue());
                secondTeamCaptain.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(messageEmbed).queue());
                for(Member member : gameCategoryManager.players) {
                    member.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(messageEmbed).queue());
                }

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Первая стадия игры - Готовность к игре");
                embedBuilder.setColor(3092790);
                embedBuilder.setDescription("""
                            Все игроки должны подтвердить, что они готовы к игре!
                
                            Обратите внимание, как только Вы станете готовым к игре, мы имеем полное право выдать Вам предупреждение, если Вы не сыграете эту игру!
                            
                            Также, игроки, которые не стали готовными к игре - получают блокировку аккаунта на 10 минут.
  
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

                        for(String unreadyName : unreadyList) {
                            UserAccount userAccount = MTHD.getInstance().database.getUserIdAndDiscordId(unreadyName);
                            if(userAccount != null) {
                                MTHD.getInstance().database.banPlayer(null, userAccount.id, userAccount.discordId, 10);
                            }
                        }

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

    /**
     * Получает личное сообщение
     * @param categoryName Название категории
     * @param assistantName Имя помощника
     * @return Личное сообщение
     */
    private MessageEmbed getPrivateMessage(String categoryName, String assistantName) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("У вас найдена игра!");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
            Просим Вас написать команду `/ready` для подтверждения готовности к игре!
                                                                                                                 
            Если Вы не станете готовым к игре в течении `2 минут`, то получите блокировку аккаунта на `10 минут`!""");
        embedBuilder.addField("Id игры", categoryName, true);
        embedBuilder.addField("Помощник", assistantName, true);

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}
