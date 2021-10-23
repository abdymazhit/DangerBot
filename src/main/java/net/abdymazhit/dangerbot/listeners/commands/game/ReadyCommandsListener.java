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
 * Команда готовности к игре
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class ReadyCommandsListener extends ListenerAdapter {

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
        if(gameCategoryManager.readyChannel == null) return;

        if(gameCategoryManager.readyChannel.channel.equals(messageChannel)) {
            String contentRaw = message.getContentRaw();
            if(contentRaw.equals("!cancel")) {
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

                if(gameCategoryManager.readyChannel.timer != null) {
                    gameCategoryManager.readyChannel.timer.cancel();
                }

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        DangerBot.getInstance().gameManager.deleteGame(gameCategoryManager.category.getId());
                    }
                }, 7000);
            }
        }
    }
}
