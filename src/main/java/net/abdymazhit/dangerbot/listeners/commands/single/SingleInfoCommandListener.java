package net.abdymazhit.dangerbot.listeners.commands.single;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.info.PlayerInfo;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;

/**
 * Команда посмотреть информацию о игроке
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class SingleInfoCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();

        if(member == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length > 1) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.SINGLE_RATING.getRole())) {
            message.reply("Ошибка! Вы не владеете статусом Single Rating!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        int playerId = DangerBot.getInstance().database.getUserId(member.getId());
        if(playerId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
            return;
        }

        PlayerInfo playerInfo = DangerBot.getInstance().database.getSinglePlayerInfo(playerId);
        if(playerInfo == null) {
            message.reply("Ошибка! Вы не владеете статусом Single Rating!").queue();
            return;
        }

        File infoFile = DangerBot.getInstance().utils.getPlayerInfoImage(playerInfo);
        if(infoFile == null) {
            message.reply("Ошибка! По неизвестной причине получить информацию о Вас не получилось! Свяжитесь с разработчиком бота!").queue();
        } else {
            message.reply(infoFile).queue();
        }
    }
}
