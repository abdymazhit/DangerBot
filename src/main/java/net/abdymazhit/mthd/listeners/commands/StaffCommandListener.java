package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

/**
 * Команда персонала
 *
 * @version   21.10.2021
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

        if(MTHD.getInstance().staffChannel.channel.equals(messageChannel)) {
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
                List<Integer> assistantsInLiveGames = MTHD.getInstance().database.getAssistantsInLiveGames();
                if(assistantsInLiveGames.contains(assistantId)) {
                    message.reply("Ошибка! Вы уже проводите игру!").queue();
                    return;
                }

                String errorMessage = MTHD.getInstance().database.setReady(assistantId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }
                message.reply("Вы успешно добавлены в таблицу доступных помощников!").queue();
                MTHD.getInstance().teamFindGameChannel.updateAvailableAssistantsMessage();
                MTHD.getInstance().singleFindGameChannel.updateAvailableAssistantsMessage();
                MTHD.getInstance().gameManager.teamGameManager.tryStartGame();
                MTHD.getInstance().gameManager.singleGameManager.tryStartGame();
            } else if(contentRaw.equals("!unready")) {
                String errorMessage = MTHD.getInstance().database.setUnready(assistantId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }
                message.reply("Вы успешно удалены из таблицы доступных помощников!").queue();
                MTHD.getInstance().teamFindGameChannel.updateAvailableAssistantsMessage();
                MTHD.getInstance().singleFindGameChannel.updateAvailableAssistantsMessage();
                MTHD.getInstance().gameManager.teamGameManager.tryStartGame();
                MTHD.getInstance().gameManager.singleGameManager.tryStartGame();
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }
}
