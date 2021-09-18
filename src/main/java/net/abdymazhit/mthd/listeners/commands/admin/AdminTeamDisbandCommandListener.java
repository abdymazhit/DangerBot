package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.*;
import java.time.Instant;
import java.util.List;

/**
 * Администраторская команда удаления команды
 *
 * @version   18.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamDisbandCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member deleter = event.getMember();

        if(deleter == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите название команды!").queue();
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

        String teamName = command[2];

        int teamId = MTHD.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        String errorMessage = disbandTeam(teamId, deleterId);
        if(errorMessage != null) {
            message.reply(errorMessage).queue();
            return;
        }

        List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(teamName, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        teamRoles.get(0).delete().queue();

        message.reply("Команда успешно удалена! Название команды: " + teamName).queue();
        MTHD.getInstance().teamsChannel.updateTopMessage();
    }

    /**
     * Удаляет команду
     * @param teamId Id команды
     * @param deleterId Id удаляющего
     * @return Текст ошибки удаления команды
     */
    private String disbandTeam(int teamId, int deleterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement deleteStatement = connection.prepareStatement(
                    "UPDATE teams SET is_deleted = true WHERE id = ?;");
            deleteStatement.setInt(1, teamId);
            deleteStatement.executeUpdate();

            PreparedStatement selectLeaderStatement = connection.prepareStatement("SELECT leader_id FROM teams WHERE id = ?;");
            selectLeaderStatement.setInt(1, teamId);
            ResultSet selectLeaderResultSet = selectLeaderStatement.executeQuery();
            if(selectLeaderResultSet.next()) {
                int leaderId = selectLeaderResultSet.getInt("leader_id");

                PreparedStatement leaderStatement = connection.prepareStatement("SELECT discord_id FROM users WHERE id = ?;");
                leaderStatement.setInt(1, leaderId);
                ResultSet leaderResultSet = leaderStatement.executeQuery();
                if(leaderResultSet.next()) {
                    MTHD.getInstance().guild.removeRoleFromMember(leaderResultSet.getString("discord_id"), UserRole.LEADER.getRole()).queue();
                }
            }

            PreparedStatement selectMemberStatement = connection.prepareStatement("SELECT member_id FROM teams_members WHERE team_id = ?;");
            selectMemberStatement.setInt(1, teamId);
            ResultSet selectMemberResultSet = selectMemberStatement.executeQuery();
            if(selectMemberResultSet.next()) {
                int memberId = selectMemberResultSet.getInt("member_id");

                PreparedStatement memberStatement = connection.prepareStatement("SELECT discord_id FROM users WHERE id = ?;");
                memberStatement.setInt(1, memberId);
                ResultSet memberResultSet = memberStatement.executeQuery();
                if(memberResultSet.next()) {
                    MTHD.getInstance().guild.removeRoleFromMember(memberResultSet.getString("discord_id"), UserRole.MEMBER.getRole()).queue();
                }
            }

            PreparedStatement membersStatement = connection.prepareStatement(
                    "DELETE FROM teams_members WHERE team_id = ?;");
            membersStatement.setInt(1, teamId);
            membersStatement.executeUpdate();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO teams_deletion_history (team_id, deleter_id, deleted_at) VALUES (?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, deleterId);
            historyStatement.setTimestamp(3, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();

            // Вернуть значение, что команда успешно удалена
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении команды! Свяжитесь с разработчиком бота!";
        }
    }
}
