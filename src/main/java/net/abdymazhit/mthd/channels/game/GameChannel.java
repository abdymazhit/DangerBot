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
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class GameChannel extends Channel {

    /** Категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Сообщение отмены игры */
    public String channelGameCancelMessageId;

    /** Таймер игры */
    public Timer timer;

    /** Время создания игры */
    private static final int gameCreationTime = 600;

    /**
     * Инициализирует канал игры
     * @param gameCategoryManager Категория игры
     */
    public GameChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;

        Category category = MTHD.getInstance().guild.getCategoryById(gameCategoryManager.categoryId);
        if(category == null) {
            System.out.println("Критическая ошибка! Категория Game не существует!");
            return;
        }

        List<Member> members = MTHD.getInstance().guild.retrieveMembersByIds(gameCategoryManager.game.firstTeamCaptain.discordId,
            gameCategoryManager.game.secondTeamCaptain.discordId).get();
        Member firstTeamCaptain = members.get(0);
        Member secondTeamCaptain = members.get(1);
        if(firstTeamCaptain == null || secondTeamCaptain == null) {
            System.out.println("Критическая ошибка! Не удалось получить роли начавших игру первой и второй команды!");
            return;
        }

        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            MTHD.getInstance().guild.retrieveMemberById(gameCategoryManager.game.assistantAccount.discordId).queue(
                    assistant -> category.createTextChannel("game").setPosition(2)
                            .addPermissionOverride(gameCategoryManager.firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                            .addPermissionOverride(gameCategoryManager.secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                            .addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE), null)
                            .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                            .queue(textChannel -> {
                                channelId = textChannel.getId();
                                if(gameCategoryManager.game.gameState.equals(GameState.GAME_CREATION)) {
                                    sendChannelMessage(gameCategoryManager, textChannel, assistant);
                                } else if(gameCategoryManager.game.gameState.equals(GameState.GAME)) {
                                    sendGameStartMessage();
                                }
                            }));
        } else {
            MTHD.getInstance().guild.retrieveMemberById(gameCategoryManager.game.assistantAccount.discordId).queue(assistant -> {
                ChannelAction<TextChannel> createAction = category.createTextChannel("game").setPosition(2)
                        .addPermissionOverride(firstTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                        .addPermissionOverride(secondTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                        .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                        .addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL), null);

                for(Member member : gameCategoryManager.players) {
                    createAction = createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
                }

                createAction.queue(textChannel -> {
                    channelId = textChannel.getId();
                    if(gameCategoryManager.game.gameState.equals(GameState.GAME_CREATION)) {
                        sendChannelMessage(gameCategoryManager, textChannel, assistant);
                    } else if(gameCategoryManager.game.gameState.equals(GameState.GAME)) {
                        sendGameStartMessage();
                    }
                });
            });
        }
    }

    /**
     * Отправляет сообщение канала
     * @param gameCategoryManager Категория игры
     * @param assistant Помощник
     */
    private void sendChannelMessage(GameCategoryManager gameCategoryManager, TextChannel textChannel, Member assistant) {
        AtomicInteger time = new AtomicInteger(gameCreationTime);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(time.get() <= 0) {
                    textChannel.sendMessage("Вы не успели начать игру в течении 10 минут! Игра отменяется...")
                        .queue(message -> channelGameCancelMessageId = message.getId());
                    MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.game);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Category category = MTHD.getInstance().guild.getCategoryById(gameCategoryManager.categoryId);
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
                    for(String username : gameCategoryManager.game.firstTeamPlayers) {
                        firstTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
                    }

                    StringBuilder secondTeamInviteStrings = new StringBuilder();
                    for(String username : gameCategoryManager.game.secondTeamPlayers) {
                        secondTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
                    }

                    if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
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
                                .replace("%format%", gameCategoryManager.game.format)
                                .replace("%map_name%", gameCategoryManager.game.gameMap.getName())
                                .replace("%first_team%", gameCategoryManager.firstTeamRole.getAsMention())
                                .replace("%second_team%", gameCategoryManager.secondTeamRole.getAsMention())
                                .replace("%first_team_invites%", firstTeamInviteStrings)
                                .replace("%second_team_invites%", secondTeamInviteStrings));
                    } else {
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
                                .replace("%format%", gameCategoryManager.game.format)
                                .replace("%map_name%", gameCategoryManager.game.gameMap.getName())
                                .replace("%first_team%", "team_" + gameCategoryManager.game.firstTeamCaptain.username)
                                .replace("%second_team%", "team_" + gameCategoryManager.game.secondTeamCaptain.username)
                                .replace("%first_team_invites%", firstTeamInviteStrings)
                                .replace("%second_team_invites%", secondTeamInviteStrings));
                    }

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

    /**
     * Отправляет сообщение о начале игры
     */
    public void sendGameStartMessage() {
        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал game не существует!");
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();

        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            embedBuilder.setTitle(gameCategoryManager.game.firstTeam.name + " vs " + gameCategoryManager.game.secondTeam.name);
        } else {
            embedBuilder.setTitle("team_" + gameCategoryManager.game.firstTeamCaptain.username + " vs team_" + gameCategoryManager.game.secondTeamCaptain.username);
        }
        embedBuilder.setColor(3092790);

        StringBuilder firstTeamPlayersStrings = new StringBuilder();
        for(String username : gameCategoryManager.game.firstTeamPlayers) {
            firstTeamPlayersStrings.append("`").append(username).append("`").append("\n");
        }

        StringBuilder secondTeamPlayersStrings = new StringBuilder();
        for(String username : gameCategoryManager.game.secondTeamPlayers) {
            secondTeamPlayersStrings.append("`").append(username).append("`").append("\n");
        }

        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            embedBuilder.addField("Команда " + gameCategoryManager.game.firstTeam.name, String.valueOf(firstTeamPlayersStrings), true);
            embedBuilder.addField("Команда " + gameCategoryManager.game.secondTeam.name, String.valueOf(secondTeamPlayersStrings), true);
        } else {
            embedBuilder.addField("Команда team_" + gameCategoryManager.game.firstTeamCaptain.username, String.valueOf(firstTeamPlayersStrings), true);
            embedBuilder.addField("Команда team_" + gameCategoryManager.game.secondTeamCaptain.username, String.valueOf(secondTeamPlayersStrings), true);
        }

        embedBuilder.addField("Отмена игры", "Данная команда доступна только для администрации. " +
                                             "Для отмены игры введите `!cancel`", false);
        embedBuilder.addField("Ручная установка id матча", "Если случилась какая-та ошибка и боту не удалось найти id матча администратор " +
                                                           "должен вручную установить id матча. Для ручной установки id матча введите `!finish <ID>`", false);
        embedBuilder.addField("Помощник игры", gameCategoryManager.game.assistantAccount.username, false);

        if(channelMessageId == null) {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
        } else {
            textChannel.editMessageEmbedsById(gameCategoryManager.gameChannel.channelMessageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }
}
