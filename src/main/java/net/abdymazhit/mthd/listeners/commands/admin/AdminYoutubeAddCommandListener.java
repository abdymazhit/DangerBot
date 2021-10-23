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
 * Администраторская команда добавления ютубера
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminYoutubeAddCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member adder = event.getMember();

        if(adder == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите имя ютубера!").queue();
            return;
        }

        if(command.length == 3) {
            message.reply("Ошибка! Укажите ссылку на канал ютубера!").queue();
            return;
        }

        if(command.length > 4) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!adder.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!adder.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int adderId = MTHD.getInstance().database.getUserId(adder.getId());
        if(adderId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String youtuberName = command[2];
        String channelLink = command[3];

        if(!channelLink.contains("https://www.youtube.com/channel/")) {
            message.reply("Ошибка! Такого канала не существует!").queue();
            return;
        }

        String channelId = channelLink.replace("https://www.youtube.com/channel/", "");

        UserAccount youtuberAccount = MTHD.getInstance().database.getUserAccount(youtuberName);
        if(youtuberAccount == null) {
            message.reply("Ошибка! Ютубер не зарегистрирован на сервере!").queue();
            return;
        }

        String errorMessage = addYoutuber(youtuberAccount.id, channelId, adderId);
        if(errorMessage != null) {
            message.reply(errorMessage).queue();
            return;
        }

        if(youtuberAccount.discordId != null) {
            MTHD.getInstance().guild.addRoleToMember(youtuberAccount.discordId, UserRole.YOUTUBE.getRole()).queue();
        }

        message.reply("Ютубер успешно добавлен! Имя ютубера: %youtuber%, id канала: %channel_id%"
                .replace("%youtuber%", youtuberName)
                .replace("%channel_id%", channelId)).queue();
    }

    /**
     * Добавляет ютубера
     * @param youtuberId Id ютубера
     * @param channelId Id канала
     * @param adderId Id добавляющего
     * @return Текст ошибки добавления ютубера
     */
    private String addYoutuber(int youtuberId, String channelId, int adderId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement addStatement = connection.prepareStatement("""
                INSERT INTO youtubers (youtuber_id, channel_id) SELECT ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM youtubers WHERE youtuber_id = ? AND is_deleted is null);""",
                    Statement.RETURN_GENERATED_KEYS);
            addStatement.setInt(1, youtuberId);
            addStatement.setString(2, channelId);
            addStatement.setInt(3, youtuberId);
            addStatement.executeUpdate();
            ResultSet createResultSet = addStatement.getGeneratedKeys();
            if(createResultSet.next()) {
                PreparedStatement historyStatement = connection.prepareStatement(
                        "INSERT INTO youtubers_addition_history (youtuber_id, adder_id, added_at) VALUES (?, ?, ?);");
                historyStatement.setInt(1, youtuberId);
                historyStatement.setInt(2, adderId);
                historyStatement.setTimestamp(3, Timestamp.from(Instant.now()));
                historyStatement.executeUpdate();

                // Вернуть значение, что ютубер успешно добавлен
                return null;
            } else {
                return "Ошибка! Ютубер уже добавлен!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении ютубера! Свяжитесь с разработчиком бота!";
        }
    }
}
