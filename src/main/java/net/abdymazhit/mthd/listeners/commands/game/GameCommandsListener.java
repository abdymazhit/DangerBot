package net.abdymazhit.mthd.listeners.commands.game;

import java.util.Timer;
import java.util.TimerTask;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.UserRole;
import net.abdymazhit.mthd.game.GameCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Команда отмены игры
 *
 * @version   22.09.2021
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

        for(GameCategory gameCategory : MTHD.getInstance().gameManager.getGameCategories()) {
            processGame(gameCategory, messageChannel, message, assistant);
        }
    }

    /**
     * Обрабыватывает действия игры
     * @param gameCategory Категория игры
     * @param messageChannel Канал сообщений
     * @param message Сообщение
     * @param assistant Помощник игры
     */
    private void processGame(GameCategory gameCategory, MessageChannel messageChannel, Message message, Member assistant) {
        if(gameCategory.gameChannel == null) return;
        if(gameCategory.gameChannel.channelId.equals(messageChannel.getId())) {
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
                    if(cancellerId != gameCategory.game.assistantId) {
                        message.reply("Ошибка! Только помощник этой игры может начать игру!").queue();
                        return;
                    }
                }

                message.reply("Вы успешно начали игру!").queue();
                gameCategory.setGameState(GameState.GAME);
                gameCategory.gameChannel.timer.cancel();
                MTHD.getInstance().liveGamesManager.addLiveGame(gameCategory.game);
                gameCategory.gameChannel.sendGameStartMessage();
            } else if(contentRaw.equals("!cancel")) {
                if(!assistant.getRoles().contains(UserRole.ADMIN.getRole())) {
                    message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                    return;
                }

                int cancellerId = MTHD.getInstance().database.getUserId(assistant.getId());
                if(cancellerId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                message.reply("Вы успешно отменили игру!").queue();
                MTHD.getInstance().gameManager.deleteGame(gameCategory.game);

                if(gameCategory.gameChannel.timer != null) {
                    gameCategory.gameChannel.timer.cancel();
                }
                MTHD.getInstance().liveGamesManager.removeLiveGame(gameCategory.game);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        MTHD.getInstance().gameManager.deleteGame(gameCategory.categoryId);
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

                String errorMessage = MTHD.getInstance().liveGamesManager.finishMatch(matchId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                if(gameCategory.gameChannel.timer != null) {
                    gameCategory.gameChannel.timer.cancel();
                }
                MTHD.getInstance().liveGamesManager.removeLiveGame(gameCategory.game);
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }
}
