package net.abdymazhit.mthd.listeners.commands.team;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Команда посмотреть информацию о команде
 *
 * @version   11.09.2021
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

        int memberId = MTHD.getInstance().database.getUserId(member.getId());
        if(memberId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        int memberTeamId = MTHD.getInstance().database.getUserTeamId(memberId);
        if(memberTeamId < 0) {
            message.reply("Ошибка! Вы не являетесь участником или лидером какой-либо команды!").queue();
            return;
        }

        Team team = new Team(memberTeamId);
        team.getTeamInfoByDatabase();
        MessageEmbed messageEmbed = MTHD.getInstance().utils.getTeamInfoMessageEmbed(team);
        if(messageEmbed == null) {
            message.reply("Ошибка! По неизвестной причине получить информацию о Вашей команде не получилось! Свяжитесь с разработчиком бота!").queue();
        } else {
            message.replyEmbeds(messageEmbed).queue();
        }
    }
}
