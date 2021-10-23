package net.abdymazhit.dangerbot.listeners.commands.team;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.info.TeamInfo;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Команда посмотреть информацию о команде
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class TeamInfoCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();

        if(member == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length > 2) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.LEADER.getRole()) && !member.getRoles().contains(UserRole.MEMBER.getRole())) {
            message.reply("Ошибка! Команда доступна только для участников или лидеров команд!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int memberId = DangerBot.getInstance().database.getUserId(member.getId());
        if(memberId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        int memberTeamId = DangerBot.getInstance().database.getUserTeamId(memberId);
        if(memberTeamId < 0) {
            message.reply("Ошибка! Вы не являетесь участником или лидером какой-либо команды!").queue();
            return;
        }

        TeamInfo teamInfo = new TeamInfo(memberTeamId);
        teamInfo.getTeamInfoByDatabase();
        MessageEmbed messageEmbed = DangerBot.getInstance().utils.getTeamInfoMessageEmbed(teamInfo);
        if(messageEmbed == null) {
            message.reply("Ошибка! По неизвестной причине получить информацию о Вашей команде не получилось! Свяжитесь с разработчиком бота!").queue();
        } else {
            message.replyEmbeds(messageEmbed).queue();
        }
    }
}
