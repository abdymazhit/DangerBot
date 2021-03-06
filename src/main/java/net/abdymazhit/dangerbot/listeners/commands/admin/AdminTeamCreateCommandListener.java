package net.abdymazhit.dangerbot.listeners.commands.admin;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Администраторская команда создания команды
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamCreateCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member creator = event.getMember();

        if(creator == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите название команды!").queue();
            return;
        }

        if(command.length == 3) {
            message.reply("Ошибка! Укажите лидера команды!").queue();
            return;
        }

        if(command.length > 4) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!creator.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!creator.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int creatorId = DangerBot.getInstance().database.getUserId(creator.getId());
        if(creatorId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String teamName = command[2];
        String leaderName = command[3];

        UserAccount leaderAccount = DangerBot.getInstance().database.getUserAccount(leaderName);
        if(leaderAccount == null) {
            message.reply("Ошибка! Лидер не зарегистрирован на сервере!").queue();
            return;
        }

        int leaderTeamId = DangerBot.getInstance().database.getUserTeamId(leaderAccount.id);
        if(leaderTeamId > 0) {
            message.reply("Ошибка! Лидер уже состоит в команде!").queue();
            return;
        }

        if(!DangerBot.getInstance().guild.getRolesByName(teamName, true).isEmpty()) {
            message.reply("Ошибка! Вы пытаетесь занять роль команды, которая уже существует! " +
                          "Возможно вы пытаетесь занять роли сервера: Admin, Assistant... Если Вы уверены, " +
                          "что не занимаете роль сервера свяжитесь с разработчиком бота!").queue();
            return;
        }

        String errorMessage = createTeam(teamName, leaderAccount.id, creatorId);
        if(errorMessage != null) {
            message.reply(errorMessage).queue();
            return;
        }

        DangerBot.getInstance().guild.createCopyOfRole(UserRole.TEST.getRole()).setName(teamName)
                .setColor(10070709).queue(role -> {
            if(leaderAccount.discordId != null) {
                DangerBot.getInstance().guild.addRoleToMember(leaderAccount.discordId, role).queue();
                DangerBot.getInstance().guild.addRoleToMember(leaderAccount.discordId, UserRole.LEADER.getRole()).queue();
            }

            List<Category> categories = DangerBot.getInstance().guild.getCategoriesByName("Team Rating", true);
            if(categories.isEmpty()) {
                throw new IllegalArgumentException("Критическая ошибка! Категория Team Rating не существует!");
            }

            Category category = categories.get(0);
            category.createVoiceChannel(teamName)
                    .addPermissionOverride(role, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(DangerBot.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();

            message.reply("Команда успешно создана! Название команды: %team%, лидер команды: %leader%, роль команды: %role%"
                    .replace("%team%", teamName)
                    .replace("%leader%", leaderName)
                    .replace("%role%", role.getAsMention())).queue();
        });
        DangerBot.getInstance().teamsChannel.updateTopMessage();
    }

    /**
     * Создает команду
     * @param teamName Название команды
     * @param leaderId Id лидера команды
     * @param creatorId Id создателя команды
     * @return Текст ошибки создания команды
     */
    private String createTeam(String teamName, int leaderId, int creatorId) {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement createStatement = connection.prepareStatement("""
                INSERT INTO teams (name, leader_id) SELECT ?, ? WHERE NOT EXISTS
                (SELECT leader_id FROM teams WHERE name LIKE ? AND is_deleted is null);""", Statement.RETURN_GENERATED_KEYS);
            createStatement.setString(1, teamName);
            createStatement.setInt(2, leaderId);
            createStatement.setString(3, teamName);
            createStatement.executeUpdate();
            ResultSet createResultSet = createStatement.getGeneratedKeys();
            if(createResultSet.next()) {
                int teamId = createResultSet.getInt(1);

                PreparedStatement historyStatement = connection.prepareStatement(
                        "INSERT INTO teams_creation_history (team_id, name, leader_id, creator_id, created_at) VALUES (?, ?, ?, ?, ?);");
                historyStatement.setInt(1, teamId);
                historyStatement.setString(2, teamName);
                historyStatement.setInt(3, leaderId);
                historyStatement.setInt(4, creatorId);
                historyStatement.setTimestamp(5, Timestamp.from(Instant.now()));
                historyStatement.executeUpdate();

                // Вернуть значение, что команда успешно создана
                return null;
            } else {
                return "Ошибка! Команда с таким именем уже существует!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при создании команды! Свяжитесь с разработчиком бота!";
        }
    }
}
