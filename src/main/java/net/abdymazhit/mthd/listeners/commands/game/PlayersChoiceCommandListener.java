package net.abdymazhit.mthd.listeners.commands.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.UserRole;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Команда выбора игроков на игру
 *
 * @version   22.10.2021
 * @author    Islam Abdymazhit
 */
public class PlayersChoiceCommandListener extends ListenerAdapter {

    /**
     * Событие получения команды
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member member = event.getMember();

        if(member == null) return;
        if(event.getAuthor().isBot()) return;

        for(GameCategoryManager gameCategoryManager : MTHD.getInstance().gameManager.gameCategories) {
            choicePlayer(gameCategoryManager, messageChannel, message, member);
        }
    }

    /**
     * Выбирает игрока
     * @param gameCategoryManager Категория игры
     * @param messageChannel Канал сообщений
     * @param message Сообщение
     * @param member Написавший команду
     */
    private void choicePlayer(GameCategoryManager gameCategoryManager, MessageChannel messageChannel, Message message, Member member) {
        if(gameCategoryManager.playersChoiceChannel == null) return;

        if(gameCategoryManager.playersChoiceChannel.channel.equals(messageChannel)) {
            String contentRaw = message.getContentRaw();
            if(contentRaw.startsWith("!add")) {
                String[] command = contentRaw.split(" ");

                if(command.length == 1) {
                    message.reply("Ошибка! Укажите имя игрока!").queue();
                    return;
                }

                if(command.length > 2) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                int captainId = MTHD.getInstance().database.getUserId(member.getId());
                if(captainId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                if(isNotCaptain(captainId)) {
                    message.reply("Ошибка! Только начавший игру может устанавливать игроков на игру!").queue();
                    return;
                }

                if(!member.getRoles().contains(gameCategoryManager.game.firstTeamInfo.role) &&
                   !member.getRoles().contains(gameCategoryManager.game.secondTeamInfo.role)) {
                    message.reply("Ошибка! Вы не являетесь участником или лидером участвующей в игре команды!").queue();
                    return;
                }

                String playerName = command[1];

                int captainTeamId = MTHD.getInstance().database.getUserTeamId(captainId);
                if(captainTeamId < 0) {
                    message.reply("Ошибка! Вы не являетесь участником или лидером какой-либо команды!").queue();
                    return;
                }

                UserAccount playerAccount = MTHD.getInstance().database.getUserAccount(playerName);
                if(playerAccount == null) {
                    message.reply("Ошибка! Игрок не зарегистрирован на сервере!").queue();
                    return;
                }

                int playerTeamId = MTHD.getInstance().database.getUserTeamId(playerAccount.id);
                if(playerTeamId < 0) {
                    message.reply("Ошибка! Игрок не является участником или лидером какой-либо команды!").queue();
                    return;
                }

                if(playerTeamId == gameCategoryManager.game.firstTeamInfo.id) {
                    if(gameCategoryManager.game.format.equals("4x2")) {
                        if(gameCategoryManager.game.firstTeamInfo.members.size() > 3) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    } else if(gameCategoryManager.game.format.equals("6x2")) {
                        if(gameCategoryManager.game.firstTeamInfo.members.size() > 5) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    }
                } else if(playerTeamId == gameCategoryManager.game.secondTeamInfo.id) {
                    if(gameCategoryManager.game.format.equals("4x2")) {
                        if(gameCategoryManager.game.secondTeamInfo.members.size() > 3) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    } else if(gameCategoryManager.game.format.equals("6x2")) {
                        if(gameCategoryManager.game.secondTeamInfo.members.size() > 5) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    }
                }

                if(captainTeamId != playerTeamId) {
                    message.reply("Ошибка! Вы можете добавлять только игроков своей команды!").queue();
                    return;
                }

                if(!gameCategoryManager.game.gameState.equals(GameState.PLAYERS_CHOICE)) {
                    message.reply("Ошибка! Стадия выбора игроков закончена!").queue();
                    return;
                }

                String errorMessage = addPlayerToGame(playerTeamId, playerAccount.id);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                if(gameCategoryManager.game.firstTeamInfo.captain.id == captainId) {
                    gameCategoryManager.game.firstTeamInfo.members.add(playerAccount);
                } else {
                    gameCategoryManager.game.secondTeamInfo.members.add(playerAccount);
                }

                message.reply("Вы успешно добавили игрока в игру!").queue();
                gameCategoryManager.playersChoiceChannel.updateGamePlayersMessage();
            } else if(contentRaw.startsWith("!delete")) {
                String[] command = contentRaw.split(" ");

                if(command.length == 1) {
                    message.reply("Ошибка! Укажите имя игрока!").queue();
                    return;
                }

                if(command.length > 2) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                int captainId = MTHD.getInstance().database.getUserId(member.getId());
                if(captainId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                if(isNotCaptain(captainId)) {
                    message.reply("Ошибка! Только начавший игру может устанавливать игроков на игру!").queue();
                    return;
                }

                if(!member.getRoles().contains(gameCategoryManager.game.firstTeamInfo.role) &&
                   !member.getRoles().contains(gameCategoryManager.game.secondTeamInfo.role)) {
                    message.reply("Ошибка! Вы не являетесь участником или лидером участвующей в игре команды!").queue();
                    return;
                }

                String playerName = command[1];

                int captainTeamId = MTHD.getInstance().database.getUserTeamId(captainId);
                if(captainTeamId < 0) {
                    message.reply("Ошибка! Вы не являетесь участником или лидером какой-либо команды!").queue();
                    return;
                }

                UserAccount playerAccount = MTHD.getInstance().database.getUserAccount(playerName);
                if(playerAccount == null) {
                    message.reply("Ошибка! Игрок не зарегистрирован на сервере!").queue();
                    return;
                }

                int playerTeamId = MTHD.getInstance().database.getUserTeamId(playerAccount.id);
                if(playerTeamId < 0) {
                    message.reply("Ошибка! Игрок не является участником или лидером какой-либо команды!").queue();
                    return;
                }

                if(captainTeamId != playerTeamId) {
                    message.reply("Ошибка! Вы можете удалять только игроков своей команды!").queue();
                    return;
                }

                if(!gameCategoryManager.game.gameState.equals(GameState.PLAYERS_CHOICE)) {
                    message.reply("Ошибка! Стадия выбора игроков закончена!").queue();
                    return;
                }

                String errorMessage = removePlayerFromGame(playerTeamId, playerAccount.id);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                if(gameCategoryManager.game.firstTeamInfo.captain.id == captainId) {
                    gameCategoryManager.game.firstTeamInfo.members.add(playerAccount);
                } else {
                    gameCategoryManager.game.secondTeamInfo.members.add(playerAccount);
                }

                message.reply("Вы успешно удалили игрока из игры!").queue();
                gameCategoryManager.playersChoiceChannel.updateGamePlayersMessage();
            }  else if(contentRaw.equals("!cancel")) {
                if(!member.getRoles().contains(UserRole.ADMIN.getRole()) && !member.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                    message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                    return;
                }

                if(member.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                    int cancellerId = MTHD.getInstance().database.getUserId(member.getId());
                    if(cancellerId < 0) {
                        message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                        return;
                    }

                    Category category = MTHD.getInstance().guild.getCategoryById(gameCategoryManager.category.getId());
                    if(category == null) {
                        message.reply("Ошибка! Категория игры не найдена!").queue();
                        return;
                    }

                    int liveGameId = Integer.parseInt(category.getName().replace("Game-", ""));
                    boolean isAssistant = MTHD.getInstance().database.isAssistant(liveGameId, cancellerId);
                    if(!isAssistant) {
                        message.reply("Ошибка! Вы не являетесь помощником этой игры!").queue();
                        return;
                    }
                }

                message.reply("Вы успешно отменили игру!").queue();
                MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.game);

                if(gameCategoryManager.playersChoiceChannel.timer != null) {
                    gameCategoryManager.playersChoiceChannel.timer.cancel();
                }
                MTHD.getInstance().liveGamesManager.removeLiveGame(gameCategoryManager.game);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.category.getId());
                    }
                }, 7000);
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }

    /**
     * Проверяет, является ли пользователь не начавшим игру
     * @param captainId Id начавшего игру
     * @return Значение, является ли пользователь не начавшим игру
     */
    private boolean isNotCaptain(int captainId) {
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
     * Добавляет игрока в список участвующих в игре игроков
     * @param teamId Id команды
     * @param playerId Id игрока
     * @return Текст ошибки добавления
     */
    private String addPlayerToGame(int teamId, int playerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement createStatement = connection.prepareStatement(
                    "INSERT INTO team_live_games_players (team_id, player_id) SELECT ?, ? " +
                    "WHERE NOT EXISTS (SELECT 1 FROM team_live_games_players WHERE team_id = ? AND player_id = ?);", Statement.RETURN_GENERATED_KEYS);
            createStatement.setInt(1, teamId);
            createStatement.setInt(2, playerId);
            createStatement.setInt(3, teamId);
            createStatement.setInt(4, playerId);
            createStatement.executeUpdate();
            ResultSet createResultSet = createStatement.getGeneratedKeys();
            if(createResultSet.next()) {
                // Вернуть значение, что игрок успешно добавлен в список участвующих в игре игроков
                return null;
            } else {
                return "Ошибка! Игрок уже участвует в игре!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении Вас в список участвующих в игре игроков! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Удаляет игрока из списка участвующих в игре игроков
     * @param teamId Id команды
     * @param playerId Id игрока
     * @return Текст ошибки удаления
     */
    private String removePlayerFromGame(int teamId, int playerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();

            PreparedStatement selectStatement = connection.prepareStatement(
                    "SELECT 1 FROM team_live_games_players WHERE team_id = ? AND player_id = ?;");
            selectStatement.setInt(1, teamId);
            selectStatement.setInt(2, playerId);
            ResultSet selectResultSet = selectStatement.executeQuery();
            if(!selectResultSet.next()) {
                return "Ошибка! Игрок не участвует в игре!";
            }

            PreparedStatement createStatement = connection.prepareStatement(
                    "DELETE FROM team_live_games_players WHERE team_id = ? AND player_id = ?;");
            createStatement.setInt(1, teamId);
            createStatement.setInt(2, playerId);
            createStatement.executeUpdate();

            // Вернуть значение, что игрок успешно удален из списока участвующих в игре игроков
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении Вас из списка участвующих в игре игроков! Свяжитесь с разработчиком бота!";
        }
    }
}
