package net.abdymazhit.dangerbot.listeners.commands.admin;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Администраторская команда добавления участника в команду
 *
 * @version   28.10.2021
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

        if(!adder.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!adder.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int adderId = DangerBot.getInstance().database.getUserId(adder.getId());
        if(adderId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String teamName = command[2];

        List<String> membersGoodResults = new ArrayList<>();
        Map<String, String> membersBadResults = new HashMap<>();

        for(int i = 3; i < command.length; i++) {
            String memberName = command[i];
            String result = addMember(memberName, teamName, adderId);

            if(result.equals("Успешное добавление")) {
                membersGoodResults.add(memberName);
            } else {
                membersBadResults.put(memberName, result);
            }
        }

        StringBuilder resultsOfGoodAdding = new StringBuilder();
        for(String memberName : membersGoodResults) {
            resultsOfGoodAdding.append(memberName).append("\n");
        }
        if(resultsOfGoodAdding.isEmpty()) {
            resultsOfGoodAdding.append("-");
        }

        StringBuilder resultsOfBadAdding = new StringBuilder();
        for(String memberName : membersBadResults.keySet()) {
            resultsOfBadAdding.append(memberName).append(": ").append(membersBadResults.get(memberName)).append("\n");
        }
        if(resultsOfBadAdding.isEmpty()) {
            resultsOfBadAdding.append("-");
        }

        message.reply("""
                **Результаты добавления:**
                Название команды: %team%
                
                Успешно добавлены:
                %resultsOfGoodAdding%
                Не были добавлены:
                %resultsOfBadAdding%
                """
                .replace("%team%", teamName)
                .replace("%resultsOfGoodAdding%", resultsOfGoodAdding)
                .replace("%resultsOfBadAdding%", resultsOfBadAdding)).queue();
    }

    /**
     * Добавляет участника в команду
     * @param memberName Имя участника
     * @param teamName Имя команды участника
     * @param adderId Id добавлящего
     * @return Результат
     */
    private String addMember(String memberName, String teamName, int adderId) {
        UserAccount memberAccount = DangerBot.getInstance().database.getUserAccount(memberName);
        if(memberAccount == null) {
            return "Ошибка! Участник не зарегистрирован на сервере!";
        }

        int memberTeamId = DangerBot.getInstance().database.getUserTeamId(memberAccount.id);
        if(memberTeamId > 0) {
            return "Ошибка! Участник уже состоит в команде!";
        }

        int teamId = DangerBot.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            return "Ошибка! Команда с таким именем не существует!";
        }

        boolean isMemberAdded = addTeamMember(teamId, memberAccount.id, adderId);
        if(!isMemberAdded) {
            return "Критическая ошибка при добавлении участника в команду! Свяжитесь с разработчиком бота!";
        }

        List<Role> teamRoles = DangerBot.getInstance().guild.getRolesByName(teamName, true);
        if(teamRoles.size() != 1) {
            return "Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!";
        }

        if(memberAccount.discordId != null) {
            DangerBot.getInstance().guild.addRoleToMember(memberAccount.discordId, teamRoles.get(0)).queue();
            DangerBot.getInstance().guild.addRoleToMember(memberAccount.discordId, UserRole.MEMBER.getRole()).queue();
        }

        return "Успешное добавление";
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
            Connection connection = DangerBot.getInstance().database.getConnection();
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
