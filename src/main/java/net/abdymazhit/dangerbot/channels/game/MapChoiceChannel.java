package net.abdymazhit.dangerbot.channels.game;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Channel;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.GameMap;
import net.abdymazhit.dangerbot.enums.GameState;
import net.abdymazhit.dangerbot.enums.Rating;
import net.abdymazhit.dangerbot.managers.GameCategoryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static net.dv8tion.jda.api.exceptions.ErrorResponseException.ignore;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_CHANNEL;

/**
 * Канал выбора карты
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class MapChoiceChannel extends Channel {

    /** Менеджер категория игры */
    private final GameCategoryManager gameCategoryManager;

    /** Информационное сообщение о выборе карты */
    public Message channelMapsMessage;

    /** Время каждого раунда бана */
    private static final int roundTime = 20;

    /** Таймер обратного отсчета */
    public Timer timer;

    /** Список выбираемых карт */
    public List<GameMap> gameAllMaps;

    /** Список оставшихся карт */
    public List<GameMap> gameMaps;

    /** Роль текущей банющей команды */
    public Role currentBannerTeamRole;

    /** Текущий банющий капитан */
    public Member currentBannerCaptain;

    /** Значение, отправляется ли сообщение о доступных картах */
    public boolean isMapsMessageSending;

    /**
     * Инициализирует канал выбора карты
     * @param gameCategoryManager Менеджер категория игры
     */
    public MapChoiceChannel(GameCategoryManager gameCategoryManager) {
        this.gameCategoryManager = gameCategoryManager;
        gameAllMaps = new ArrayList<>();
        gameMaps = new ArrayList<>();
        isMapsMessageSending = true;

        gameCategoryManager.game.playersAccounts = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = DangerBot.getInstance().database.getConnection().prepareStatement(
                    "SELECT player_id FROM single_live_games_players WHERE live_game_id = ?;");
            preparedStatement.setInt(1, gameCategoryManager.game.id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                int playerId = resultSet.getInt("player_id");
                gameCategoryManager.game.playersAccounts.add(new UserAccount(playerId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(gameCategoryManager.game.format.equals("4x2")) {
            List<GameMap> random4x2Maps = GameMap.getRandom4x2Maps();
            gameMaps = new ArrayList<>(random4x2Maps);
            gameAllMaps = new ArrayList<>(random4x2Maps);
        } else if(gameCategoryManager.game.format.equals("6x2")) {
            List<GameMap> random6x2Maps = GameMap.getRandom6x2Maps();
            gameMaps = new ArrayList<>(random6x2Maps);
            gameAllMaps = new ArrayList<>(random6x2Maps);
        }

        ChannelAction<TextChannel> createAction = gameCategoryManager.category.createTextChannel("map-choice").setPosition(2).setSlowmode(5)
                .addPermissionOverride(DangerBot.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            createAction = createAction.addPermissionOverride(gameCategoryManager.game.firstTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                    .addPermissionOverride(gameCategoryManager.game.secondTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                    .addPermissionOverride(gameCategoryManager.game.firstTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE), null)
                    .addPermissionOverride(gameCategoryManager.game.secondTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE), null);
        } else {
            createAction = createAction.addPermissionOverride(gameCategoryManager.game.firstTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(gameCategoryManager.game.secondTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null);
            for(UserAccount userAccount : gameCategoryManager.game.playersAccounts) {
                createAction = createAction.addPermissionOverride(userAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
            }
        }
        createAction.addPermissionOverride(gameCategoryManager.game.assistantAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue(textChannel -> {
            channel = textChannel;

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Третья стадия игры - Выбор карты");
            embedBuilder.setColor(3092790);

            String description = """
                    Капитаны команд (%first_team% и %second_team%) должны решить какую карту будут играть!
                
                    Обратите внимание, если Вы не успеете заблокировать карту за отведенное время, тогда будет заблокирована случайная карта.
  
                    Заблокировать карту
                    `!ban <ПОРЯДКОВЫЙ НОМЕР> или <НАЗВАНИЕ КАРТЫ>`""";
            if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
                description = description.replace("%first_team%", gameCategoryManager.game.firstTeamInfo.role.getAsMention())
                        .replace("%second_team%", gameCategoryManager.game.secondTeamInfo.role.getAsMention());
            } else {
                description = description.replace("%first_team%", gameCategoryManager.game.firstTeamInfo.captain.member.getAsMention())
                        .replace("%second_team%", gameCategoryManager.game.secondTeamInfo.captain.member.getAsMention());
            }

            embedBuilder.setDescription(description);
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
            embedBuilder.clear();

            updateMapsMessage();
        });
    }

    /**
     * Блокирует карту
     * @param gameMap Карта
     */
    public void banMap(GameMap gameMap) {
        gameMaps.remove(gameMap);
        updateMapsMessage();
    }

    /**
     * Обновляет информационное сообщение о выборе карты
     */
    private void updateMapsMessage() {
        if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
            if(currentBannerTeamRole == null) {
                currentBannerTeamRole = gameCategoryManager.game.firstTeamInfo.role;
            } else {
                if(currentBannerTeamRole.equals(gameCategoryManager.game.firstTeamInfo.role)) {
                    currentBannerTeamRole = gameCategoryManager.game.secondTeamInfo.role;
                } else {
                    currentBannerTeamRole = gameCategoryManager.game.firstTeamInfo.role;
                }
            }
        } else {
            if(currentBannerCaptain == null) {
                currentBannerCaptain = gameCategoryManager.game.firstTeamInfo.captain.member;
            } else {
                if(currentBannerCaptain.equals(gameCategoryManager.game.firstTeamInfo.captain.member)) {
                    currentBannerCaptain = gameCategoryManager.game.secondTeamInfo.captain.member;
                } else {
                    currentBannerCaptain = gameCategoryManager.game.firstTeamInfo.captain.member;
                }
            }
        }

        Map<GameMap, BufferedImage> images = new HashMap<>();
        if(gameMaps.size() == 1) {
            images.put(gameMaps.get(0), gameMaps.get(0).getPickImage());
            for(GameMap gameMap : gameAllMaps) {
                if(!images.containsKey(gameMap)) {
                    images.put(gameMap, gameMap.getBanImage());
                }
            }
        } else {
            for(GameMap gameMap : gameAllMaps) {
                if(gameMaps.contains(gameMap)) {
                    images.put(gameMap, gameMap.getNormalImage());
                } else {
                    images.put(gameMap, gameMap.getBanImage());
                }
            }
        }

        BufferedImage image = new BufferedImage(gameAllMaps.size() * 710,624, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        int x = 0;
        for(GameMap gameAllMap : gameAllMaps) {
            graphics.drawImage(images.get(gameAllMap), x, 0, null);
            x += 710;
        }
        graphics.dispose();

        try {
            if(timer != null) {
                timer.cancel();
            }

            File file = new File("./maps/image.png");
            ImageIO.write(image, "png", file);
            createCountdownTask(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таймер обратного отсчета
     * @param file Файл изображения
     */
    private void createCountdownTask(File file) {
        if(gameMaps.size() == 1) {
            gameCategoryManager.setGameState(GameState.GAME_CREATION);
            gameCategoryManager.setGameMap(gameMaps.get(0));

            isMapsMessageSending = true;
            channel.editMessageEmbedsById(channelMapsMessage.getId(), getMapPickMessage(-1)).retainFiles(new ArrayList<>()).addFile(file).queue(message -> {
                isMapsMessageSending = false;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        gameCategoryManager.createGameChannel();
                    }
                }, 7000);
            });
        } else {
            if(timer != null) {
                timer.cancel();
            }

            AtomicBoolean canStart = new AtomicBoolean(false);
            if(channelMapsMessage == null) {
                isMapsMessageSending = true;
                channel.sendMessageEmbeds(getMapPickMessage(roundTime)).addFile(file).queue(message -> {
                    channelMapsMessage = message;
                    isMapsMessageSending = false;
                    canStart.set(true);
                });
            } else {
                isMapsMessageSending = true;
                channel.editMessageEmbedsById(channelMapsMessage.getId(), getMapPickMessage(roundTime))
                        .retainFiles(new ArrayList<>()).addFile(file).queue(message -> {
                    isMapsMessageSending = false;
                    canStart.set(true);
                });
            }

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if(canStart.get()) {
                        AtomicInteger time =  new AtomicInteger(roundTime);
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if(time.get() % 2 == 0) {
                                    channel.editMessageEmbedsById(channelMapsMessage.getId(), getMapPickMessage(time.get())).queue(null, ignore(UNKNOWN_CHANNEL));
                                }

                                if(time.get() == 0) {
                                    banMap(gameMaps.get(new Random().nextInt(gameMaps.size())));
                                    cancel();
                                }
                                time.getAndDecrement();
                            }
                        }, 0, 1000);
                        cancel();
                    }
                }
            }, 0, 100);
        }
    }

    /**
     * Получает информационное сообщение о выборе карты
     * @param time Время до автоблокировки карты
     * @return Информационное сообщение о выборе карты
     */
    private MessageEmbed getMapPickMessage(int time) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(3092790);

        if(time >= 0) {
            if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
                embedBuilder.setTitle("Команда %team% должна забанить карту!"
                        .replace("%team%", currentBannerTeamRole.getName()));
            } else {
                if(currentBannerCaptain.getNickname() != null) {
                    embedBuilder.setTitle("Капитан %captain% должен забанить карту!"
                            .replace("%captain%", currentBannerCaptain.getNickname()));
                } else {
                    embedBuilder.setTitle("Капитан %captain% должен забанить карту!"
                            .replace("%captain%", currentBannerCaptain.getEffectiveName()));
                }
            }

            embedBuilder.setDescription("Оставшееся время для бана карты: `%time% сек.`"
                    .replace("%time%", String.valueOf(time)));
        } else {
            embedBuilder.setTitle("Карта успешно выбрана! Переход к созданию игры...");
        }

        StringBuilder mapsString = new StringBuilder();
        for(int i = 0; i < gameAllMaps.size(); i++) {
            GameMap gameMap = gameAllMaps.get(i);
            if(gameMaps.contains(gameMap)) {
                mapsString.append(i + 1).append(". ").append(gameMap.getName()).append("\n");
            }
        }
        embedBuilder.addField("Доступные карты", mapsString.toString(), true);

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }
}