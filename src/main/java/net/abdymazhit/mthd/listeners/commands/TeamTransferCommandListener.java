package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Команда передать права лидера
 *
 * @version   07.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamTransferCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!contentRaw.startsWith("!team transfer")) return;
        if(!messageChannel.equals(MTHD.getInstance().myTeamChannel.channel)) return;
        if(member == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите ник нового лидера команды!").queue();
            return;
        }

        if(command.length > 3) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.LEADER.getRole())) {
            message.reply("Ошибка! Вы не являетесь лидером команды!").queue();
            return;
        }

        String changerName;
        if(member.getNickname() == null) {
            changerName = member.getEffectiveName();
        } else {
            changerName = member.getNickname();
        }

        int changerId = MTHD.getInstance().database.getUserId(changerName);
        if(changerId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String newLeaderName = command[2];

        int teamId = MTHD.getInstance().database.getLeaderTeamId(changerId);
        if(teamId < 0) {
            message.reply("Ошибка! Вы не являетесь лидером какой-либо команды!").queue();
            return;
        }

        int newLeaderId = MTHD.getInstance().database.getUserId(newLeaderName);
        if(newLeaderId < 0) {
            message.reply("Ошибка! Новый лидер команды не зарегистрирован на сервере!").queue();
            return;
        }

        boolean isUserTeamMember = MTHD.getInstance().database.isUserTeamMember(newLeaderId, teamId);
        if(!isUserTeamMember) {
            message.reply("Ошибка! Новый лидер команды в настоящий момент не является участником этой команды!").queue();
            return;
        }

        boolean isTransferred = transferLeader(teamId, changerId, newLeaderId, changerId);
        if(!isTransferred) {
            message.reply("Ошибка! По неизвестной причине Вы не смогли передать права лидера! Свяжитесь с разработчиком бота!").queue();
            return;
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
    private boolean transferLeader(int teamId, int fromId, int toId, int changerId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement updateStatement = connection.prepareStatement(
                    "UPDATE teams SET leader_id = ? WHERE id = ?;");
            updateStatement.setInt(1, toId);
            updateStatement.setInt(2, teamId);
            updateStatement.executeUpdate();
            updateStatement.close();

            PreparedStatement deleteStatement = connection.prepareStatement(
                    "DELETE FROM teams_members WHERE team_id = ? AND member_id = ?;");
            deleteStatement.setInt(1, teamId);
            deleteStatement.setInt(2, toId);
            deleteStatement.executeUpdate();
            deleteStatement.close();

            PreparedStatement createStatement = connection.prepareStatement(
                    "INSERT INTO teams_members (team_id, member_id) VALUES (?, ?);");
            createStatement.setInt(1, teamId);
            createStatement.setInt(2, fromId);
            createStatement.executeUpdate();
            createStatement.close();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO teams_leaders_transfer_history (team_id, from_id, to_id, changer_id, changed_at) VALUES (?, ?, ?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, fromId);
            historyStatement.setInt(3, toId);
            historyStatement.setInt(4, changerId);
            historyStatement.setTimestamp(5, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();
            historyStatement.close();

            // Вернуть значение, что права лидера переданы
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}