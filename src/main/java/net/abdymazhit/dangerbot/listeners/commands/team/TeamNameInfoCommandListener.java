package net.abdymazhit.dangerbot.listeners.commands.team;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.info.TeamInfo;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Команда посмотреть информацию о команде по названию
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class TeamNameInfoCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();

        if(member == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите название команды!").queue();
            return;
        }

        if(command.length > 3) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        String teamName = command[2];

        int teamId = DangerBot.getInstance().database.getTeamId(teamName);
        if(teamId < 0) {
            message.reply("Ошибка! Команда с таким именем не существует!").queue();
            return;
        }

        TeamInfo teamInfo = new TeamInfo(teamId);
        teamInfo.getTeamInfoByDatabase();
        MessageEmbed messageEmbed = DangerBot.getInstance().utils.getTeamInfoMessageEmbed(teamInfo);
        if(messageEmbed == null) {
            message.reply("Ошибка! По неизвестной причине получить информацию о команде не получилось! Свяжитесь с разработчиком бота!").queue();
        } else {
            message.replyEmbeds(messageEmbed).queue();
        }
    }
}
