package net.abdymazhit.mthd.listeners.commands.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Команда отмены игры
 *
 * @version   05.10.2021
 * @author    Islam Abdymazhit
 */
public class GameCommandsListener extends ListenerAdapter {

    /**
     * Событие получения команды
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member assistant = event.getMember();

        if(assistant == null) return;
        if(event.getAuthor().isBot()) return;

        for(GameCategoryManager gameCategoryManager : MTHD.getInstance().gameManager.gameCategories) {
            processGame(gameCategoryManager, messageChannel, message, assistant);
        }
    }

    /**
     * Обрабыватывает действия игры
     * @param gameCategoryManager Категория игры
     * @param messageChannel Канал сообщений
     * @param message Сообщение
     * @param assistant Помощник игры
     */
    private void processGame(GameCategoryManager gameCategoryManager, MessageChannel messageChannel, Message message, Member assistant) {
        if(gameCategoryManager.gameChannel == null) return;
        if(gameCategoryManager.gameChannel.channelId == null) return;

        if(gameCategoryManager.gameChannel.channelId.equals(messageChannel.getId())) {
            String contentRaw = message.getContentRaw();
            if(contentRaw.equals("!start")) {
                if(!assistant.getRoles().contains(UserRole.ADMIN.getRole()) &&
                   !assistant.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                    message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                    return;
                }

                int cancellerId = MTHD.getInstance().database.getUserId(assistant.getId());
                if(cancellerId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                if(!assistant.getRoles().contains(UserRole.ADMIN.getRole())) {
                    if(cancellerId != gameCategoryManager.game.assistantAccount.id) {
                        message.reply("Ошибка! Только помощник этой игры может начать игру!").queue();
                        return;
                    }
                }

                message.reply("Вы успешно начали игру!").queue();
                gameCategoryManager.setGameState(GameState.GAME);
                gameCategoryManager.gameChannel.timer.cancel();
                MTHD.getInstance().liveGamesManager.addLiveGame(gameCategoryManager.game);
                gameCategoryManager.gameChannel.sendGameStartMessage();
            } else if(contentRaw.equals("!cancel")) {
                if(!assistant.getRoles().contains(UserRole.ADMIN.getRole()) && !assistant.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                    message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                    return;
                }

                int cancellerId = MTHD.getInstance().database.getUserId(assistant.getId());
                if(cancellerId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                message.reply("Вы успешно отменили игру!").queue();
                MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.game);

                if(gameCategoryManager.gameChannel.timer != null) {
                    gameCategoryManager.gameChannel.timer.cancel();
                }
                MTHD.getInstance().liveGamesManager.removeLiveGame(gameCategoryManager.game);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        MTHD.getInstance().gameManager.deleteGame(gameCategoryManager.categoryId);
                    }
                }, 7000);
            } else if(contentRaw.startsWith("!finish")) {
                String[] command = message.getContentRaw().split(" ");

                if(command.length == 1) {
                    message.reply("Ошибка! Укажите id матча!").queue();
                    return;
                }

                if(command.length > 2) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                if(!assistant.getRoles().contains(UserRole.ADMIN.getRole()) &&
                   !assistant.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                    message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                    return;
                }

                String matchId = command[1];

                int finisherId = MTHD.getInstance().database.getUserId(assistant.getId());
                if(finisherId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                MTHD.getInstance().liveGamesManager.addLiveGame(gameCategoryManager.game);

                String errorMessage = MTHD.getInstance().liveGamesManager.finishMatch(matchId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                if(gameCategoryManager.gameChannel.timer != null) {
                    gameCategoryManager.gameChannel.timer.cancel();
                }
                MTHD.getInstance().liveGamesManager.removeLiveGame(gameCategoryManager.game);
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }
}
