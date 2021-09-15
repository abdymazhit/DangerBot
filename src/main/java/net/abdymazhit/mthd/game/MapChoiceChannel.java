package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.GameMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал выбора карты
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class MapChoiceChannel extends Channel {

    /** Категория игры */
    private final GameCategory gameCategory;

    /** Список карт */
    public final List<GameMap> gameMaps;

    /** Роль текущей банющей команды */
    public Role currentBannerTeamRole;

    /** Id сообщения о картах */
    public String channelMapsMessageId;

    /** Таймер обратного отсчета */
    private Timer timer;

    /**
     * Инициализирует канал выбора карты
     * @param gameCategory Категория игры
     */
    public MapChoiceChannel(GameCategory gameCategory) {
        this.gameCategory = gameCategory;
        gameMaps = new ArrayList<>();
        Collections.addAll(gameMaps, GameMap.values());

        ChannelAction<TextChannel> createAction = createChannel(gameCategory.categoryId, "map-choice", 2);

        List<Member> members = MTHD.getInstance().guild.retrieveMembersByIds(gameCategory.game.firstTeamStarterDiscordId,
                gameCategory.game.secondTeamStarterDiscordId).get();

        Member firstTeamStarter = members.get(0);
        Member secondTeamStarter = members.get(1);

        if(firstTeamStarter == null || secondTeamStarter == null) {
            return;
        }

        createAction = createAction.addPermissionOverride(firstTeamStarter,
                EnumSet.of(Permission.MESSAGE_WRITE), null);
        createAction = createAction.addPermissionOverride(secondTeamStarter,
                EnumSet.of(Permission.MESSAGE_WRITE), null);

        createAction = createAction.addPermissionOverride(gameCategory.firstTeamRole,
                EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
        createAction = createAction.addPermissionOverride(gameCategory.secondTeamRole,
                EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
        createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(),
                null, EnumSet.of(Permission.VIEW_CHANNEL));
        createAction.queue(textChannel -> channelId = textChannel.getId());

        sendChannelMessage();
        updateMapsMessage();
    }

    /**
     * Отправляет сообщение канала выбора карты
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Вторая стадия игры - Выбор карты");
        embedBuilder.setDescription("Начавшие поиск игры команд (" + gameCategory.firstTeamRole.getAsMention() + " и "
                + gameCategory.secondTeamRole.getAsMention() + ") должны решить какую карту будут играть!");
        embedBuilder.setColor(0xFF58B9FF);
        embedBuilder.addField("Заблокировать карту", "`!ban <NAME>`", false);

        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(channel != null) {
            if(channelMessageId == null) {
                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
            } else {
                channel.editMessageEmbedsById(channelMessageId, embedBuilder.build()).queue();
            }
        }

        embedBuilder.clear();
    }

    /**
     * Обновляет сообщение о картах
     */
    private void updateMapsMessage() {
        Map<GameMap, BufferedImage> images = new HashMap<>();

        if(currentBannerTeamRole == null) {
            currentBannerTeamRole = gameCategory.firstTeamRole;
        } else {
            if(currentBannerTeamRole.equals(gameCategory.firstTeamRole)) {
                currentBannerTeamRole = gameCategory.secondTeamRole;
            } else {
                currentBannerTeamRole = gameCategory.firstTeamRole;
            }
        }

        if(gameMaps.size() == 1) {
            images.put(gameMaps.get(0), gameMaps.get(0).getPickImage());

            for(GameMap gameMap : GameMap.values()) {
                if(!images.containsKey(gameMap)) {
                    images.put(gameMap, gameMap.getBanImage());
                }
            }
        } else {
            for(GameMap gameMap : GameMap.values()) {
                if(gameMaps.contains(gameMap)) {
                    images.put(gameMap, gameMap.getNormalImage());
                } else {
                    images.put(gameMap, gameMap.getBanImage());
                }
            }
        }

        BufferedImage image = new BufferedImage(((GameMap.values().length / 2) + 1) * 710,624 * 2, BufferedImage.TYPE_INT_ARGB);
        int x = 0;
        boolean isSecondLine = false;
        for(int i = 0; i < GameMap.values().length; i++) {
            int index = (GameMap.values().length / 2) + 1;
            if(i < index) {
                image.getGraphics().drawImage(images.get(GameMap.values()[i]), x, 0, null);
            } else {
                if(!isSecondLine) {
                    x = 315;
                }
                isSecondLine = true;
                image.getGraphics().drawImage(images.get(GameMap.values()[i]), x, 624, null);
            }
            x += 710;
        }

        try {
            File file = new File("./maps/image.png");
            ImageIO.write(image, "png", file);

            if(timer != null) {
                timer.cancel();
            }

            if(channelMapsMessageId == null) {
                TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
                if(channel != null) {
                    channel.sendMessage("Команда " + currentBannerTeamRole.getAsMention() +
                            " должна забанить карту! " + "Оставшееся  время для бана карты: " + 20)
                            .addFile(file).queue(message -> {
                        channelMapsMessageId = message.getId();
                        createCountdownTask(file);
                    });   
                }
            } else {
                createCountdownTask(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таймер обратного отсчета
     * @param file Файл изображения
     */
    private void createCountdownTask(File file) {
        AtomicInteger time =  new AtomicInteger(20);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
                if(channel == null) {
                    cancel();
                    return;
                }
                
                if(gameMaps.size() == 1) {
                    if(time.get() <= 0) {
                        cancel();
                        gameCategory.createGameChannel();
                    }

                    if(time.get() == 20) {
                        channel.editMessageById(channelMapsMessageId, "Карта игры успешно выбрана! Название карты: " +
                                gameMaps.get(0).getName()).retainFiles(new ArrayList<>()).addFile(file).queue();
                        gameCategory.game.gameMap = gameMaps.get(0);
                    } else {
                        if(time.get() % 2 == 0) {
                            channel.editMessageById(channelMapsMessageId, "Карта игры успешно выбрана! Название карты: " +
                                    gameMaps.get(0).getName()).queue();
                        }
                    }
                } else {
                    if(time.get() <= 0) {
                        cancel();
                        banMap(gameMaps.get(new Random().nextInt(gameMaps.size())));
                    }

                    if(time.get() == 20) {
                        channel.editMessageById(channelMapsMessageId, "Команда " + currentBannerTeamRole.getAsMention() +
                                " должна забанить карту! " + "Оставшееся  время для бана карты: " + time)
                                .retainFiles(new ArrayList<>()).addFile(file).queue();
                    } else {
                        if(time.get() % 2 == 0) {
                            channel.editMessageById(channelMapsMessageId, "Команда " + currentBannerTeamRole.getAsMention() +
                                    " должна забанить карту! " + "Оставшееся  время для бана карты: " + time).queue();
                        }
                    }
                }

                time.getAndDecrement();
            }
        }, 0, 1000);
    }

    /**
     * Банит карту
     * @param gameMap Карта
     */
    public void banMap(GameMap gameMap) {
        gameMaps.remove(gameMap);
        updateMapsMessage();
    }
}