package net.abdymazhit.mthd.game;

import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.GameState;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Канал выбора игроков на игру
 *
 * @version   22.09.2021
 * @author    Islam Abdymazhit
 */
public class PlayersChoiceChannel extends Channel {

    /** Категория игры */
    private final GameCategory gameCategory;

    /** Сообщение о игроках игры */
    public String channelGamePlayersMessageId;

    /** Сообщение отмены игры */
    public String channelGameCancelMessageId;

    /** Время выбора игроков */
    private static final int choiceTime = 120;

    /**
     * Инициализирует канал выбора игроков на игру
     * @param gameCategory Категория игры
     */
    public PlayersChoiceChannel(GameCategory gameCategory) {
        this.gameCategory = gameCategory;

        Category category = MTHD.getInstance().guild.getCategoryById(gameCategory.categoryId);
        if(category == null) {
            System.out.println("Критическая ошибка! Категория Game не существует!");
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

        MTHD.getInstance().guild.retrieveMemberById(gameCategory.game.assistantDiscordId).queue(assistant ->
            category.createTextChannel("players-choice").setPosition(2)
                .addPermissionOverride(firstTeamStarter, EnumSet.of(Permission.MESSAGE_WRITE), null)
                .addPermissionOverride(secondTeamStarter, EnumSet.of(Permission.MESSAGE_WRITE), null)
                .addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(gameCategory.firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(gameCategory.secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    channelId = textChannel.getId();

                    AtomicInteger time = new AtomicInteger(choiceTime);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(time.get() <= 0) {
                                boolean isCancelling = false;

                                if(gameCategory.game.format.equals("4x2")) {
                                    if(gameCategory.game.firstTeamPlayers.size() < 4 || gameCategory.game.secondTeamPlayers.size() < 4) {
                                        isCancelling = true;
                                    }
                                } else if(gameCategory.game.format.equals("6x2")) {
                                    if(gameCategory.game.firstTeamPlayers.size() < 6 || gameCategory.game.secondTeamPlayers.size() < 6) {
                                        isCancelling = true;
                                    }
                                }

                                if(isCancelling) {
                                    textChannel.sendMessage("Недостаточно игроков для начала игры! Игра отменяется...")
                                        .queue(message -> channelGameCancelMessageId = message.getId());
                                    MTHD.getInstance().gameManager.deleteGame(gameCategory.game);
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            MTHD.getInstance().gameManager.deleteGame(category.getId());
                                        }
                                    }, 7000);
                                } else {
                                    gameCategory.setGameState(GameState.MAP_CHOICE);
                                    textChannel.sendMessage("Игроки успешно выбраны для игры. Переход к выбору карт...").queue();
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            gameCategory.createMapsChoiceChannel();
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
                })
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
            .replace("%first_team%", gameCategory.firstTeamRole.getAsMention())
            .replace("%second_team%", gameCategory.secondTeamRole.getAsMention())
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

        gameCategory.game.firstTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(gameCategory.game.firstTeamId);
        gameCategory.game.secondTeamPlayers = MTHD.getInstance().database.getTeamPlayersNames(gameCategory.game.secondTeamId);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Список участвующих в игре игроков");
        embedBuilder.setColor(3092790);

        StringBuilder firstTeamPlayersNames = new StringBuilder();
        if(gameCategory.game.firstTeamPlayers.isEmpty()) {
            firstTeamPlayersNames.append("-").append("\n");
        } else {
            for(String name : gameCategory.game.firstTeamPlayers) {
                firstTeamPlayersNames.append(name).append("\n");
            }
        }
        embedBuilder.addField(gameCategory.game.firstTeamName, firstTeamPlayersNames.toString(), true);

        StringBuilder secondTeamPlayersNames = new StringBuilder();
        if(gameCategory.game.secondTeamPlayers.isEmpty()) {
            secondTeamPlayersNames.append("-").append("\n");
        } else {
            for(String name : gameCategory.game.secondTeamPlayers) {
                secondTeamPlayersNames.append(name).append("\n");
            }
        }
        embedBuilder.addField(gameCategory.game.secondTeamName, secondTeamPlayersNames.toString(), true);

        if(channelGamePlayersMessageId == null) {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelGamePlayersMessageId = message.getId());
        } else {
            textChannel.editMessageEmbedsById(channelGamePlayersMessageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }
}
