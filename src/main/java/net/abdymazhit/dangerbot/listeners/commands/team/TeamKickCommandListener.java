package net.abdymazhit.dangerbot.listeners.commands.team;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.customs.info.TeamInfo;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

/**
 * Команда исключить участника из команды
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class TeamKickCommandListener extends TeamLeaveCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member deleter = event.getMember();

        if(deleter == null) return;

        String[] command = message.getContentRaw().split(" ");

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

        int deleterId = DangerBot.getInstance().database.getUserId(deleter.getId());
        if(deleterId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String memberName = command[2];

        UserAccount memberAccount = DangerBot.getInstance().database.getUserAccount(memberName);
        if(memberAccount == null) {
            message.reply("Ошибка! Участник не зарегистрирован на сервере!").queue();
            return;
        }

        TeamInfo teamInfo = DangerBot.getInstance().database.getLeaderTeam(deleterId);
        if(teamInfo == null) {
            message.reply("Ошибка! Вы не являетесь лидером какой-либо команды!").queue();
            return;
        }

        boolean isUserTeamMember = DangerBot.getInstance().database.isUserTeamMember(memberAccount.id, teamInfo.id);
        if(!isUserTeamMember) {
            message.reply("Ошибка! Участник не является участником этой команды!").queue();
            return;
        }

        boolean isMemberDeleted = deleteTeamMember(teamInfo.id, memberAccount.id, deleterId);
        if(!isMemberDeleted) {
            message.reply("Критическая ошибка при удалении участника из команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        List<Role> teamRoles = DangerBot.getInstance().guild.getRolesByName(teamInfo.name, true);
        if(teamRoles.size() != 1) {
            message.reply("Критическая ошибка при получении роли команды! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        if(memberAccount.discordId != null) {
            DangerBot.getInstance().guild.removeRoleFromMember(memberAccount.discordId, teamRoles.get(0)).queue();
            DangerBot.getInstance().guild.removeRoleFromMember(memberAccount.discordId, UserRole.MEMBER.getRole()).queue();
        }

        message.reply("Вы успешно выгнали участника из команды!").queue();
    }
}
