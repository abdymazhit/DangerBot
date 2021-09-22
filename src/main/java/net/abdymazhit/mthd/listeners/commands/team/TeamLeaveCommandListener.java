package net.abdymazhit.mthd.listeners.commands.team;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Команда покинуть команду
 *
 * @version   22.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamLeaveCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member deleter = event.getMember();

        if(deleter == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length > 2) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(deleter.getRoles().contains(UserRole.LEADER.getRole())) {
            message.reply("Ошибка! Вы не можете покинуть команду, так как Вы являетесь лидером команды!").queue();
            return;
        }

        if(!deleter.getRoles().contains(UserRole.MEMBER.getRole())) {
            message.reply("Ошибка! Команда доступна только для участников команды!").queue();
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

        Team team = MTHD.getInstance().database.getMemberTeam(deleterId);
        if(team == null) {
            message.reply("Ошибка! Вы не являетесь участником какой-либо команды!").queue();
            return;
        }

        boolean isMemberDeleted = deleteTeamMember(team.id, deleterId, deleterId);
        if(!isMemberDeleted) {
            message.reply("Критическая ошибка при попытке покинуть команду! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(team.name, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        MTHD.getInstance().guild.removeRoleFromMember(deleter.getId(), teamRoles.get(0)).queue();
        MTHD.getInstance().guild.removeRoleFromMember(deleter.getId(), UserRole.MEMBER.getRole()).queue();

        message.reply("Вы успешно покинули команду!").queue();
    }

    /**
     * Удаляет участника из команды
     * @param teamId Id команды
     * @param memberId Id участника
     * @param deleterId Id удаляющего
     * @return Значение, удален ли участник из команды
     */
    public boolean deleteTeamMember(int teamId, int memberId, int deleterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM teams_members WHERE team_id = ? AND member_id = ?;");
            deleteStatement.setInt(1, teamId);
            deleteStatement.setInt(2, memberId);
            deleteStatement.executeUpdate();

            PreparedStatement historyStatement = connection.prepareStatement(
                "INSERT INTO teams_members_deletion_history (team_id, member_id, deleter_id, deleted_at) VALUES (?, ?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, memberId);
            historyStatement.setInt(3, deleterId);
            historyStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();

            // Вернуть значение, что участник успешно удален
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
