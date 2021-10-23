package net.abdymazhit.dangerbot.listeners.commands.admin;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Администраторская команда удаления ютубера
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminYoutubeDeleteCommandListener {

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

        int deleterId = DangerBot.getInstance().database.getUserId(deleter.getId());
        if(deleterId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String youtuberName = command[2];

        UserAccount youtuberAccount = DangerBot.getInstance().database.getUserAccount(youtuberName);
        if(youtuberAccount == null) {
            message.reply("Ошибка! Ютубер не зарегистрирован на сервере!").queue();
            return;
        }

        boolean isYoutuberDeleted = deleteYoutuber(youtuberAccount.id, deleterId);
        if(!isYoutuberDeleted) {
            message.reply("Критическая ошибка при удалении ютубера! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        if(youtuberAccount.discordId != null) {
            DangerBot.getInstance().guild.removeRoleFromMember(youtuberAccount.discordId, UserRole.YOUTUBE.getRole()).queue();
        }

        message.reply("Ютубер успешно удален! Имя ютубера: %youtuber%"
                .replace("%youtuber%", youtuberName)).queue();
    }

    /**
     * Удаляет ютубера
     * @param youtuberId Id ютубера
     * @param deleterId Id удаляющего
     * @return Значение, удален ли ютубер
     */
    private boolean deleteYoutuber(int youtuberId, int deleterId) {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement deleteStatement = connection.prepareStatement(
                    "UPDATE youtubers SET is_deleted = true WHERE youtuber_id = ? AND is_deleted is null;");
            deleteStatement.setInt(1, youtuberId);
            deleteStatement.executeUpdate();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO youtubers_deletion_history (youtuber_id, deleter_id, deleted_at) VALUES (?, ?, ?);");
            historyStatement.setInt(1, youtuberId);
            historyStatement.setInt(2, deleterId);
            historyStatement.setTimestamp(3, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();

            // Вернуть значение, что ютубер успешно удален
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}