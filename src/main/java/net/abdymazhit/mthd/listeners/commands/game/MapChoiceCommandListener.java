package net.abdymazhit.mthd.listeners.commands.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.Rating;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Команда выбора карты
 *
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class MapChoiceCommandListener extends ListenerAdapter {

    /**
     * Событие получения команды
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member captain = event.getMember();

        if(captain == null) return;
        if(event.getAuthor().isBot()) return;

        for(GameCategoryManager gameCategoryManager : MTHD.getInstance().gameManager.gameCategories) {
            choiceMap(gameCategoryManager, messageChannel, message, captain);
        }
    }

    /**
     * Выбирает карту
     * @param gameCategoryManager Категория игры
     * @param messageChannel Канал сообщений
     * @param message Сообщение
     * @param captain Капитан команды
     */
    private void choiceMap(GameCategoryManager gameCategoryManager, MessageChannel messageChannel, Message message, Member captain) {
        if(gameCategoryManager.mapChoiceChannel == null) return;

        if(gameCategoryManager.mapChoiceChannel.channelId.equals(messageChannel.getId())) {
            String contentRaw = message.getContentRaw();
            if(contentRaw.startsWith("!ban")) {
                String[] command = contentRaw.split(" ");

                if(command.length == 1) {
                    message.reply("Ошибка! Укажите имя игрока!").queue();
                    return;
                }

                if(command.length > 2) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                if(gameCategoryManager.mapChoiceChannel.isMapsMessageSending) {
                    message.reply("Ошибка! Дождитесь загрузки фотографии доступных карт!").queue();
                    return;
                }

                String mapName = command[1];

                GameMap banningGameMap = null;
                if(gameCategoryManager.game.format.equals("4x2")) {
                    for(GameMap gameMap : GameMap.values4x2()) {
                        if(gameMap.getName().equalsIgnoreCase(mapName)) {
                            banningGameMap = gameMap;
                            break;
                        } else {
                            try{
                                if(gameMap.getId() == Integer.parseInt(mapName)) {
                                    banningGameMap = gameMap;
                                    break;
                                }
                            } catch(NumberFormatException ignored) { }
                        }
                    }
                } else if(gameCategoryManager.game.format.equals("6x2")) {
                    for(GameMap gameMap : GameMap.values6x2()) {
                        if(gameMap.getName().equalsIgnoreCase(mapName)) {
                            banningGameMap = gameMap;
                            break;
                        } else {
                            try{
                                if(gameMap.getId() == Integer.parseInt(mapName)) {
                                    banningGameMap = gameMap;
                                    break;
                                }
                            } catch(NumberFormatException ignored) { }
                        }
                    }
                }

                if(banningGameMap == null) {
                    message.reply("Ошибка! Такой карты не существует!").queue();
                    return;
                }

                if(!gameCategoryManager.mapChoiceChannel.gameMaps.contains(banningGameMap)) {
                    message.reply("Ошибка! Карта уже забанена!").queue();
                    return;
                }

                if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
                    if(!captain.getRoles().contains(gameCategoryManager.firstTeamRole) &&
                       !captain.getRoles().contains(gameCategoryManager.secondTeamRole)) {
                        message.reply("Ошибка! Вы не являетесь участником или лидером участвующей в игре команды!").queue();
                        return;
                    }

                    if(!captain.getRoles().contains(gameCategoryManager.mapChoiceChannel.currentBannerTeamRole)) {
                        message.reply("Ошибка! Сейчас не ваша очередь бана!").queue();
                        return;
                    }
                } else {
                    if(!captain.equals(gameCategoryManager.mapChoiceChannel.currentBannerCaptain)) {
                        message.reply("Ошибка! Сейчас не ваша очередь бана или Вы не являетесь капитаном команды!").queue();
                        return;
                    }
                }

                if(gameCategoryManager.mapChoiceChannel.gameMaps.size() == 1) {
                    message.reply("Ошибка! Все карты забанены!").queue();
                    return;
                }

                int captainId = MTHD.getInstance().database.getUserId(captain.getId());
                if(captainId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                if(gameCategoryManager.game.rating.equals(Rating.TEAM_RATING)) {
                    if(isNotTeamCaptain(captainId)) {
                        message.reply("Ошибка! Только начавший игру может банить карты!").queue();
                        return;
                    }
                } else {
                    if(isNotSingleCaptain(captainId)) {
                        message.reply("Ошибка! Вы не являетесь капитаном команды!").queue();
                        return;
                    }
                }

                if(!gameCategoryManager.game.gameState.equals(GameState.MAP_CHOICE)) {
                    message.reply("Ошибка! Стадия выбора карты закончена!").queue();
                    return;
                }

                message.reply("Вы успешно забанили карту!").queue();
                gameCategoryManager.mapChoiceChannel.banMap(banningGameMap);
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }

    /**
     * Проверяет, является ли пользователь не начавшим игру
     * @param captainId Id капитана команды
     * @return Значение, является ли пользователь не начавшим игру
     */
    private boolean isNotTeamCaptain(int captainId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT 1 FROM team_live_games WHERE first_team_captain_id = ? OR second_team_captain_id = ?;");
            preparedStatement.setInt(1, captainId);
            preparedStatement.setInt(2, captainId);
            ResultSet resultSet = preparedStatement.executeQuery();
            return !resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Проверяет, является ли пользователь не капитаном команды
     * @param captainId Id капитана команды
     * @return Значение, является ли пользователь не капитаном команды
     */
    private boolean isNotSingleCaptain(int captainId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT 1 FROM single_live_games WHERE first_team_captain_id = ? OR second_team_captain_id = ?;");
            preparedStatement.setInt(1, captainId);
            preparedStatement.setInt(2, captainId);
            ResultSet resultSet = preparedStatement.executeQuery();
            return !resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}
