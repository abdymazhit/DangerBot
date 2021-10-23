package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Администраторская команда удаления ютубера
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminStreamDeleteCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member deleter = event.getMember();

        if(deleter == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите имя ютубера!").queue();
            return;
        }

        if(command.length > 3) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!deleter.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!deleter.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int deleterId = MTHD.getInstance().database.getUserId(deleter.getId());
        if(deleterId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String youtuberName = command[2];

        UserAccount youtuberAccount = MTHD.getInstance().database.getUserAccount(youtuberName);
        if(youtuberAccount == null) {
            message.reply("Ошибка! Ютубер не зарегистрирован на сервере!").queue();
            return;
        }

        boolean isStreamDeleted = MTHD.getInstance().database.deleteStream(youtuberAccount.id, deleterId);
        if(!isStreamDeleted) {
            message.reply("Критическая ошибка при удалении трансляции! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Трансляция успешно удалена! Имя ютубера: %youtuber%"
                .replace("%youtuber%", youtuberName)).queue();
    }
}