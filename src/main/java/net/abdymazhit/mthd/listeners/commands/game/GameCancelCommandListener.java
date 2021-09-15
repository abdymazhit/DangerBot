package net.abdymazhit.mthd.listeners.commands.game;

import net.abdymazhit.mthd.MTHD;
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
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class GameCancelCommandListener extends ListenerAdapter {

    /**
     * Событие получения команды
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member canceller = event.getMember();

        if(canceller == null) return;
        if(event.getAuthor().isBot()) return;

        for(GameCategory gameCategory : MTHD.getInstance().gameManager.getGameCategories()) {
            if(gameCategory.gameChannel == null) return;

            if(gameCategory.gameChannel.channelId.equals(messageChannel.getId())) {
                String contentRaw = message.getContentRaw();
                if(contentRaw.equals("!cancel")) {
                    if(!canceller.getRoles().contains(UserRole.ADMIN.getRole()) &&
                            !canceller.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                        message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                        return;
                    }

                    int cancellerId = MTHD.getInstance().database.getUserId(canceller.getId());
                    if(cancellerId < 0) {
                        message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                        return;
                    }

                    if(!canceller.getRoles().contains(UserRole.ADMIN.getRole())) {
                        if(cancellerId == gameCategory.game.assistantId) {
                            message.reply("Ошибка! Только помощник этой игры может отменить игру!").queue();
                            return;
                        }
                    }

                    message.reply("Вы успешно отменили игру!").queue();
                    MTHD.getInstance().gameManager.deleteGame(gameCategory.game);
                    MTHD.getInstance().gameManager.deleteGame(gameCategory.categoryId);
                } else {
                    message.reply("Ошибка! Неверная команда!").queue();
                }
                break;
            }
        }
    }
}
