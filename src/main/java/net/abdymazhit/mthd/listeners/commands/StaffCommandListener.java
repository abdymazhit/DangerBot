package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Команда персонала
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class StaffCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщений
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member assistant = event.getMember();

        if(assistant == null) return;
        if(event.getAuthor().isBot()) return;

        if(MTHD.getInstance().staffChannel.channelId.equals(messageChannel.getId())) {
            String contentRaw = message.getContentRaw();

            if(!assistant.getRoles().contains(UserRole.ASSISTANT.getRole()) &&
                    !assistant.getRoles().contains(UserRole.ADMIN.getRole()) ) {
                message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                return;
            }

            if(!assistant.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
                message.reply("Ошибка! Вы не авторизованы!").queue();
                return;
            }

            int assistantId = MTHD.getInstance().database.getUserId(assistant.getId());
            if(assistantId < 0) {
                message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                return;
            }

            if(contentRaw.equals("!ready")) {
                String errorMessage = MTHD.getInstance().database.setReady(assistantId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }
                message.reply("Вы успешно добавлены в таблицу доступных помощников!").queue();
                MTHD.getInstance().findGameChannel.updateAvailableAssistantsMessage();
                MTHD.getInstance().gameManager.tryStartGame();
            } else if(contentRaw.equals("!unready")) {
                String errorMessage = MTHD.getInstance().database.setUnready(assistantId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }
                message.reply("Вы успешно удалены из таблицы доступных помощников!").queue();
                MTHD.getInstance().findGameChannel.updateAvailableAssistantsMessage();
                MTHD.getInstance().gameManager.tryStartGame();
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }
}
