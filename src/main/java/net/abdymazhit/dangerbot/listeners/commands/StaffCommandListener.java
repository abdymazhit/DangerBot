package net.abdymazhit.dangerbot.listeners.commands;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

/**
 * Команда персонала
 *
 * @version   23.10.2021
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

        if(DangerBot.getInstance().staffChannel.channel.equals(messageChannel)) {
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

            int assistantId = DangerBot.getInstance().database.getUserId(assistant.getId());
            if(assistantId < 0) {
                message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                return;
            }

            if(contentRaw.equals("!ready")) {
                List<Integer> assistantsInLiveGames = DangerBot.getInstance().database.getAssistantsInLiveGames();
                if(assistantsInLiveGames.contains(assistantId)) {
                    message.reply("Ошибка! Вы уже проводите игру!").queue();
                    return;
                }

                String errorMessage = DangerBot.getInstance().database.setReady(assistantId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }
                message.reply("Вы успешно добавлены в таблицу доступных помощников!").queue();
                DangerBot.getInstance().teamFindGameChannel.updateAvailableAssistantsMessage();
                DangerBot.getInstance().singleFindGameChannel.updateAvailableAssistantsMessage();
                DangerBot.getInstance().gameManager.teamGameManager.tryStartGame();
                DangerBot.getInstance().gameManager.singleGameManager.tryStartGame();
            } else if(contentRaw.equals("!unready")) {
                String errorMessage = DangerBot.getInstance().database.setUnready(assistantId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }
                message.reply("Вы успешно удалены из таблицы доступных помощников!").queue();
                DangerBot.getInstance().teamFindGameChannel.updateAvailableAssistantsMessage();
                DangerBot.getInstance().singleFindGameChannel.updateAvailableAssistantsMessage();
                DangerBot.getInstance().gameManager.teamGameManager.tryStartGame();
                DangerBot.getInstance().gameManager.singleGameManager.tryStartGame();
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }
}
