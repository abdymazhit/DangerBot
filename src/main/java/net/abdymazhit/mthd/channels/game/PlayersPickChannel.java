package net.abdymazhit.mthd.channels.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал выбора игроков в команду
 *
 * @version   21.10.2021
 * @author    Islam Abdymazhit
 */
public class PlayersPickChannel extends Channel {

    /** Менеджер категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Информационное сообщение о выборе игроков */
    public Message channelPlayersMessage;

    /** Время каждого раунда выбора игроков */
    private static final int roundTime = 30;

    /** Таймер обратного отсчета */
    public Timer timer;

    /** Капитан текущей выбирающей команды */
    public Member currentPickerCaptain;

    /**
     * Инициализирует канал выбора игроков в команду
     * @param gameCategoryManager Менеджер категория игры
     */
    public PlayersPickChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;

        ChannelAction<TextChannel> createAction = gameCategoryManager.category.createTextChannel("players-pick").setPosition(2).setSlowmode(5)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(gameCategoryManager.game.firstTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(gameCategoryManager.game.secondTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null);
        for(UserAccount userAccount : gameCategoryManager.game.playersAccounts) {
            createAction = createAction.addPermissionOverride(userAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
        }
        createAction.addPermissionOverride(gameCategoryManager.game.assistantAccount.member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE), null).queue(textChannel -> {
            channel = textChannel;

            // Отправляет главное сообщение канала
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Вторая стадия игры - Выбор игроков");
            embedBuilder.setColor(3092790);
            embedBuilder.setDescription("""
                    Капитаны команд (%first_captain% и %second_captain%) должны выбрать игроков в свою команду!
                        
                    Обратите внимание, если Вы не успеете выбрать игрока за отведенное время, тогда для вашей команды будет выбран случайный игрок.
          
                    Выбрать игрока
                    `!pick <НИК>`"""
                    .replace("%first_captain%", gameCategoryManager.game.firstTeamInfo.captain.member.getAsMention())
                    .replace("%second_captain%", gameCategoryManager.game.secondTeamInfo.captain.member.getAsMention()));
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
            embedBuilder.clear();

            updatePlayersMessage();
        });
    }

    /**
     * Выбирает игрока в команду
     * @param playerName Имя игрока
     * @param teamId Id команды
     */
    public void pickPlayer(String playerName, int teamId) {
        for(UserAccount playerAccount : new ArrayList<>(gameCategoryManager.game.playersAccounts)) {
            if(playerAccount.username.equalsIgnoreCase(playerName)) {
                gameCategoryManager.game.playersAccounts.remove(playerAccount);

                String discordId = MTHD.getInstance().database.getUserDiscordId(playerName);
                if(discordId != null) {
                    Member member = MTHD.getInstance().guild.getMemberById(discordId);
                    if(teamId == 0) {
                        gameCategoryManager.addToFirstTeamVoiceChannel(member);
                    } else {
                        gameCategoryManager.addToSecondTeamVoiceChannel(member);
                    }
                }

                if(teamId == 0) {
                    gameCategoryManager.game.firstTeamInfo.members.add(playerAccount);
                } else {
                    gameCategoryManager.game.secondTeamInfo.members.add(playerAccount);
                }
                break;
            }
        }
        updatePlayersMessage();
    }

    /**
     * Выбирает игрока в команду
     * @param playerName Имя игрока
     * @param teamId Id команды
     */
    public void pickPlayerToTeam(String playerName, int teamId) {
        int userId = MTHD.getInstance().database.getUserIdByUsername(playerName);
        if(userId > 0) {
            MTHD.getInstance().database.addPlayerToTeam(teamId, userId);
        }
    }

    /**
     * Обновляет информационное сообщение о выборе игроков
     */
    private void updatePlayersMessage() {
        if(currentPickerCaptain == null) {
            currentPickerCaptain = gameCategoryManager.game.firstTeamInfo.captain.member;
        } else {
            if(currentPickerCaptain.equals(gameCategoryManager.game.firstTeamInfo.captain.member)) {
                currentPickerCaptain = gameCategoryManager.game.secondTeamInfo.captain.member;
            } else {
                currentPickerCaptain = gameCategoryManager.game.firstTeamInfo.captain.member;
            }
        }

        if(timer != null) {
            timer.cancel();
        }

        if(gameCategoryManager.game.playersAccounts.size() == 1) {
            String playerName = gameCategoryManager.game.playersAccounts.get(0).username;
            if(currentPickerCaptain.equals(gameCategoryManager.game.firstTeamInfo.captain.member)) {
                pickPlayer(playerName, 0);
                pickPlayerToTeam(playerName, 0);
            } else {
                pickPlayer(playerName, 1);
                pickPlayerToTeam(playerName, 1);
            }
        } else {
            createCountdownTask();
        }
    }

    /**
     * Создает таймер обратного отсчета
     */
    private void createCountdownTask() {
        if(gameCategoryManager.game.playersAccounts.size() == 0) {
            gameCategoryManager.setGameState(GameState.MAP_CHOICE);

            if(channelPlayersMessage == null) {
                channel.sendMessageEmbeds( getPlayersPickMessage(-1)).queue(message -> {
                    channelPlayersMessage = message;
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            gameCategoryManager.createMapsChoiceChannel();
                        }
                    }, 7000); }
                );
            } else {
                channel.editMessageEmbedsById(channelPlayersMessage.getId(), getPlayersPickMessage(-1)).queue(message -> new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        gameCategoryManager.createMapsChoiceChannel();
                    }
                }, 7000));
            }
        } else {
            if(timer != null) {
                timer.cancel();
            }

            if(channelPlayersMessage == null) {
                channel.sendMessageEmbeds(getPlayersPickMessage(roundTime)).queue(message -> {
                    channelPlayersMessage = message;
                    performTimeActions();
                });
            } else {
                channel.editMessageEmbedsById(channelPlayersMessage.getId(), getPlayersPickMessage(roundTime)).queue(message -> performTimeActions());
            }
        }
    }

    /**
     * Выполняет действия времени
     */
    private void performTimeActions() {
        AtomicInteger time =  new AtomicInteger(roundTime);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(time.get() % 2 == 0) {
                    channel.editMessageEmbedsById(channelPlayersMessage.getId(), getPlayersPickMessage(time.get())).queue();
                }

                if(time.get() == 0) {
                    String playerName = gameCategoryManager.game.playersAccounts.get(new Random().nextInt(gameCategoryManager.game.playersAccounts.size())).username;
                    if(currentPickerCaptain.equals(gameCategoryManager.game.firstTeamInfo.captain.member)) {
                        pickPlayer(playerName, 0);
                        pickPlayerToTeam(playerName, 0);
                    } else {
                        pickPlayer(playerName, 1);
                        pickPlayerToTeam(playerName, 1);
                    }
                    cancel();
                }
                time.getAndDecrement();
            }
        }, 0, 1000);
    }

    /**
     * Получает информационное сообщение о выборе игроков
     * @param time Время до автовыбора игрока
     * @return Информационное сообщение о выборе игроков
     */
    private MessageEmbed getPlayersPickMessage(int time) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(3092790);

        StringBuilder players1String = new StringBuilder();
        StringBuilder players2String = new StringBuilder();

        for(int i = 0; i < gameCategoryManager.game.firstTeamInfo.members.size(); i++) {
            players1String.append("`").append(gameCategoryManager.game.firstTeamInfo.members.get(i).username).append("`").append("\n");
        }
        for(int i = 0; i < gameCategoryManager.game.secondTeamInfo.members.size(); i++) {
            players2String.append("`").append(gameCategoryManager.game.secondTeamInfo.members.get(i).username).append("`").append("\n");
        }

        embedBuilder.addField("Команда " + gameCategoryManager.game.firstTeamInfo.captain.username, players1String.toString(), true);
        embedBuilder.addField("Команда " + gameCategoryManager.game.secondTeamInfo.captain.username, players2String.toString(), true);

        if(time >= 0) {
            if(currentPickerCaptain.getNickname() != null) {
                embedBuilder.setTitle("Капитан (%captain%) должен выбрать игрока в команду!".replace("%captain%", currentPickerCaptain.getNickname()));
            } else {
                embedBuilder.setTitle("Капитан (%captain%) должен выбрать игрока в команду!".replace("%captain%", currentPickerCaptain.getEffectiveName()));
            }

            embedBuilder.setDescription("Оставшееся время для выбора игрока: `%time% сек.`".replace("%time%", String.valueOf(time)));

            StringBuilder playersNames = new StringBuilder();
            for(UserAccount userAccount : gameCategoryManager.game.playersAccounts) {
                playersNames.append("`").append(userAccount.username).append("`").append("\n");
            }
            embedBuilder.addField("Невыбранные игроки", playersNames.toString(), false);
        } else {
            embedBuilder.setTitle("Переход к выбору карт...");
        }

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}
