package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Администраторская команда удаления игрока из Single Rating
 *
 * @version   21.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminSingleDeleteCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member deleter = event.getMember();

        if(deleter == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите имя игрока!").queue();
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

        String playerName = command[2];

        UserAccount playerAccount = MTHD.getInstance().database.getUserIdAndDiscordId(playerName);
        if(playerAccount == null) {
            message.reply("Ошибка! Игрок не зарегистрирован на сервере!").queue();
            return;
        }

        String errorMessage = deletePlayer(playerAccount.id, deleterId);
        if(errorMessage != null) {
            message.reply(errorMessage).queue();
            return;
        }

        MTHD.getInstance().guild.removeRoleFromMember(playerAccount.discordId, UserRole.SINGLE_RATING.getRole()).queue();

        message.reply("Игрок успешно удален из Single Rating! Имя игрока: %player%"
                .replace("%player%", playerName)).queue();
        MTHD.getInstance().playersChannel.updateTopMessage();
    }

    /**
     * Удаляет игрока из Single Rating
     * @param playerId Id игрока
     * @param deleterId Id удаляющего
     * @return Текст ошибки удаления игрока из Single Rating
     */
    public String deletePlayer(int playerId, int deleterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement deleteStatement = connection.prepareStatement(
                    "UPDATE players SET is_deleted = true WHERE player_id = ? AND is_deleted is null;");
            deleteStatement.setInt(1, playerId);
            deleteStatement.executeUpdate();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO players_deletion_history (player_id, deleter_id, deleted_at) VALUES (?, ?, ?);");
            historyStatement.setInt(1, playerId);
            historyStatement.setInt(2, deleterId);
            historyStatement.setTimestamp(3, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();

            // Вернуть значение, что игрок удален из Single Rating
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении игрока из Single Rating! Свяжитесь с разработчиком бота!";
        }
    }
}