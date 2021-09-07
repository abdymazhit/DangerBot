package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.*;
import java.time.Instant;

/**
 * Администраторская команда удаления участника из команды
 *
 * @version   07.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamDeleteCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!contentRaw.startsWith("!adminteam delete")) return;
        if(!messageChannel.equals(MTHD.getInstance().adminChannel.channel)) return;
        if(member == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите название команды!").queue();
            return;
        }

        if(command.length == 3) {
            message.reply("Ошибка! Укажите участника команды!").queue();
            return;
        }

        if(command.length > 4) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        String deleterName;
        if(member.getNickname() == null) {
            deleterName = member.getEffectiveName();
        } else {
            deleterName = member.getNickname();
        }

        int deleterId = MTHD.getInstance().database.getUserId(deleterName);
        if(deleterId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String teamName = command[2];
        String memberName = command[3];

        int memberId = MTHD.getInstance().database.getUserId(memberName);
        if(memberId < 0) {
            message.reply("Ошибка! Участник не зарегистрирован на сервере!").queue();
            return;
        }

        int teamId = MTHD.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        boolean isUserTeamLeader = MTHD.getInstance().database.isUserTeamLeader(memberId);
        if(isUserTeamLeader) {
            message.reply("Ошибка! Участник является лидером команды! " +
                    "Для удаления участника из команды Вы сперва должны передавать права лидера другому игроку!").queue();
            return;
        }

        boolean isUserTeamMember = MTHD.getInstance().database.isUserTeamMember(memberId, teamId);
        if(!isUserTeamMember) {
            message.reply("Ошибка! Участник не является участником этой команды!").queue();
            return;
        }

        boolean isMemberDeleted = deleteTeamMember(teamId, memberId, deleterId);
        if(!isMemberDeleted) {
            message.reply("Ошибка! По неизвестной причине участник не удалился из команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Участник успешно удален! Название команды: " + teamName + ", название участника: " + memberName).queue();
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
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "DELETE FROM teams_members WHERE team_id = ? AND member_id = ? RETURNING id;");
            preparedStatement.setInt(1, teamId);
            preparedStatement.setInt(2, memberId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                PreparedStatement historyStatement = connection.prepareStatement(
                        "INSERT INTO teams_members_deletion_history (team_id, member_id, deleter_id, deleted_at) VALUES (?, ?, ?, ?);");
                historyStatement.setInt(1, teamId);
                historyStatement.setInt(2, memberId);
                historyStatement.setInt(3, deleterId);
                historyStatement.setTimestamp(4, Timestamp.from(Instant.now()));
                historyStatement.executeUpdate();
                historyStatement.close();

                // Вернуть значение, что участник удален
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
