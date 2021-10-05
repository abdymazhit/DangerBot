package net.abdymazhit.mthd.listeners.commands.team;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Team;
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
 * Команда передать права лидера
 *
 * @version   05.10.2021
 * @author    Islam Abdymazhit
 */
public class TeamTransferCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member changer = event.getMember();

        if(changer == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите ник нового лидера команды!").queue();
            return;
        }

        if(command.length > 3) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!changer.getRoles().contains(UserRole.LEADER.getRole())) {
            message.reply("Ошибка! Вы не являетесь лидером команды!").queue();
            return;
        }

        if(!changer.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int changerId = MTHD.getInstance().database.getUserId(changer.getId());
        if(changerId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String newLeaderName = command[2];

        Team team = MTHD.getInstance().database.getLeaderTeam(changerId);
        if(team == null) {
            message.reply("Ошибка! Вы не являетесь лидером какой-либо команды!").queue();
            return;
        }

        UserAccount newLeaderAccount = MTHD.getInstance().database.getUserIdAndDiscordId(newLeaderName);
        if(newLeaderAccount == null) {
            message.reply("Ошибка! Новый лидер команды не зарегистрирован на сервере!").queue();
            return;
        }

        boolean isUserTeamMember = MTHD.getInstance().database.isUserTeamMember(newLeaderAccount.id, team.id);
        if(!isUserTeamMember) {
            message.reply("Ошибка! Новый лидер команды в настоящий момент не является участником этой команды!").queue();
            return;
        }

        boolean isTransferred = transferLeader(team.id, changerId, newLeaderAccount.id, changerId);
        if(!isTransferred) {
            message.reply("Критическая ошибка при передачи прав лидера! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        MTHD.getInstance().guild.removeRoleFromMember(changer.getId(), UserRole.LEADER.getRole()).queue();
        MTHD.getInstance().guild.addRoleToMember(changer.getId(), UserRole.MEMBER.getRole()).queue();

        if(newLeaderAccount.discordId != null) {
            MTHD.getInstance().guild.removeRoleFromMember(newLeaderAccount.discordId, UserRole.MEMBER.getRole()).queue();
            MTHD.getInstance().guild.addRoleToMember(newLeaderAccount.discordId, UserRole.LEADER.getRole()).queue();
        }

        message.reply("Вы успешно передали права лидера! Новый лидер команды: " + newLeaderName).queue();
    }

    /**
     * Передает права лидера команды
     * @param teamId Id команды
     * @param fromId Id текущего лидера команды
     * @param toId Id нового лидера команды
     * @param changerId Id изменяющего
     * @return Значение, переданы ли права лидера команды
     */
    public boolean transferLeader(int teamId, int fromId, int toId, int changerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement updateStatement = connection.prepareStatement(
                "UPDATE teams SET leader_id = ? WHERE id = ? AND is_deleted is null;");
            updateStatement.setInt(1, toId);
            updateStatement.setInt(2, teamId);
            updateStatement.executeUpdate();

            PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM teams_members WHERE team_id = ? AND member_id = ?;");
            deleteStatement.setInt(1, teamId);
            deleteStatement.setInt(2, toId);
            deleteStatement.executeUpdate();

            PreparedStatement createStatement = connection.prepareStatement(
                "INSERT INTO teams_members (team_id, member_id) VALUES (?, ?);");
            createStatement.setInt(1, teamId);
            createStatement.setInt(2, fromId);
            createStatement.executeUpdate();

            PreparedStatement historyStatement = connection.prepareStatement(
                "INSERT INTO teams_leaders_transfer_history (team_id, from_id, to_id, changer_id, changed_at) VALUES (?, ?, ?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, fromId);
            historyStatement.setInt(3, toId);
            historyStatement.setInt(4, changerId);
            historyStatement.setTimestamp(5, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();

            // Вернуть значение, что права лидера успешно переданы
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}