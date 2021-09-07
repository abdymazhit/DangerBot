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
 * Администраторская команда добавления участника в команду
 *
 * @version   07.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamAddCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!contentRaw.startsWith("!adminteam add")) return;
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

        String adderName;
        if(member.getNickname() == null) {
            adderName = member.getEffectiveName();
        } else {
            adderName = member.getNickname();
        }

        int adderId = MTHD.getInstance().database.getUserId(adderName);
        if(adderId < 0) {
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

        boolean isUserTeamMember = MTHD.getInstance().database.isUserTeamMember(memberId);
        if(isUserTeamMember) {
            message.reply("Ошибка! Участник уже является участником другой команды!").queue();
            return;
        }

        boolean isUserTeamLeader = MTHD.getInstance().database.isUserTeamLeader(memberId);
        if(isUserTeamLeader) {
            message.reply("Ошибка! Участник уже является лидером другой команды!").queue();
            return;
        }

        int teamId = MTHD.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        boolean isMemberAdded = addTeamMember(teamId, memberId, adderId);
        if(!isMemberAdded) {
            message.reply("Ошибка! По неизвестной причине участник не добавился в команду! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Участник успешно добавлен! Название команды: " + teamName + ", название участника: " + memberName).queue();
    }

    /**
     * Добавляет участника в команду
     * @param teamId Id команды
     * @param memberId Id участника
     * @param adderId Id добавляющего
     * @return Значение, добавлен ли участник в команду
     */
    private boolean addTeamMember(int teamId, int memberId, int adderId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO teams_members (team_id, member_id) VALUES (?, ?) RETURNING id;");
            preparedStatement.setInt(1, teamId);
            preparedStatement.setInt(2, memberId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                PreparedStatement historyStatement = connection.prepareStatement(
                        "INSERT INTO teams_members_addition_history (team_id, member_id, adder_id, added_at) VALUES (?, ?, ?, ?);");
                historyStatement.setInt(1, teamId);
                historyStatement.setInt(2, memberId);
                historyStatement.setInt(3, adderId);
                historyStatement.setTimestamp(4, Timestamp.from(Instant.now()));
                historyStatement.executeUpdate();
                historyStatement.close();

                // Вернуть значение, что участник добавлен
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
