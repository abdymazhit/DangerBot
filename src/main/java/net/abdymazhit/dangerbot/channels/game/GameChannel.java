package net.abdymazhit.dangerbot.channels.game;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Channel;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.GameState;
import net.abdymazhit.dangerbot.enums.Rating;
import net.abdymazhit.dangerbot.managers.GameCategoryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал игры
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class GameChannel extends Channel {

    /** Категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Время до начала игры */
    private static final int gameStartTime = 120;

    /** Таймер обратного отсчета */
    public Timer timer;

    /**
     * Инициализирует канал игры
     * @param gameCategoryManager Категория игры
     */
    public GameChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;

        ChannelAction<TextChannel> createAction = gameCategoryManager.category.createTextChannel("game").setPosition(2).setSlowmode(5)
                .addPermissionOverride(DangerBot.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));

        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            createAction.addPermissionOverride(gameCategoryManager.game.firstTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                    .addPermissionOverride(gameCategoryManager.game.secondTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                    .addPermissionOverride(gameCategoryManager.game.assistantAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue(textChannel -> {
                channel = textChannel;

                if(gameCategoryManager.game.gameState.equals(GameState.GAME_CREATION)) {
                    sendGameCreationMessage();
                } else if(gameCategoryManager.game.gameState.equals(GameState.GAME)) {
                    sendGameStartMessage();
                }
            });
        } else {
            createAction = createAction.addPermissionOverride(gameCategoryManager.game.firstTeamInfo.captain.member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                    .addPermissionOverride(gameCategoryManager.game.secondTeamInfo.captain.member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
            for(UserAccount userAccount : gameCategoryManager.game.playersAccounts) {
                createAction = createAction.addPermissionOverride(userAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
            }
            createAction.addPermissionOverride(gameCategoryManager.game.assistantAccount.member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE), null).queue(textChannel -> {
                channel = textChannel;

                if(gameCategoryManager.game.gameState.equals(GameState.GAME_CREATION)) {
                    sendGameCreationMessage();
                } else if(gameCategoryManager.game.gameState.equals(GameState.GAME)) {
                    sendGameStartMessage();
                }
            });
        }
    }

    /**
     * Отправляет сообщение канала
     */
    private void sendGameCreationMessage() {
        AtomicInteger time = new AtomicInteger(gameStartTime);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(time.get() % 2 == 0) {
                    if(channelMessage == null) {
                        channel.sendMessageEmbeds(getGameCreationMessage(time.get())).queue(message -> channelMessage = message);
                    } else {
                        channel.editMessageEmbedsById(channelMessage.getId(), getGameCreationMessage(time.get())).queue();
                    }
                }

                if(time.get() == 0) {
                    gameCategoryManager.setGameState(GameState.GAME);
                    DangerBot.getInstance().liveGamesManager.addLiveGame(gameCategoryManager.game);
                    sendGameStartMessage();
                    cancel();
                }
                time.getAndDecrement();
            }
        }, 0, 1000);
    }

    /**
     * Отправляет сообщение начала игры
     */
    public void sendGameStartMessage() {
        if(channelMessage == null) {
            channel.sendMessageEmbeds(getGameStartMessage()).queue(message -> channelMessage = message);
        } else {
            channel.editMessageEmbedsById(channelMessage.getId(), getGameStartMessage()).queue();
        }
    }

    /**
     * Получает сообщение создания игры
     * @param time Оставшееся время до начала игры
     * @return Сообщение создания игры
     */
    private MessageEmbed getGameCreationMessage(int time) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Инструкция для помощника");
        embedBuilder.setColor(3092790);

        StringBuilder firstTeamInviteStrings = new StringBuilder();
        for(UserAccount userAccount : gameCategoryManager.game.firstTeamInfo.members) {
            firstTeamInviteStrings.append("`/game summon ").append(userAccount.username).append("`").append("\n");
        }

        StringBuilder secondTeamInviteStrings = new StringBuilder();
        for(UserAccount userAccount : gameCategoryManager.game.secondTeamInfo.members) {
            secondTeamInviteStrings.append("`/game summon ").append(userAccount.username).append("`").append("\n");
        }

        String description = """
                Помощник (%assistantInfo%) должен создать игру!
                Игра перейдет в стадию игры через `%time% сек.`
                            
                Игра: BedWars Hard
                Формат игры: %format%
                Название карты: `%map_name%`
                            
                Настройки сервера:
                `/game flag allow-warp false`
                `/game flag kick-on-lose true`
                `/game flag final-dm true`
                            
                Команды для приглашения игроков `%first_team%`
                %first_team_invites%
                Команды для приглашения игроков `%second_team%`
                %second_team_invites%"""
                .replace("%assistantInfo%", gameCategoryManager.game.assistantAccount.member.getAsMention())
                .replace("%format%", gameCategoryManager.game.format)
                .replace("%map_name%", gameCategoryManager.game.gameMap.getName())
                .replace("%first_team_invites%", firstTeamInviteStrings)
                .replace("%second_team_invites%", secondTeamInviteStrings);
        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            description = description.replace("%time%", String.valueOf(time))
                    .replace("%first_team%", gameCategoryManager.game.firstTeamInfo.role.getAsMention())
                    .replace("%second_team%", gameCategoryManager.game.secondTeamInfo.role.getAsMention());
        } else {
            description = description.replace("%time%", String.valueOf(time))
                    .replace("%first_team%", "team_" + gameCategoryManager.game.firstTeamInfo.captain.username)
                    .replace("%second_team%", "team_" + gameCategoryManager.game.secondTeamInfo.captain.username);
        }
        embedBuilder.setDescription(description);

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }

    /**
     * Получает сообщение начала игры
     * @return Сообщение начала игры
     */
    private MessageEmbed getGameStartMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Игра успешно началась!");
        embedBuilder.setColor(3092790);

        StringBuilder firstTeamInviteStrings = new StringBuilder();
        for(UserAccount userAccount : gameCategoryManager.game.firstTeamInfo.members) {
            firstTeamInviteStrings.append("`/game summon ").append(userAccount.username).append("`\n");
        }

        StringBuilder secondTeamInviteStrings = new StringBuilder();
        for(UserAccount userAccount : gameCategoryManager.game.secondTeamInfo.members) {
            secondTeamInviteStrings.append("`/game summon ").append(userAccount.username).append("`\n");
        }

        String description = """
                Помощник игры: %assistantInfo%
                            
                Игра: BedWars Hard
                Формат игры: %format%
                Название карты: `%map_name%`
                            
                Настройки сервера:
                `/game flag allow-warp false`
                `/game flag kick-on-lose true`
                `/game flag final-dm true`
                            
                Команды для приглашения игроков `%first_team%`
                %first_team_invites%
                Команды для приглашения игроков `%second_team%`
                %second_team_invites%"""
                .replace("%assistantInfo%", gameCategoryManager.game.assistantAccount.member.getAsMention())
                .replace("%format%", gameCategoryManager.game.format)
                .replace("%map_name%", gameCategoryManager.game.gameMap.getName())
                .replace("%first_team_invites%", firstTeamInviteStrings)
                .replace("%second_team_invites%", secondTeamInviteStrings);
        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            description = description.replace("%first_team%", gameCategoryManager.game.firstTeamInfo.role.getAsMention())
                    .replace("%second_team%", gameCategoryManager.game.secondTeamInfo.role.getAsMention());
        } else {
            description = description.replace("%first_team%", "team_" + gameCategoryManager.game.firstTeamInfo.captain.username)
                    .replace("%second_team%", "team_" + gameCategoryManager.game.secondTeamInfo.captain.username);
        }
        embedBuilder.setDescription(description);

        embedBuilder.addField("Отмена игры", "Для отмены игры введите `!cancel`", false);
        embedBuilder.addField("Ручная установка результата", "Для ручной установки id матча введите `!finish <ID>`", false);

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}
