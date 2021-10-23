package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
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
 * Администраторская команда добавления участника в команду
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamAddCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member adder = event.getMember();

        if(adder == null) return;

        String[] command = message.getContentRaw().split(" ");

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

        String teamName = command[2];
        String memberName = command[3];

        UserAccount memberAccount = MTHD.getInstance().database.getUserAccount(memberName);
        if(memberAccount == null) {
            message.reply("Ошибка! Участник не зарегистрирован на сервере!").queue();
            return;
        }

        int memberTeamId = MTHD.getInstance().database.getUserTeamId(memberAccount.id);
        if(memberTeamId > 0) {
            message.reply("Ошибка! Участник уже состоит в команде!").queue();
            return;
        }

        int teamId = MTHD.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        boolean isMemberAdded = addTeamMember(teamId, memberAccount.id, adderId);
        if(!isMemberAdded) {
            message.reply("Критическая ошибка при добавлении участника в команду! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(teamName, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        if(memberAccount.discordId != null) {
            MTHD.getInstance().guild.addRoleToMember(memberAccount.discordId, teamRoles.get(0)).queue();
            MTHD.getInstance().guild.addRoleToMember(memberAccount.discordId, UserRole.MEMBER.getRole()).queue();
        }

        message.reply("Участник успешно добавлен в команду! Название команды: %team%, ник участника: %member%"
                .replace("%team%", teamName)
                .replace("%member%", memberName)).queue();
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
            PreparedStatement addStatement = connection.prepareStatement("""
                INSERT INTO teams_members (team_id, member_id) SELECT ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM teams_members WHERE member_id = ?);""");
            addStatement.setInt(1, teamId);
            addStatement.setInt(2, memberId);
            addStatement.setInt(3, memberId);
            addStatement.executeUpdate();

            PreparedStatement historyStatement = connection.prepareStatement(
                    "INSERT INTO teams_members_addition_history (team_id, member_id, adder_id, added_at) VALUES (?, ?, ?, ?);");
            historyStatement.setInt(1, teamId);
            historyStatement.setInt(2, memberId);
            historyStatement.setInt(3, adderId);
            historyStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            historyStatement.executeUpdate();

            // Вернуть значение, что участник успешно добавлен
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
