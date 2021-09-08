package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Команда посмотреть информацию о команде
 *
 * @version   08.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamInfoCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!contentRaw.startsWith("!team info")) return;
        if(!messageChannel.equals(MTHD.getInstance().myTeamChannel.channel)) return;
        if(member == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length > 2) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.LEADER.getRole()) && !member.getRoles().contains(UserRole.MEMBER.getRole())) {
            message.reply("Ошибка! Команда доступна только для участников или лидеров команд!").queue();
            return;
        }

        String memberName;
        if(member.getNickname() == null) {
            memberName = member.getEffectiveName();
        } else {
            memberName = member.getNickname();
        }

        int memberId = MTHD.getInstance().database.getUserId(memberName);
        if(memberId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        int teamId;

        teamId = MTHD.getInstance().database.getLeaderTeamId(memberId);
        if(teamId < 0) {
            teamId = MTHD.getInstance().database.getMemberTeamId(memberId);
        }

        if(teamId < 0) {
            message.reply("Ошибка! Вы не являетесь участником или лидеров какой-либо команды!").queue();
            return;
        }

        MessageEmbed messageEmbed = MTHD.getInstance().utils.getTeamInfoMessageEmbed(new Team(teamId));
        if(messageEmbed == null) {
            message.reply("Ошибка! По неизвестной причине " +
                    "получить информацию о Вашей команде не получилось! Свяжитесь с разработчиком бота!").queue();
        } else {
            message.replyEmbeds(messageEmbed).queue();
        }
    }
}
