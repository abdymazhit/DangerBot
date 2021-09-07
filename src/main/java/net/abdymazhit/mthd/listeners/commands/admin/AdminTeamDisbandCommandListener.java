package net.abdymazhit.mthd.listeners.commands.admin;

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
 * Администраторская команда удаления команды
 *
 * @version   07.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamDisbandCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!contentRaw.startsWith("!adminteam disband")) return;
        if(!messageChannel.equals(MTHD.getInstance().adminChannel.channel)) return;
        if(member == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите название команды!").queue();
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

        int teamId = MTHD.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        boolean isDisbanded = disbandTeam(teamId, deleterId);
        if(!isDisbanded) {
            message.reply("Ошибка! По неизвестной причине команда не удалилась! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Команда успешно удалена! Название команды: " + teamName).queue();
    }

    /**
     * Удаляет команду
     * @param teamId Id команды
     * @param deleterId Id удаляющего
     * @return Значение, удалена ли команда
     */
    private boolean disbandTeam(int teamId, int deleterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement updateStatement = connection.prepareStatement(
                    "UPDATE teams SET is_deleted = true WHERE id = ?;");
            updateStatement.setInt(1, teamId);
            updateStatement.executeUpdate();
            updateStatement.close();

            PreparedStatement membersStatement = connection.prepareStatement(
                    "DELETE FROM teams_members WHERE team_id = ?;");
            membersStatement.setInt(1, teamId);
            membersStatement.executeUpdate();
            membersStatement.close();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO teams_deletion_history (team_id, deleter_id, deleted_at) VALUES (?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, deleterId);
            historyStatement.setTimestamp(3, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();
            historyStatement.close();

            // Вернуть значение, что команда удалена
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
