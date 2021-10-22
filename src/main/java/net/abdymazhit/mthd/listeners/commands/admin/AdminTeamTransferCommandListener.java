package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.abdymazhit.mthd.listeners.commands.team.TeamTransferCommandListener;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Администраторская команда передачи прав лидера команды
 *
 * @version   22.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminTeamTransferCommandListener extends TeamTransferCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member changer = event.getMember();

        if(changer == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите название команды!").queue();
            return;
        }

        if(command.length == 3) {
            message.reply("Ошибка! Укажите ник текущего лидера команды!").queue();
            return;
        }

        if(command.length == 4) {
            message.reply("Ошибка! Укажите ник нового лидера команды!").queue();
            return;
        }

        if(command.length > 5) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!changer.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!changer.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int changerId = MTHD.getInstance().database.getUserId(changer.getId());
        if(changerId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        String teamName = command[2];
        String currentLeaderName = command[3];
        String newLeaderName = command[4];

        int teamId = MTHD.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        UserAccount currentLeaderAccount = MTHD.getInstance().database.getUserAccount(currentLeaderName);
        if(currentLeaderAccount == null) {
            message.reply("Ошибка! Текущий лидер команды не зарегистрирован на сервере!").queue();
            return;
        }

        UserAccount newLeaderAccount = MTHD.getInstance().database.getUserAccount(newLeaderName);
        if(newLeaderAccount == null) {
            message.reply("Ошибка! Новый лидер команды не зарегистрирован на сервере!").queue();
            return;
        }

        boolean isUserTeamLeader = MTHD.getInstance().database.isUserTeamLeader(currentLeaderAccount.id, teamId);
        if(!isUserTeamLeader) {
            message.reply("Ошибка! Текущий лидер команды в настоящий момент не является лидером этой команды!").queue();
            return;
        }

        boolean isUserTeamMember = MTHD.getInstance().database.isUserTeamMember(newLeaderAccount.id, teamId);
        if(!isUserTeamMember) {
            message.reply("Ошибка! Новый лидер команды в настоящий момент не является участником этой команды!").queue();
            return;
        }

        boolean isTransferred = transferLeader(teamId, currentLeaderAccount.id, newLeaderAccount.id, changerId);
        if(!isTransferred) {
            message.reply("Критическая ошибка при передачи прав лидера! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        if(currentLeaderAccount.discordId != null) {
            MTHD.getInstance().guild.removeRoleFromMember(currentLeaderAccount.discordId, UserRole.LEADER.getRole()).queue();
            MTHD.getInstance().guild.addRoleToMember(currentLeaderAccount.discordId, UserRole.MEMBER.getRole()).queue();
        }

        if(newLeaderAccount.discordId != null) {
            MTHD.getInstance().guild.removeRoleFromMember(newLeaderAccount.discordId, UserRole.MEMBER.getRole()).queue();
            MTHD.getInstance().guild.addRoleToMember(newLeaderAccount.discordId, UserRole.LEADER.getRole()).queue();
        }

        message.reply("Права лидера успешно переданы! Название команды: %team%, новый лидер команды: %leader%"
                .replace("%team%", teamName)
                .replace("%leader%", newLeaderName)).queue();
    }
}
