package net.abdymazhit.mthd.listeners.commands.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameState;
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
 * Команда выбора игроков в команду
 *
 * @version   26.09.2021
 * @author    Islam Abdymazhit
 */
public class PlayersPickCommandListener extends ListenerAdapter {

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
            choicePlayer(gameCategoryManager, messageChannel, message, captain);
        }
    }

    /**
     * Выбирает игрока
     * @param gameCategoryManager Категория игры
     * @param messageChannel Канал сообщений
     * @param message Сообщение
     * @param captain Начавщий игру
     */
    private void choicePlayer(GameCategoryManager gameCategoryManager, MessageChannel messageChannel, Message message, Member captain) {
        if(gameCategoryManager.playersPickChannel == null) return;
        if(gameCategoryManager.playersPickChannel.channelId == null) return;

        if(gameCategoryManager.playersPickChannel.channelId.equals(messageChannel.getId())) {
            String contentRaw = message.getContentRaw();
            if(contentRaw.startsWith("!pick")) {
                String[] command = contentRaw.split(" ");

                if(command.length == 1) {
                    message.reply("Ошибка! Укажите имя игрока!").queue();
                    return;
                }

                if(command.length > 2) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                int captainId = MTHD.getInstance().database.getUserId(captain.getId());
                if(captainId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                int gameId = getCaptainGameId(captainId);
                if(gameId < 0) {
                    message.reply("Ошибка! Только капитан команды может выбирать игроков!").queue();
                    return;
                }

                String playerName = command[1];

                UserAccount playerAccount = MTHD.getInstance().database.getUserIdAndDiscordId(playerName);
                if(playerAccount == null) {
                    message.reply("Ошибка! Игрок не зарегистрирован на сервере!").queue();
                    return;
                }

                boolean isPlayerAvailable = MTHD.getInstance().database.getSingleGamePlayer(playerAccount.id, gameId);
                if(!isPlayerAvailable) {
                    message.reply("Ошибка! Игрок не участвует в этой игре или уже выбран в команду!").queue();
                    return;
                }

                if(captain == gameCategoryManager.game.firstTeamCaptainMember) {
                    if(gameCategoryManager.game.format.equals("4x2")) {
                        if(gameCategoryManager.game.firstTeamPlayers.size() > 3) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    } else if(gameCategoryManager.game.format.equals("6x2")) {
                        if(gameCategoryManager.game.firstTeamPlayers.size() > 5) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    }
                } else if(captain == gameCategoryManager.game.secondTeamCaptainMember) {
                    if(gameCategoryManager.game.format.equals("4x2")) {
                        if(gameCategoryManager.game.secondTeamPlayers.size() > 3) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    } else if(gameCategoryManager.game.format.equals("6x2")) {
                        if(gameCategoryManager.game.secondTeamPlayers.size() > 5) {
                            message.reply("Ошибка! Ваша команда имеет максимальное количество игроков!").queue();
                            return;
                        }
                    }
                }

                if(!gameCategoryManager.game.gameState.equals(GameState.PLAYERS_CHOICE)) {
                    message.reply("Ошибка! Стадия выбора игроков закончена!").queue();
                    return;
                }

                String errorMessage;
                if(captain == gameCategoryManager.game.firstTeamCaptainMember) {
                    errorMessage = MTHD.getInstance().database.addPlayerToTeam(0, playerAccount.id);
                } else {
                    errorMessage = MTHD.getInstance().database.addPlayerToTeam(1, playerAccount.id);
                }
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }


                message.reply("Вы успешно добавили игрока в команду!").queue();

                if(captain == gameCategoryManager.game.firstTeamCaptainMember) {
                    MTHD.getInstance().guild.retrieveMemberById(playerAccount.discordId).queue(gameCategoryManager::addToFirstTeamVoiceChannel);
                    gameCategoryManager.playersPickChannel.pickPlayer(playerName, 0);
                } else {
                    MTHD.getInstance().guild.retrieveMemberById(playerAccount.discordId).queue(gameCategoryManager::addToSecondTeamVoiceChannel);
                    gameCategoryManager.playersPickChannel.pickPlayer(playerName, 1);
                }
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
    private int getCaptainGameId(int captainId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id FROM single_live_games WHERE first_team_captain_id = ? OR second_team_captain_id = ?;");
            preparedStatement.setInt(1, captainId);
            preparedStatement.setInt(2, captainId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
