package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Администраторская команда блокировки игроков
 *
 * @version   22.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminBanCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();

        if(member == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 1) {
            message.reply("Ошибка! Укажите имя игрока!").queue();
            return;
        }

        if(command.length == 2) {
            message.reply("Ошибка! Укажите время бана!").queue();
            return;
        }

        if(command.length > 3) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int bannerId = MTHD.getInstance().database.getUserId(member.getId());
        if(bannerId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String playerName = command[1];
        String banTime = command[2];

        UserAccount userAccount = MTHD.getInstance().database.getUserAccount(playerName);
        if(userAccount == null) {
            message.reply("Ошибка! Игрок не зарегистрирован на сервере!").queue();
            return;
        }

        try {
            int timeInMinutes;

            char symbol = banTime.charAt(banTime.length() - 1);
            if(symbol == 'm') {
                banTime = banTime.replace("m", "");
                timeInMinutes = Integer.parseInt(banTime);
            } else if(symbol == 'h') {
                banTime = banTime.replace("h", "");
                timeInMinutes = Integer.parseInt(banTime) * 60;
            } else if(symbol == 'd') {
                banTime = banTime.replace("d", "");
                timeInMinutes = Integer.parseInt(banTime) * 1440;
            } else {
                message.reply("Ошибка! Вы неправильно ввели команду!").queue();
                return;
            }

            MTHD.getInstance().database.banPlayer(bannerId, userAccount.id, timeInMinutes);
            message.reply("Вы успешно заблокировали игрока! Имя игрока: %player%, discord id: %discord_id%, время в минутах: %minutes%"
                    .replace("%player%", playerName)
                    .replace("%discord_id%", userAccount.discordId)
                    .replace("%minutes%", String.valueOf(timeInMinutes))).queue();
        } catch (NumberFormatException exception) {
            exception.printStackTrace();
            message.reply("Ошибка! Неизвестная ошибка!").queue();
        }
    }
}