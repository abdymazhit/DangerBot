package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Канал выбора карты
 *
 * @version   21.09.2021
 * @author    Islam Abdymazhit
 */
public class MapChoiceChannel extends Channel {

    /** Категория игры */
    private final GameCategory gameCategory;

    /** Список доступных карт */
    public final List<GameMap> gameMaps;

    /** Роль текущей банющей команды */
    public Role currentBannerTeamRole;

    /** Id сообщения о картах */
    public String channelMapsMessageId;

    /** Таймер обратного отсчета */
    private Timer timer;

    /** Время каждого раунда бана */
    private static final int roundTime = 20;

    /**
     * Инициализирует канал выбора карты
     * @param gameCategory Категория игры
     */
    public MapChoiceChannel(GameCategory gameCategory) {
        this.gameCategory = gameCategory;
        gameMaps = new ArrayList<>();

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

        if(gameCategory.game.format.equals("4x2")) {
            Collections.addAll(gameMaps, GameMap.values4x2());
        } else if(gameCategory.game.format.equals("6x2")) {
            Collections.addAll(gameMaps, GameMap.values6x2());
        }

        MTHD.getInstance().guild.retrieveMemberById(gameCategory.game.assistantDiscordId).queue(member ->
            category.createTextChannel("map-choice").setPosition(2)
                .addPermissionOverride(firstTeamStarter, EnumSet.of(Permission.MESSAGE_WRITE), null)
                .addPermissionOverride(secondTeamStarter, EnumSet.of(Permission.MESSAGE_WRITE), null)
                .addPermissionOverride(gameCategory.firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(gameCategory.secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null)
                .queue(textChannel -> {
                    channelId = textChannel.getId();
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle("Вторая стадия игры - Выбор карты");
                    embedBuilder.setColor(3092790);
                    embedBuilder.setDescription("""
                        Начавшие поиск игры команд (%first_team% и %second_team%) должны решить какую карту будут играть!
          
                        Заблокировать карту
                        `!ban <ПОРЯДКОВЫЙ НОМЕР> или <НАЗВАНИЕ КАРТЫ>`"""
                        .replace("%first_team%", gameCategory.firstTeamRole.getAsMention())
                        .replace("%second_team%", gameCategory.secondTeamRole.getAsMention()));
                    textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                    embedBuilder.clear();
                    updateMapsMessage(textChannel);
                })
        );
    }

    /**
     * Банит карту
     * @param gameMap Карта
     */
    public void banMap(GameMap gameMap) {
        gameMaps.remove(gameMap);

        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал map-choice не существует!");
            return;
        }

        updateMapsMessage(textChannel);
    }

    /**
     * Обновляет сообщение о доступных картах
     */
    private void updateMapsMessage(TextChannel textChannel) {
        if(currentBannerTeamRole == null) {
            currentBannerTeamRole = gameCategory.firstTeamRole;
        } else {
            if(currentBannerTeamRole.equals(gameCategory.firstTeamRole)) {
                currentBannerTeamRole = gameCategory.secondTeamRole;
            } else {
                currentBannerTeamRole = gameCategory.firstTeamRole;
            }
        }

        GameMap[] maps = new GameMap[0];
        if(gameCategory.game.format.equals("4x2")) {
            maps = GameMap.values4x2();
        } else if(gameCategory.game.format.equals("6x2")) {
            maps = GameMap.values6x2();
        }

        Map<GameMap, BufferedImage> images = new HashMap<>();

        if(gameMaps.size() == 1) {
            images.put(gameMaps.get(0), gameMaps.get(0).getPickImage());
            for(GameMap gameMap : maps) {
                if(!images.containsKey(gameMap)) {
                    images.put(gameMap, gameMap.getBanImage());
                }
            }
        } else {
            for(GameMap gameMap : maps) {
                if(gameMaps.contains(gameMap)) {
                    images.put(gameMap, gameMap.getNormalImage());
                } else {
                    images.put(gameMap, gameMap.getBanImage());
                }
            }
        }

        BufferedImage image = new BufferedImage(((maps.length / 2) + 1) * 710,624 * 2, BufferedImage.TYPE_INT_ARGB);
        int x = 0;
        boolean isSecondLine = false;
        for(int i = 0; i < maps.length; i++) {
            int index = (maps.length / 2) + 1;
            if(i < index) {
                image.getGraphics().drawImage(images.get(maps[i]), x, 0, null);
            } else {
                if(!isSecondLine) {
                    x = 0;
                }
                isSecondLine = true;
                image.getGraphics().drawImage(images.get(maps[i]), x, 624, null);
            }
            x += 710;
        }

        try {
            if(timer != null) {
                timer.cancel();
            }

            File file = new File("./maps/image.png");
            ImageIO.write(image, "png", file);
            createCountdownTask(textChannel, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таймер обратного отсчета
     * @param file Файл изображения
     */
    private void createCountdownTask(TextChannel textChannel, File file) {
        AtomicInteger time =  new AtomicInteger(roundTime);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(gameMaps.size() == 1) {
                    if(time.get() <= 0) {
                        gameCategory.createGameChannel();
                        cancel();
                    }

                    if(time.get() == roundTime) {
                        gameCategory.setGameState(GameState.GAME_CREATION);
                        gameCategory.setGameMap(gameMaps.get(0));
                        gameCategory.game.gameMap = gameMaps.get(0);

                        textChannel.editMessageById(channelMapsMessageId, """
                            Карта игры успешно выбрана! Название карты: %map_name%. Переход к созданию игры..."""
                            .replace("%map_name%", gameMaps.get(0).getName()))
                            .retainFiles(new ArrayList<>()).addFile(file).queue();
                        MTHD.getInstance().liveGamesChannel.updateLiveGamesMessages();
                    }
                } else {
                    String mapsMessageText = """
                        Команда %team% должна забанить карту!
                            
                        Оставшееся время для бана карты: `%time% сек.`"""
                        .replace("%team%", currentBannerTeamRole.getAsMention())
                        .replace("%time%", String.valueOf(time));

                    if(channelMapsMessageId == null) {
                        try {
                            Message message = textChannel.sendMessage(mapsMessageText).retainFiles(new ArrayList<>()).addFile(file).submit().get();
                            channelMapsMessageId = message.getId();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if(time.get() <= 0) {
                            banMap(gameMaps.get(new Random().nextInt(gameMaps.size())));
                            cancel();
                        }

                        if(time.get() == roundTime) {
                            textChannel.editMessageById(channelMapsMessageId, mapsMessageText)
                                .retainFiles(new ArrayList<>()).addFile(file).queue(message -> channelMapsMessageId = message.getId());
                        } else {
                            if(time.get() % 2 == 0) {
                                textChannel.editMessageById(channelMapsMessageId, mapsMessageText).queue();
                            }
                        }
                    }
                }
                time.getAndDecrement();
            }
        }, 0, 1000);
    }
}