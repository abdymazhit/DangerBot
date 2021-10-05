package net.abdymazhit.mthd.channels.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал выбора игроков на игру
 *
 * @version   05.10.2021
 * @author    Islam Abdymazhit
 */
public class PlayersChoiceChannel extends Channel {

    /** Категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Сообщение о игроках игры */
    public String channelGamePlayersMessageId;

    /** Сообщение отмены игры */
    public String channelGameCancelMessageId;

    /** Таймер выбора игроков */
    public Timer timer;

    /** Время выбора игроков */
    private static final int choiceTime = 120;

    /**
     * Инициализирует канал выбора игроков на игру
     * @param gameCategoryManager Категория игры
     */
    public PlayersChoiceChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;

        Category category = MTHD.getInstance().guild.getCategoryById(gameCategoryManager.categoryId);
        if(category == null) {
            System.out.println("Критическая ошибка! Категория Game не существует!");
            return;
        }

        MTHD.getInstance().guild.retrieveMemberById(gameCategoryManager.game.assistantAccount.discordId).queue(
                assistant -> {
                    List<Member> members = MTHD.getInstance().guild.retrieveMembersByIds(gameCategoryManager.game.firstTeamCaptain.discordId,
                            gameCategoryManager.game.secondTeamCaptain.discordId).get();
                    Member firstTeamCaptain = members.get(0);
                    Member secondTeamCaptain = members.get(1);
                    if(firstTeamCaptain == null || secondTeamCaptain == null) {
                        System.out.println("Критическая ошибка! Не удалось получить роли начавших игру первой и второй команды!");
                        return;
                    }

                    category.createTextChannel("players-choice").setPosition(2)
                            .addPermissionOverride(firstTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE), null)
                            .addPermissionOverride(secondTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE), null)
                            .addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                            .addPermissionOverride(gameCategoryManager.firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                            .addPermissionOverride(gameCategoryManager.secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                            .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                            .queue(textChannel -> {
                                channelId = textChannel.getId();

                                AtomicInteger time = new AtomicInteger(choiceTime);
                                timer = new Timer();
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        if(time.get() <= 0) {
                                            boolean isCancelling = false;

                                            if(gameCategoryManager.game.format.equals("4x2")) {
                                                if(gameCategoryManager.game.firstTeamPlayers.size() < 4 || gameCategoryManager.game.secondTeamPlayers.size() < 4) {
                                                    isCancelling = true;
                                                }
                                            } else if(gameCategoryManager.game.format.equals("6x2")) {
                                                if(gameCategoryManager.game.firstTeamPlayers.size() < 6 || gameCategoryManager.game.secondTeamPlayers.size() < 6) {
                                                    isCancelling = true;
                                                }
                                            }

                                            if(isCancelling) {
                                                textChannel.sendMessage("Недостаточно игроков для начала игры! Игра отменяется...")
                                                        .queue(message -> channelGameCancelMessageId = message.getId());
                                                MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.game);
                                                new Timer().schedule(new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        MTHD.getInstance().gameManager.deleteGame(category.getId());
                                                    }
                                                }, 7000);
                                            } else {
                                                gameCategoryManager.setGameState(GameState.MAP_CHOICE);
                                                textChannel.sendMessage("Игроки успешно выбраны для игры. Переход к выбору карт...").queue();
                                                new Timer().schedule(new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        gameCategoryManager.createMapsChoiceChannel();
                                                    }
                                                }, 7000);
                                            }
                                            cancel();
                                        }

                                        if(time.get() % 2 == 0) {
                                            sendChannelMessage(textChannel, time.get());
                                        }

                                        time.getAndDecrement();
                                    }
                                }, 0, 1000);
                                updateGamePlayersMessage();
                            });
                }
        );
    }

    /**
     * Отправляет сообщение канала выбора игроков на игру
     * @param time Время до конца установки игроков на игру
     */
    private void sendChannelMessage(TextChannel textChannel, int time) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Первая стадия игры - Выбор игроков на игру");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
            Начавшие поиск игры команд (%first_team% и %second_team%) должны решить, кто из игроков будет играть в этой игре!
            У вас есть `%time% сек.` для установки игроков на игру!
             
            Добавить участника в игру
            `!add <NAME>`

            Удалить участника из игры
            `!delete <NAME>`"""
                .replace("%first_team%", gameCategoryManager.firstTeamRole.getAsMention())
                .replace("%second_team%", gameCategoryManager.secondTeamRole.getAsMention())
                .replace("%time%", String.valueOf(time)));
        if(channelMessageId == null) {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
        } else {
            textChannel.editMessageEmbedsById(channelMessageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Обновляет сообщение о игроках игры
     */
    public void updateGamePlayersMessage() {
        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал players-choice не существует!");
            return;
        }

        gameCategoryManager.game.firstTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(gameCategoryManager.game.firstTeam.id);
        gameCategoryManager.game.secondTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(gameCategoryManager.game.secondTeam.id);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Список участвующих в игре игроков");
        embedBuilder.setColor(3092790);

        StringBuilder firstTeamPlayersNames = new StringBuilder();
        if(gameCategoryManager.game.firstTeamPlayers.isEmpty()) {
            firstTeamPlayersNames.append("-").append("\n");
        } else {
            for(String name : gameCategoryManager.game.firstTeamPlayers) {
                firstTeamPlayersNames.append(name).append("\n");
            }
        }

        StringBuilder secondTeamPlayersNames = new StringBuilder();
        if(gameCategoryManager.game.secondTeamPlayers.isEmpty()) {
            secondTeamPlayersNames.append("-").append("\n");
        } else {
            for(String name : gameCategoryManager.game.secondTeamPlayers) {
                secondTeamPlayersNames.append(name).append("\n");
            }
        }

        embedBuilder.addField(gameCategoryManager.game.firstTeam.name, firstTeamPlayersNames.toString(), true);
        embedBuilder.addField(gameCategoryManager.game.secondTeam.name, secondTeamPlayersNames.toString(), true);

        if(channelGamePlayersMessageId == null) {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelGamePlayersMessageId = message.getId());
        } else {
            textChannel.editMessageEmbedsById(channelGamePlayersMessageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }
}
