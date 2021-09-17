package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал игры
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public class GameChannel extends Channel {

    /** Сообщение отмены игры */
    public String channelGameCancelMessageId;

    /** Таймер игры */
    public Timer timer;

    /**
     * Инициализирует канал игры
     * @param gameCategory Категория игры
     */
    public GameChannel(GameCategory gameCategory) {
        Category category = MTHD.getInstance().guild.getCategoryById(gameCategory.categoryId);
        if(category != null) {
            List<Member> members = MTHD.getInstance().guild.retrieveMembersByIds(gameCategory.game.firstTeamStarterDiscordId,
                    gameCategory.game.secondTeamStarterDiscordId).get();

            Member firstTeamStarter = members.get(0);
            Member secondTeamStarter = members.get(1);

            if(firstTeamStarter == null || secondTeamStarter == null) {
                return;
            }

            ChannelAction<TextChannel> createAction = category.createTextChannel("game").setPosition(2);
            createAction = createAction.addPermissionOverride(gameCategory.firstTeamRole,
                    EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
            createAction = createAction.addPermissionOverride(gameCategory.secondTeamRole,
                    EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(),
                    null, EnumSet.of(Permission.VIEW_CHANNEL));
            createAction.queue(textChannel -> {
                channelId = textChannel.getId();

                MTHD.getInstance().guild.retrieveMemberById(gameCategory.game.assistantDiscordId).queue(member ->
                        sendChannelMessage(gameCategory, member));
            });
        }
    }

    public GameChannel(GameCategory gameCategory, boolean isRebooting) {
        Category category = MTHD.getInstance().guild.getCategoryById(gameCategory.categoryId);
        if(category != null) {
            for(TextChannel textChannel : category.getTextChannels()) {
                channelId = textChannel.getId();
            }
        }
    }

    /**
     * Отправляет сообщение канала
     * @param gameCategory Категория игры
     * @param assistant Помощник
     */
    private void sendChannelMessage(GameCategory gameCategory, Member assistant) {
        AtomicInteger time = new AtomicInteger(600);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
                if(channel == null) {
                    cancel();
                } else {
                    if(time.get() <= 0) {
                        channel.sendMessage("Вы не успели начать игру в течении 5 минут! Игра отменяется...")
                                .queue(message -> channelGameCancelMessageId = message.getId());

                        MTHD.getInstance().gameManager.deleteGame(gameCategory.game);
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Category category = MTHD.getInstance().guild.getCategoryById(gameCategory.categoryId);
                                if(category != null) {
                                    MTHD.getInstance().gameManager.deleteGame(category.getId());
                                }
                            }
                        }, 7000);
                    }

                    if(time.get() == 600) {
                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        embedBuilder.setTitle("Инструкция для помощника");

                        StringBuilder firstTeamInviteStrings = new StringBuilder();
                        for(String username : gameCategory.game.firstTeamPlayers) {
                            firstTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
                        }

                        StringBuilder secondTeamInviteStrings = new StringBuilder();
                        for(String username : gameCategory.game.secondTeamPlayers) {
                            secondTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
                        }

                        embedBuilder.setDescription("У вас (" + assistant.getAsMention() + ") есть 10 минут, чтобы начать игру!\n\n" +
                                "Оставшееся время для для начала игры: " + time.get() + "\n" +
                                "Противостояние: " + gameCategory.firstTeamRole.getAsMention() + " vs " + gameCategory.secondTeamRole.getAsMention() + "\n" +
                                "Игра: BedWars Hard\n" +
                                "\n" +
                                "Формат игры: **" + gameCategory.game.format + "**\n" +
                                "Название карты: **" + gameCategory.game.gameMap.getName() + "**\n" +
                                "\n" +
                                "Настройки сервера:\n" +
                                "`/game flag allow-warp false`\n" +
                                "`/game flag kick-on-lose true`\n" +
                                "`/game flag final-dm true`\n" +
                                "\n" +
                                "Команда для создания игры: `/game create`\n\n" +
                                "Команды для приглашения игроков " + gameCategory.firstTeamRole.getAsMention() + ":\n" +
                                firstTeamInviteStrings +
                                "\n" +
                                "Команды для приглашения игроков " + gameCategory.secondTeamRole.getAsMention() + ":\n" +
                                secondTeamInviteStrings
                        );

                        embedBuilder.setColor(0xFF58B9FF);
                        embedBuilder.addField("Начать игру", "Обратите внимание, вы должны ввести команду только после начала игры в самом VimeWorld!" +
                                " Введите `!start` для начала игры", false);

                        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                        embedBuilder.clear();
                    }

                    if(time.get() % 2 == 0) {
                        if(channelMessageId != null) {
                            EmbedBuilder embedBuilder = new EmbedBuilder();
                            embedBuilder.setTitle("Инструкция для помощника");

                            StringBuilder firstTeamInviteStrings = new StringBuilder();
                            for(String username : gameCategory.game.firstTeamPlayers) {
                                firstTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
                            }

                            StringBuilder secondTeamInviteStrings = new StringBuilder();
                            for(String username : gameCategory.game.secondTeamPlayers) {
                                secondTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
                            }

                            embedBuilder.setDescription("У вас (" + assistant.getAsMention() + ") есть 10 минут для создания игры!\n\n" +
                                    "Оставшееся время для для создания карты: " + time.get() + "\n" +
                                    "Противостояние: " + gameCategory.firstTeamRole.getAsMention() + " vs " + gameCategory.secondTeamRole.getAsMention() + "\n" +
                                    "Игра: BedWars Hard\n" +
                                    "\n" +
                                    "Формат игры: **" + gameCategory.game.format + "**\n" +
                                    "Название карты: **" + gameCategory.game.gameMap.getName() + "**\n" +
                                    "\n" +
                                    "Настройки сервера:\n" +
                                    "`/game flag allow-warp false`\n" +
                                    "`/game flag kick-on-lose true`\n" +
                                    "`/game flag final-dm true`\n" +
                                    "\n" +
                                    "Команда для создания игры: `/game create`\n\n" +
                                    "Команды для приглашения игроков " + gameCategory.firstTeamRole.getAsMention() + ":\n" +
                                    firstTeamInviteStrings +
                                    "\n" +
                                    "Команды для приглашения игроков " + gameCategory.secondTeamRole.getAsMention() + ":\n" +
                                    secondTeamInviteStrings
                            );

                            embedBuilder.setColor(0xFF58B9FF);
                            embedBuilder.addField("Начать игру", "Обратите внимание, вы должны ввести команду только после начала игры в самом VimeWorld!" +
                                    " Введите `!start` для начала игры", false);

                            channel.editMessageEmbedsById(channelMessageId, embedBuilder.build()).queue();
                            embedBuilder.clear();
                        }
                    }
                }

                time.getAndDecrement();
            }
        }, 0, 1000);
    }
}
