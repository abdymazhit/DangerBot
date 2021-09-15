package net.abdymazhit.mthd.listeners.commands.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.game.GameCategory;
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
 * @version   15.09.2021
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
        Member starter = event.getMember();

        if(starter == null) return;
        if(event.getAuthor().isBot()) return;

        for(GameCategory gameCategory : MTHD.getInstance().gameManager.getGameCategories()) {
            if(gameCategory.mapChoiceChannel == null) return;

            if(gameCategory.mapChoiceChannel.channel.equals(messageChannel)) {
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

                    String mapName = command[1];

                    GameMap banningGameMap = null;
                    for(GameMap gameMap : GameMap.values()) {
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

                    if(banningGameMap == null) {
                        message.reply("Ошибка! Такой карты не существует!").queue();
                        return;
                    }

                    if(!gameCategory.mapChoiceChannel.gameMaps.contains(banningGameMap)) {
                        message.reply("Ошибка! Карта уже забанена!").queue();
                        return;
                    }

                    if(!starter.getRoles().contains(gameCategory.firstTeamRole) &&
                            !starter.getRoles().contains(gameCategory.secondTeamRole)) {
                        message.reply("Ошибка! Вы не являетесь участником или лидером участвующей в игре команды!").queue();
                        return;
                    }

                    if(!starter.getRoles().contains(gameCategory.mapChoiceChannel.currentBannerTeamRole)) {
                        message.reply("Ошибка! Сейчас не ваша очередь бана!").queue();
                        return;
                    }

                    int starterId = MTHD.getInstance().database.getUserId(starter.getId());
                    if(starterId < 0) {
                        message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                        return;
                    }

                    if(isNotStarter(starterId)) {
                        message.reply("Ошибка! Только начавший игру может банить карты!").queue();
                        return;
                    }

                    int starterTeamId = MTHD.getInstance().database.getUserTeamId(starterId);
                    if(starterTeamId < 0) {
                        message.reply("Ошибка! Вы не являетесь участником или лидером какой-либо команды!").queue();
                        return;
                    }

                    gameCategory.mapChoiceChannel.banMap(banningGameMap);
                    message.reply("Вы успешно забанили карту!").queue();
                } else {
                    message.reply("Ошибка! Неверная команда!").queue();
                }
                break;
            }
        }
    }

    /**
     * Проверяет, является ли пользователь не начавшим игру
     * @param starterId Id начавшего игру
     * @return Значение, является ли пользователь не начавшим игру
     */
    private boolean isNotStarter(int starterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT 1 FROM live_games WHERE first_team_starter_id = ? OR second_team_starter_id = ?;");
            preparedStatement.setInt(1, starterId);
            preparedStatement.setInt(2, starterId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();
            return !resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}
