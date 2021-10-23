package net.abdymazhit.dangerbot.listeners.commands.game;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.abdymazhit.dangerbot.managers.GameCategoryManager;
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
 * @version   23.10.2021
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

        for(GameCategoryManager gameCategoryManager : DangerBot.getInstance().gameManager.gameCategories) {
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

        if(gameCategoryManager.gameChannel.channel.equals(messageChannel)) {
            String contentRaw = message.getContentRaw();
            if(contentRaw.equals("!cancel")) {
                if(!assistant.getRoles().contains(UserRole.ADMIN.getRole()) && !assistant.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                    message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                    return;
                }

                if(assistant.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                    int cancellerId = DangerBot.getInstance().database.getUserId(assistant.getId());
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

                if(gameCategoryManager.gameChannel.timer != null) {
                    gameCategoryManager.gameChannel.timer.cancel();
                }
                DangerBot.getInstance().liveGamesManager.removeLiveGame(gameCategoryManager.game);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        DangerBot.getInstance().gameManager.deleteGame(gameCategoryManager.category.getId());
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

                int finisherId = DangerBot.getInstance().database.getUserId(assistant.getId());
                if(finisherId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                DangerBot.getInstance().liveGamesManager.addLiveGame(gameCategoryManager.game);

                String errorMessage = DangerBot.getInstance().liveGamesManager.finishMatch(matchId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                if(gameCategoryManager.gameChannel.timer != null) {
                    gameCategoryManager.gameChannel.timer.cancel();
                }
                DangerBot.getInstance().liveGamesManager.removeLiveGame(gameCategoryManager.game);
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }
}
