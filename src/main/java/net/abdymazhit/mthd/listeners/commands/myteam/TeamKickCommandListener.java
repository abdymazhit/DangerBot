package net.abdymazhit.mthd.listeners.commands.myteam;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Команда исключить участника из команды
 *
 * @version   09.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamKickCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member deleter = event.getMember();

        if(!contentRaw.startsWith("!team kick")) return;
        if(!messageChannel.equals(MTHD.getInstance().myTeamChannel.channel)) return;
        if(deleter == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите участника команды!").queue();
            return;
        }

        if(command.length > 3) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!deleter.getRoles().contains(UserRole.LEADER.getRole())) {
            message.reply("Ошибка! Команда доступна только для лидеров команд!").queue();
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

        String memberName = command[2];

        UserAccount memberAccount = MTHD.getInstance().database.getUserIdAndDiscordId(memberName);
        if(memberAccount == null) {
            message.reply("Ошибка! Участник не зарегистрирован на сервере!").queue();
            return;
        }

        Team team = MTHD.getInstance().database.getLeaderTeam(deleterId);
        if(team == null) {
            message.reply("Ошибка! Вы не являетесь лидером какой-либо команды!").queue();
            return;
        }

        boolean isUserTeamMember = MTHD.getInstance().database.isUserTeamMember(memberAccount.getId(), team.id);
        if(!isUserTeamMember) {
            message.reply("Ошибка! Участник не является участником этой команды!").queue();
            return;
        }

        boolean isMemberDeleted = deleteTeamMember(team.id, memberAccount.getId(), deleterId);
        if(!isMemberDeleted) {
            message.reply("Критическая ошибка при удалении участника из команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(team.name, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        try {
            MTHD.getInstance().guild.removeRoleFromMember(memberAccount.getDiscordId(), teamRoles.get(0)).submit().get();
            MTHD.getInstance().guild.removeRoleFromMember(memberAccount.getDiscordId(), UserRole.MEMBER.getRole()).submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            message.reply("Критическая ошибка при удалении у участника роли команды и роли участника! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Вы успешно выгнали участника из команды!").queue();
    }

    /**
     * Удаляет участника из команды
     * @param teamId Id команды
     * @param memberId Id участника
     * @param deleterId Id удаляющего
     * @return Значение, удален ли участник из команды
     */
    private boolean deleteTeamMember(int teamId, int memberId, int deleterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement deleteStatement = connection.prepareStatement(
                    "DELETE FROM teams_members WHERE team_id = ? AND member_id = ?;");
            deleteStatement.setInt(1, teamId);
            deleteStatement.setInt(2, memberId);
            deleteStatement.executeUpdate();
            deleteStatement.close();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO teams_members_deletion_history (team_id, member_id, deleter_id, deleted_at) VALUES (?, ?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, memberId);
            historyStatement.setInt(3, deleterId);
            historyStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();
            historyStatement.close();

            // Вернуть значение, что участник успешно удален
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
