package net.abdymazhit.dangerbot.listeners.commands.game;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.GameState;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.abdymazhit.dangerbot.managers.GameCategoryManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Команда выбора игроков в команду
 *
 * @version   23.10.2021
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
        Member member = event.getMember();

        if(member == null) return;
        if(event.getAuthor().isBot()) return;

        for(GameCategoryManager gameCategoryManager : DangerBot.getInstance().gameManager.gameCategories) {
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
        if(gameCategoryManager.playersPickChannel == null) return;

        if(gameCategoryManager.playersPickChannel.channel.equals(messageChannel)) {
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

                int captainId = DangerBot.getInstance().database.getUserId(member.getId());
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

                if(!member.equals(gameCategoryManager.playersPickChannel.currentPickerCaptain)) {
                    message.reply("Ошибка! Сейчас не ваша очередь бана или Вы не являетесь капитаном команды!").queue();
                    return;
                }

                UserAccount playerAccount = DangerBot.getInstance().database.getUserAccount(playerName);
                if(playerAccount == null) {
                    message.reply("Ошибка! Игрок не зарегистрирован на сервере!").queue();
                    return;
                }

                boolean isPlayerAvailable = DangerBot.getInstance().database.getSingleGamePlayer(playerAccount.id, gameId);
                if(!isPlayerAvailable) {
                    message.reply("Ошибка! Игрок не участвует в этой игре или уже выбран в команду!").queue();
                    return;
                }

                if(member == gameCategoryManager.game.firstTeamInfo.captain.member) {
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
                } else if(member == gameCategoryManager.game.secondTeamInfo.captain.member) {
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

                if(!gameCategoryManager.game.gameState.equals(GameState.PLAYERS_CHOICE)) {
                    message.reply("Ошибка! Стадия выбора игроков закончена!").queue();
                    return;
                }

                String errorMessage;
                if(member == gameCategoryManager.game.firstTeamInfo.captain.member) {
                    errorMessage = DangerBot.getInstance().database.addPlayerToTeam(0, playerAccount.id);
                } else {
                    errorMessage = DangerBot.getInstance().database.addPlayerToTeam(1, playerAccount.id);
                }
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                message.reply("Вы успешно добавили игрока в команду!").queue();

                if(member == gameCategoryManager.game.firstTeamInfo.captain.member) {
                    gameCategoryManager.playersPickChannel.pickPlayer(playerName, 0);
                } else {
                    gameCategoryManager.playersPickChannel.pickPlayer(playerName, 1);
                }
            } else if(contentRaw.equals("!cancel")) {
                if(!member.getRoles().contains(UserRole.ADMIN.getRole()) && !member.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                    message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                    return;
                }

                if(member.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                    int cancellerId = DangerBot.getInstance().database.getUserId(member.getId());
                    if(cancellerId < 0) {
                        message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                        return;
                    }

                    int liveGameId = Integer.parseInt(gameCategoryManager.category.getName().replace("Game-", ""));
                    boolean isAssistant = DangerBot.getInstance().database.isAssistant(liveGameId, cancellerId);
                    if(!isAssistant) {
                        message.reply("Ошибка! Вы не являетесь помощником этой игры!").queue();
                        return;
                    }
                }

                message.reply("Вы успешно отменили игру!").queue();
                DangerBot.getInstance().gameManager.deleteGame(gameCategoryManager.game);

                if(gameCategoryManager.playersPickChannel.timer != null) {
                    gameCategoryManager.playersPickChannel.timer.cancel();
                }
                DangerBot.getInstance().liveGamesManager.removeLiveGame(gameCategoryManager.game);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        DangerBot.getInstance().gameManager.deleteGame(gameCategoryManager.category.getId());
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
    private int getCaptainGameId(int captainId) {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
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
