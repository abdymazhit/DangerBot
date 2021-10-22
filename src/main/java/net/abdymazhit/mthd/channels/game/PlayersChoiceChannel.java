package net.abdymazhit.mthd.channels.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал выбора игроков на игру
 *
 * @version   22.10.2021
 * @author    Islam Abdymazhit
 */
public class PlayersChoiceChannel extends Channel {

    /** Менеджер категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Информационное сообщение о игроках */
    public Message channelPlayersMessage;


    /** Время выбора игроков */
    private static final int choiceTime = 120;

    /** Оставшееся время для выбора игроков */
    private final AtomicInteger time;

    /** Таймер выбора игроков */
    public Timer timer;

    /**
     * Инициализирует канал выбора игроков на игру
     * @param gameCategoryManager Менеджер категория игры
     */
    public PlayersChoiceChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;
        time = new AtomicInteger(choiceTime);

        gameCategoryManager.category.createTextChannel("players-choice").setPosition(2).setSlowmode(5)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(gameCategoryManager.game.firstTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(gameCategoryManager.game.secondTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(gameCategoryManager.game.firstTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE), null)
                .addPermissionOverride(gameCategoryManager.game.secondTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE), null)
                .addPermissionOverride(gameCategoryManager.game.assistantAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE)).queue(textChannel -> {
            channel = textChannel;

            // Отправляет главное сообщение канала
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Вторая стадия игры - Выбор игроков на игру");
            embedBuilder.setColor(3092790);
            embedBuilder.setDescription("""
                    Начавшие поиск игры команд (%first_team% и %second_team%) должны решить, кто из игроков будет играть в этой игре!
             
                    Добавить участника в игру
                    `!add <NAME>`

                    Удалить участника из игры
                    `!delete <NAME>`"""
                    .replace("%first_team%", gameCategoryManager.game.firstTeamInfo.role.getAsMention())
                    .replace("%second_team%", gameCategoryManager.game.secondTeamInfo.role.getAsMention()));
            if(channelMessage == null) {
                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
            } else {
                channel.editMessageEmbedsById(channelMessage.getId(), embedBuilder.build()).queue();
            }
            embedBuilder.clear();

            // Запускает таймер обратного отсчета
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(time.get() % 2 == 0) {
                        updateGamePlayersMessage();
                    }

                    if(time.get() == 0) {
                        boolean isCancelling = false;

                        if(gameCategoryManager.game.format.equals("4x2")) {
                            if(gameCategoryManager.game.firstTeamInfo.members.size() < 2 || gameCategoryManager.game.secondTeamInfo.members.size() < 2) {
                                isCancelling = true;
                            }
                        } else if(gameCategoryManager.game.format.equals("6x2")) {
                            if(gameCategoryManager.game.firstTeamInfo.members.size() < 6 || gameCategoryManager.game.secondTeamInfo.members.size() < 6) {
                                isCancelling = true;
                            }
                        }

                        if(isCancelling) {
                            textChannel.sendMessage("Недостаточно игроков для начала игры! Игра отменяется...").queue();
                            MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.game);
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.category.getId());
                                }
                            }, 7000);
                        } else {
                            gameCategoryManager.setGameState(GameState.MAP_CHOICE);
                            textChannel.sendMessage("Игроки успешно выбраны. Переход к выбору карт...").queue();
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    gameCategoryManager.createMapsChoiceChannel();
                                }}, 7000);
                        }
                        cancel();
                    }

                    time.getAndDecrement();
                }}, 0, 1000);
        });
    }

    /**
     * Обновляет информационное сообщение о игроках игры
     */
    public void updateGamePlayersMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Список участвующих в игре игроков");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("У вас есть `%time% сек.` для установки игроков на игру!".replace("%time%", String.valueOf(time.get())));

        StringBuilder firstTeamPlayersNames = new StringBuilder();
        if(gameCategoryManager.game.firstTeamInfo.members.isEmpty()) {
            firstTeamPlayersNames.append("-").append("\n");
        } else {
            for(UserAccount userAccount : gameCategoryManager.game.firstTeamInfo.members) {
                firstTeamPlayersNames.append(userAccount.username).append("\n");
            }
        }

        StringBuilder secondTeamPlayersNames = new StringBuilder();
        if(gameCategoryManager.game.secondTeamInfo.members.isEmpty()) {
            secondTeamPlayersNames.append("-").append("\n");
        } else {
            for(UserAccount userAccount : gameCategoryManager.game.secondTeamInfo.members) {
                secondTeamPlayersNames.append(userAccount.username).append("\n");
            }
        }

        embedBuilder.addField(gameCategoryManager.game.firstTeamInfo.name, firstTeamPlayersNames.toString(), true);
        embedBuilder.addField(gameCategoryManager.game.secondTeamInfo.name, secondTeamPlayersNames.toString(), true);

        if(channelPlayersMessage == null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelPlayersMessage = message);
        } else {
            channel.editMessageEmbedsById(channelPlayersMessage.getId(), embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }
}
