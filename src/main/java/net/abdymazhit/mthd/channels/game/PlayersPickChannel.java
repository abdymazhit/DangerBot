package net.abdymazhit.mthd.channels.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал выбора игроков в команду
 *
 * @version   09.10.2021
 * @author    Islam Abdymazhit
 */
public class PlayersPickChannel extends Channel {

    /** Категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Капитан текущей выбирающей команды */
    public Member currentPickerCaptain;

    /** Id сообщения о картах */
    public String channelPlayersMessageId;

    /** Таймер обратного отсчета */
    public Timer timer;

    /** Время каждого раунда выбора игроков */
    private static final int roundTime = 30;

    /**
     * Инициализирует канал выбора игроков в команду
     * @param gameCategoryManager Категория игры
     */
    public PlayersPickChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;
        gameCategoryManager.game.players = new ArrayList<>();
        gameCategoryManager.game.firstTeamPlayers = new ArrayList<>();
        gameCategoryManager.game.firstTeamPlayers.add(gameCategoryManager.game.firstTeamCaptain.username);
        gameCategoryManager.game.secondTeamPlayers = new ArrayList<>();
        gameCategoryManager.game.secondTeamPlayers.add(gameCategoryManager.game.secondTeamCaptain.username);

        try {
            PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement("""
                    SELECT u.username as username FROM users as u
                    INNER JOIN single_live_games_players as slgp ON slgp.live_game_id = ? AND u.id = slgp.player_id;""");
            preparedStatement.setInt(1, gameCategoryManager.game.id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                String username = resultSet.getString("username");
                gameCategoryManager.game.players.add(username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

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
            System.out.println("Критическая ошибка! Не удалось получить роли капитанов первой и второй команды!");
            return;
        }

        MTHD.getInstance().guild.retrieveMemberById(gameCategoryManager.game.assistantAccount.discordId).queue(assistant -> {
            ChannelAction<TextChannel> createAction = category.createTextChannel("players-pick").setPosition(2)
                    .setSlowmode(5)
                    .addPermissionOverride(firstTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(secondTeamCaptain, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));

            for(Member member : gameCategoryManager.players) {
                createAction = createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
            }

            createAction.addPermissionOverride(assistant, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE), null).queue(textChannel -> {
                channelId = textChannel.getId();
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Первая стадия игры - Выбор игроков");
                embedBuilder.setColor(3092790);
                embedBuilder.setDescription("""
                        Капитаны команд (%first_captain% и %second_captain%) должны выбрать игроков в свою команду!
                        
                        Обратите внимание, если Вы не успеете выбрать игрока за отведенное время, тогда для вашей команды будет выбран случайный игрок.
          
                        Выбрать игрока
                        `!pick <НИК>`"""
                        .replace("%first_captain%", firstTeamCaptain.getAsMention())
                        .replace("%second_captain%", secondTeamCaptain.getAsMention()));
                textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                embedBuilder.clear();
                updatePlayersMessage(textChannel);
            });
        });
    }

    /**
     * Выбирает игрока
     * @param player Игрок
     */
    public void pickPlayer(String player, int teamId, String discordId) {
        if(discordId != null) {
            if(teamId == 0) {
                MTHD.getInstance().guild.retrieveMemberById(discordId).queue(gameCategoryManager::addToFirstTeamVoiceChannel);
            } else {
                MTHD.getInstance().guild.retrieveMemberById(discordId).queue(gameCategoryManager::addToSecondTeamVoiceChannel);
            }
        } else {
            UserAccount playerAccount = MTHD.getInstance().database.getUserIdAndDiscordId(player);
            if(playerAccount != null) {
                if(teamId == 0) {
                    MTHD.getInstance().guild.retrieveMemberById(playerAccount.discordId).queue(gameCategoryManager::addToFirstTeamVoiceChannel);
                } else {
                    MTHD.getInstance().guild.retrieveMemberById(playerAccount.discordId).queue(gameCategoryManager::addToSecondTeamVoiceChannel);
                }
            }
        }

        for(String name : gameCategoryManager.game.players) {
            if(name.equalsIgnoreCase(player)) {
                gameCategoryManager.game.players.remove(name);

                if(teamId == 0) {
                    gameCategoryManager.game.firstTeamPlayers.add(name);
                } else {
                    gameCategoryManager.game.secondTeamPlayers.add(name);
                }
                break;
            }
        }

        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал map-choice не существует!");
            return;
        }

        updatePlayersMessage(textChannel);
    }

    /**
     * Выбирает игрока за команду
     * @param playerName Имя игрока
     * @param teamId Id команды
     */
    public void pickPlayerToTeam(String playerName, int teamId) {
        UserAccount playerAccount = MTHD.getInstance().database.getUserIdAndDiscordId(playerName);
        if(playerAccount == null) return;

        MTHD.getInstance().database.addPlayerToTeam(teamId, playerAccount.id);
    }

    /**
     * Обновляет сообщение о доступных картах
     */
    private void updatePlayersMessage(TextChannel textChannel) {
        if(currentPickerCaptain == null) {
            currentPickerCaptain = gameCategoryManager.game.firstTeamCaptainMember;
        } else {
            if(currentPickerCaptain.equals(gameCategoryManager.game.firstTeamCaptainMember)) {
                currentPickerCaptain = gameCategoryManager.game.secondTeamCaptainMember;
            } else {
                currentPickerCaptain = gameCategoryManager.game.firstTeamCaptainMember;
            }
        }

        if(timer != null) {
            timer.cancel();
        }

        if(gameCategoryManager.game.players.size() == 1) {
            String playerName = gameCategoryManager.game.players.get(0);
            if(currentPickerCaptain.equals(gameCategoryManager.game.firstTeamCaptainMember)) {
                pickPlayerToTeam(playerName, 0);
                pickPlayer(playerName, 0, null);
            } else {
                pickPlayerToTeam(playerName, 1);
                pickPlayer(playerName, 1, null);
            }
        } else {
            createCountdownTask(textChannel);
        }
    }

    /**
     * Создает обратный отсчет
     * @param textChannel Канал выбора игроков в команду
     */
    private void createCountdownTask(TextChannel textChannel) {
        if(gameCategoryManager.game.players.size() == 0) {
            gameCategoryManager.setGameState(GameState.MAP_CHOICE);

            if(channelPlayersMessageId == null) {
                textChannel.sendMessageEmbeds(getPlayersPickMessage(-1)).queue(message -> new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        gameCategoryManager.createMapsChoiceChannel();
                    }
                }, 7000));
            } else {
                textChannel.editMessageEmbedsById(channelPlayersMessageId, getPlayersPickMessage(-1)).queue(message -> new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        gameCategoryManager.createMapsChoiceChannel();
                    }
                }, 7000));
            }
        } else {
            if(timer != null) {
                timer.cancel();
                timer = null;
            }

            if(channelPlayersMessageId == null) {
                textChannel.sendMessageEmbeds(getPlayersPickMessage(roundTime)).queue(message -> {
                    channelPlayersMessageId = message.getId();

                    AtomicInteger time =  new AtomicInteger(roundTime);
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(time.get() % 2 == 0) {
                                textChannel.editMessageEmbedsById(channelPlayersMessageId, getPlayersPickMessage(time.get())).queue();
                            }

                            if(time.get() <= 0) {
                                String playerName = gameCategoryManager.game.players.get(new Random().nextInt(gameCategoryManager.game.players.size()));
                                if(currentPickerCaptain.equals(gameCategoryManager.game.firstTeamCaptainMember)) {
                                    pickPlayerToTeam(playerName, 0);
                                    pickPlayer(playerName, 0, null);
                                } else {
                                    pickPlayerToTeam(playerName, 1);
                                    pickPlayer(playerName, 1, null);
                                }
                                cancel();
                            }
                            time.getAndDecrement();
                        }
                    }, 0, 1000);
                });
            } else {
                textChannel.editMessageEmbedsById(channelPlayersMessageId, getPlayersPickMessage(roundTime)).queue(message -> {
                    AtomicInteger time =  new AtomicInteger(roundTime);
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(time.get() % 2 == 0) {
                                textChannel.editMessageEmbedsById(channelPlayersMessageId, getPlayersPickMessage(time.get())).queue();
                            }

                            if(time.get() <= 0) {
                                String playerName = gameCategoryManager.game.players.get(new Random().nextInt(gameCategoryManager.game.players.size()));
                                if(currentPickerCaptain.equals(gameCategoryManager.game.firstTeamCaptainMember)) {
                                    pickPlayerToTeam(playerName, 0);
                                    pickPlayer(playerName, 0, null);
                                } else {
                                    pickPlayerToTeam(playerName, 1);
                                    pickPlayer(playerName, 1, null);
                                }
                                cancel();
                            }
                            time.getAndDecrement();
                        }
                    }, 0, 1000);
                });
            }
        }
    }

    /**
     * Получает сообщение о выборе игроков
     * @param time Время до выбора
     * @return Сообщение о выборе игроков
     */
    private MessageEmbed getPlayersPickMessage(int time) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(3092790);

        StringBuilder players1String = new StringBuilder();
        StringBuilder players2String = new StringBuilder();

        for(int i = 0; i < gameCategoryManager.game.firstTeamPlayers.size(); i++) {
            players1String.append("`").append(gameCategoryManager.game.firstTeamPlayers.get(i)).append("`").append("\n");
        }
        for(int i = 0; i < gameCategoryManager.game.secondTeamPlayers.size(); i++) {
            players2String.append("`").append(gameCategoryManager.game.secondTeamPlayers.get(i)).append("`").append("\n");
        }

        embedBuilder.addField("Команда " + gameCategoryManager.game.firstTeamCaptain.username, players1String.toString(), true);
        embedBuilder.addField("Команда " + gameCategoryManager.game.secondTeamCaptain.username, players2String.toString(), true);

        if(time >= 0) {
            if(currentPickerCaptain.getNickname() != null) {
                embedBuilder.setTitle("Капитан (%captain%) должен выбрать игрока в команду!"
                        .replace("%captain%", currentPickerCaptain.getNickname().replace("_", "\\_")));
            } else {
                embedBuilder.setTitle("Капитан (%captain%) должен выбрать игрока в команду!"
                        .replace("%captain%", currentPickerCaptain.getEffectiveName().replace("_", "\\_")));
            }
            embedBuilder.setDescription("Оставшееся время для выбора игрока: `%time% сек.`"
                    .replace("%time%", String.valueOf(time)));

            StringBuilder players = new StringBuilder();

            for(String player : gameCategoryManager.game.players) {
                players.append("`").append(player).append("`").append("\n");
            }
            embedBuilder.addField("Невыбранные игроки", players.toString(), false);
        } else {
            embedBuilder.setTitle("Переход к выбору карт...");
        }

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}
