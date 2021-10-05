package net.abdymazhit.mthd.listeners.commands.team;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.*;
import java.time.Instant;
import java.util.List;

/**
 * Команда удалить команду
 *
 * @version   05.10.2021
 * @author    Islam Abdymazhit
 */
public class TeamDisbandCommandListener {

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

        Team team = MTHD.getInstance().database.getLeaderTeam(deleterId);
        if(team == null) {
            message.reply("Ошибка! Вы не являетесь лидером какой-либо команды!").queue();
            return;
        }

        String errorMessage = disbandTeam(team.id, deleterId);
        if(errorMessage != null) {
            message.reply(errorMessage).queue();
            return;
        }

        List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(team.name, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        // Удалить голосовой канал команды
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Team Rating не существует!");
        }

        Category category = categories.get(0);
        for(VoiceChannel voiceChannel : category.getVoiceChannels()) {
            if(voiceChannel.getName().equals(team.name)) {
                voiceChannel.delete().queue();
            }
        }

        teamRoles.get(0).delete().queue();

        message.reply("Вы успешно удалили команду!").queue();
        MTHD.getInstance().teamsChannel.updateTopMessage();
    }

    /**
     * Удаляет команду
     * @param teamId Id команды
     * @param deleterId Id удаляющего
     * @return Текст ошибки удаления команды
     */
    public String disbandTeam(int teamId, int deleterId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement deleteStatement = connection.prepareStatement(
                "UPDATE teams SET is_deleted = true WHERE id = ? AND is_deleted is null;");
            deleteStatement.setInt(1, teamId);
            deleteStatement.executeUpdate();

            PreparedStatement selectLeaderStatement = connection.prepareStatement(
                "SELECT discord_id FROM users WHERE id = (SELECT leader_id FROM teams WHERE id = ? AND is_deleted is null);");
            selectLeaderStatement.setInt(1, teamId);
            ResultSet leaderResultSet = selectLeaderStatement.executeQuery();
            if(leaderResultSet.next()) {
                String leaderDiscordId = leaderResultSet.getString("discord_id");
                if(leaderDiscordId != null) {
                    MTHD.getInstance().guild.removeRoleFromMember(leaderDiscordId, UserRole.LEADER.getRole()).queue();
                }
            }

            PreparedStatement selectMemberStatement = connection.prepareStatement("""
                    SELECT u.discord_id as discord_id FROM users as u
                    INNER JOIN teams_members as tm ON tm.team_id = ? AND u.id = tm.member_id;""");
            selectMemberStatement.setInt(1, teamId);
            ResultSet selectMemberResultSet = selectMemberStatement.executeQuery();
            while(selectMemberResultSet.next()) {
                String memberDiscordId = selectMemberResultSet.getString("discord_id");
                if(memberDiscordId != null) {
                    MTHD.getInstance().guild.removeRoleFromMember(memberDiscordId, UserRole.MEMBER.getRole()).queue();
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
