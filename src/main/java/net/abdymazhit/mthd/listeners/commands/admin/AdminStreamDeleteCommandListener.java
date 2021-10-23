package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.*;
import java.time.Instant;

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

        boolean isStreamDeleted = deleteStream(youtuberAccount.id, deleterId);
        if(!isStreamDeleted) {
            message.reply("Критическая ошибка при удалении трансляции! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Трансляция успешно удалена! Имя ютубера: %youtuber%"
                .replace("%youtuber%", youtuberName)).queue();
    }

    /**
     * Удаляет трансляцию
     * @param youtuberId Id ютубера
     * @param deleterId Id удаляющего
     * @return Значение, удалена ли трансляция
     */
    private boolean deleteStream(int youtuberId, int deleterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();

            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT link FROM streams WHERE youtuber_id = ?;");
            preparedStatement.setInt(1, youtuberId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                String link = resultSet.getString("link");

                MTHD.getInstance().liveStreamsManager.removeLiveStream(link);

                PreparedStatement deleteStatement = connection.prepareStatement(
                        "DELETE FROM streams WHERE youtuber_id = ?;");
                deleteStatement.setInt(1, youtuberId);
                deleteStatement.executeUpdate();

                PreparedStatement historyStatement = connection.prepareStatement(
                        "INSERT INTO streams_deletion_history (youtuber_id, link, deleter_id, deleted_at) VALUES (?, ?, ?, ?);");
                historyStatement.setInt(1, youtuberId);
                historyStatement.setString(2, link);
                historyStatement.setInt(3, deleterId);
                historyStatement.setTimestamp(4, Timestamp.from(Instant.now()));
                historyStatement.executeUpdate();
            }

            // Вернуть значение, что трансляция успешно удалена
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}