package net.abdymazhit.mthd.game;

import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Канал игры
 *
 * @version   21.09.2021
 * @author    Islam Abdymazhit
 */
public class GameChannel extends Channel {

    /** Сообщение отмены игры */
    public String channelGameCancelMessageId;

    /** Таймер игры */
    public Timer timer;

    /** Время создания игры */
    private static final int gameCreationTime = 600;

    /**
     * Инициализирует канал игры
     * @param gameCategory Категория игры
     * @param isRebooting Значение, является ли инициализация по причине перезагрузки
     */
    public GameChannel(GameCategory gameCategory, boolean isRebooting) {
        Category category = MTHD.getInstance().guild.getCategoryById(gameCategory.categoryId);
        if(category == null) {
            System.out.println("Критическая ошибка! Категория Game не существует!");
            return;
        }

        if(isRebooting) {
            for(TextChannel textChannel : category.getTextChannels()) {
                channelId = textChannel.getId();
            }
            return;
        }

        List<Member> members = MTHD.getInstance().guild.retrieveMembersByIds(gameCategory.game.firstTeamStarterDiscordId,
            gameCategory.game.secondTeamStarterDiscordId).get();
        Member firstTeamStarter = members.get(0);
        Member secondTeamStarter = members.get(1);
        if(firstTeamStarter == null || secondTeamStarter == null) {
            System.out.println("Критическая ошибка! Не удалось получить роли начавших игру первой и второй команды!");
            return;
        }

        MTHD.getInstance().guild.retrieveMemberById(gameCategory.game.assistantDiscordId).queue(
            assistant -> category.createTextChannel("game").setPosition(2)
                .addPermissionOverride(gameCategory.firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(gameCategory.secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE), null)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    channelId = textChannel.getId();
                    sendChannelMessage(gameCategory, textChannel, assistant);
                }));
    }

    /**
     * Отправляет сообщение канала
     * @param gameCategory Категория игры
     * @param assistant Помощник
     */
    private void sendChannelMessage(GameCategory gameCategory, TextChannel textChannel, Member assistant) {
        AtomicInteger time = new AtomicInteger(gameCreationTime);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(time.get() <= 0) {
                    textChannel.sendMessage("Вы не успели начать игру в течении 5 минут! Игра отменяется...")
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
                    cancel();
                }

                if(time.get() % 2 == 0) {
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle("Инструкция для помощника");
                    embedBuilder.setColor(3092790);

                    StringBuilder firstTeamInviteStrings = new StringBuilder();
                    for(String username : gameCategory.game.firstTeamPlayers) {
                        firstTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
                    }

                    StringBuilder secondTeamInviteStrings = new StringBuilder();
                    for(String username : gameCategory.game.secondTeamPlayers) {
                        secondTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
                    }

                    embedBuilder.setDescription("""
                            У вас (%assistant%) есть 10 минут для создания игры!
                            Оставшееся время для для создания карты: `%time% сек.`
                            
                            Игра: BedWars Hard
                            Формат игры: %format%
                            Название карты: %map_name%
                            
                            Настройки сервера:
                            `/game flag allow-warp false`
                            `/game flag kick-on-lose true`
                            `/game flag final-dm true`
                            
                            Команды для приглашения игроков %first_team%
                            %first_team_invites%
                            Команды для приглашения игроков %second_team%
                            %second_team_invites%
                            """
                        .replace("%time%", String.valueOf(time))
                        .replace("%assistant%", assistant.getAsMention())
                        .replace("%format%", gameCategory.game.format)
                        .replace("%map_name%", gameCategory.game.gameMap.getName())
                        .replace("%first_team%", gameCategory.firstTeamRole.getAsMention())
                        .replace("%second_team%", gameCategory.secondTeamRole.getAsMention())
                        .replace("%first_team_invites%", firstTeamInviteStrings)
                        .replace("%second_team_invites%", secondTeamInviteStrings));

                    embedBuilder.addField("Начать игру", """
                            Обратите внимание, вы должны ввести команду только после начала игры в самом VimeWorld! Введите `!start` для начала игры""", false);

                    if(channelMessageId == null) {
                        textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                    } else {
                        textChannel.editMessageEmbedsById(channelMessageId, embedBuilder.build()).queue();
                    }
                    embedBuilder.clear();
                }
                time.getAndDecrement();
            }
        }, 0, 1000);
    }
}
