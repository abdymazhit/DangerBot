package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.abdymazhit.mthd.listeners.commands.team.TeamLeaveCommandListener;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

/**
 * Администраторская команда удаления участника из команды
 *
 * @version   22.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamDeleteCommandListener extends TeamLeaveCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member deleter = event.getMember();

        if(deleter == null) return;

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

        if(!deleter.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
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

        String teamName = command[2];
        String memberName = command[3];

        UserAccount memberAccount = MTHD.getInstance().database.getUserAccount(memberName);
        if(memberAccount == null) {
            message.reply("Ошибка! Участник не зарегистрирован на сервере!").queue();
            return;
        }

        int teamId = MTHD.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        boolean isUserTeamLeader = MTHD.getInstance().database.isUserTeamLeader(memberAccount.id);
        if(isUserTeamLeader) {
            message.reply("Ошибка! Участник является лидером команды! " +
                          "Для удаления участника из команды Вы сперва должны передавать права лидера другому игроку!").queue();
            return;
        }

        boolean isUserTeamMember = MTHD.getInstance().database.isUserTeamMember(memberAccount.id, teamId);
        if(!isUserTeamMember) {
            message.reply("Ошибка! Участник не является участником этой команды!").queue();
            return;
        }

        boolean isMemberDeleted = deleteTeamMember(teamId, memberAccount.id, deleterId);
        if(!isMemberDeleted) {
            message.reply("Критическая ошибка при удалении участника из команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(teamName, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        if(memberAccount.discordId != null) {
            MTHD.getInstance().guild.removeRoleFromMember(memberAccount.discordId, teamRoles.get(0)).queue();
            MTHD.getInstance().guild.removeRoleFromMember(memberAccount.discordId, UserRole.MEMBER.getRole()).queue();
        }

        message.reply("Участник успешно удален из команды! Название команды: %team%, ник участника: %member%"
                .replace("%team%", teamName)
                .replace("%member%", memberName)).queue();
    }
}
