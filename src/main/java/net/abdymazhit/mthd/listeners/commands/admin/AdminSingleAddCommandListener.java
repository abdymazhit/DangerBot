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
 * Администраторская команда добавления игрока в Single Rating
 *
 * @version   05.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminSingleAddCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member adder = event.getMember();

        if(adder == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите имя игрока!").queue();
            return;
        }

        if(command.length > 3) {
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

        String playerName = command[2];

        UserAccount playerAccount = MTHD.getInstance().database.getUserIdAndDiscordId(playerName);
        if(playerAccount == null) {
            message.reply("Ошибка! Игрок не зарегистрирован на сервере!").queue();
            return;
        }

        String errorMessage = addPlayer(playerAccount.id, adderId);
        if(errorMessage != null) {
            message.reply(errorMessage).queue();
            return;
        }

        if(playerAccount.discordId != null) {
            MTHD.getInstance().guild.addRoleToMember(playerAccount.discordId, UserRole.SINGLE_RATING.getRole()).queue();
        }

        message.reply("Игрок успешно добавлен в Single Rating! Имя игрока: " + playerName).queue();
        MTHD.getInstance().playersChannel.updateTopMessage();
    }

    /**
     * Добавляет игрока в Single Rating
     * @param playerId Id игрока
     * @param adderId Id добавляющего
     * @return Текст ошибки добавления игрока в Single Rating
     */
    private String addPlayer(int playerId, int adderId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement createStatement = connection.prepareStatement("""
                INSERT INTO players (player_id) SELECT ? WHERE NOT EXISTS
                (SELECT player_id FROM players WHERE player_id = ? AND is_deleted is null);""", Statement.RETURN_GENERATED_KEYS);
            createStatement.setInt(1, playerId);
            createStatement.setInt(2, playerId);
            createStatement.executeUpdate();
            ResultSet createResultSet = createStatement.getGeneratedKeys();
            if(createResultSet.next()) {
                PreparedStatement historyStatement = connection.prepareStatement(
                        "INSERT INTO players_addition_history (player_id, adder_id, added_at) VALUES (?, ?, ?);");
                historyStatement.setInt(1, playerId);
                historyStatement.setInt(2, adderId);
                historyStatement.setTimestamp(3, Timestamp.from(Instant.now()));
                historyStatement.executeUpdate();

                // Вернуть значение, что игрок успешно добавлен в Single Rating
                return null;
            } else {
                return "Ошибка! Игрок уже владеет статусом Single Rating!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении игрока в Single Rating! Свяжитесь с разработчиком бота!";
        }
    }
}
