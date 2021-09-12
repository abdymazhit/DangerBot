package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.*;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

/**
 * Администраторская команда создания команды
 *
 * @version   12.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamCreateCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member creator = event.getMember();

        if(creator == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите название команды!").queue();
            return;
        }

        if(command.length == 3) {
            message.reply("Ошибка! Укажите лидера команды!").queue();
            return;
        }

        if(command.length > 4) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!creator.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!creator.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int creatorId = MTHD.getInstance().database.getUserId(creator.getId());
        if(creatorId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String teamName = command[2];
        String leaderName = command[3];

        UserAccount leaderAccount = MTHD.getInstance().database.getUserIdAndDiscordId(leaderName);
        if(leaderAccount == null) {
            message.reply("Ошибка! Лидер не зарегистрирован на сервере!").queue();
            return;
        }

        int leaderTeamId = MTHD.getInstance().database.getUserTeamId(leaderAccount.getId());
        if(leaderTeamId > 0) {
            message.reply("Ошибка! Лидер уже состоит в команде!").queue();
            return;
        }

        if(!MTHD.getInstance().guild.getRolesByName(teamName, true).isEmpty()) {
            message.reply("Ошибка! Вы пытаетесь занять роль команды, которая уже существует! " +
                    "Возможно вы пытаетесь занять роли сервера: Admin, Assistant... Если Вы уверены, " +
                    "что не занимаете роль сервера свяжитесь с разработчиком бота!").queue();
            return;
        }

        String errorMessage = createTeam(teamName, leaderAccount.getId(), creatorId);
        if(errorMessage != null) {
            message.reply(errorMessage).queue();
            return;
        }

        try {
            Role teamRole = MTHD.getInstance().guild.createCopyOfRole(UserRole.TEST.getRole()).setName(teamName)
                    .setColor(10070709).submit().get();
            MTHD.getInstance().guild.addRoleToMember(leaderAccount.getDiscordId(), teamRole).queue();
            MTHD.getInstance().guild.addRoleToMember(leaderAccount.getDiscordId(), UserRole.LEADER.getRole()).queue();

            message.reply("Команда успешно создана! Название команды: " + teamName + ", лидер команды: "
                    + leaderName + ", роль команды: " + teamRole.getAsMention()).queue();
            MTHD.getInstance().teamsChannel.updateTop();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            message.reply("Критическая ошибка при создании роли команды и выдачи лидеру роли лидера! Свяжитесь с разработчиком бота!").queue();
        }
    }

    /**
     * Создает команду
     * @param teamName Название команды
     * @param leaderId Id лидера команды
     * @param creatorId Id создателя команды
     * @return Текст ошибки создания команды
     */
    private String createTeam(String teamName, int leaderId, int creatorId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement createStatement = connection.prepareStatement(
                            "INSERT INTO teams (name, leader_id) SELECT ?, ? " +
                            "WHERE NOT EXISTS (SELECT leader_id FROM teams WHERE name ILIKE ? AND is_deleted is null)" +
                            "RETURNING id;");
            createStatement.setString(1, teamName);
            createStatement.setInt(2, leaderId);
            createStatement.setString(3, teamName);
            ResultSet createResultSet = createStatement.executeQuery();
            createStatement.close();

            if(createResultSet.next()) {
                int teamId = createResultSet.getInt("id");

                PreparedStatement historyStatement = connection.prepareStatement(
                        "INSERT INTO teams_creation_history (team_id, name, leader_id, creator_id, created_at) VALUES (?, ?, ?, ?, ?);");
                historyStatement.setInt(1, teamId);
                historyStatement.setString(2, teamName);
                historyStatement.setInt(3, leaderId);
                historyStatement.setInt(4, creatorId);
                historyStatement.setTimestamp(5, Timestamp.from(Instant.now()));
                historyStatement.executeUpdate();
                historyStatement.close();

                // Вернуть значение, что команда успешно создана
                return null;
            } else {
                return "Ошибка! Команда с таким именем уже существует!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при создании команды! Свяжитесь с разработчиком бота!";
        }
    }
}
