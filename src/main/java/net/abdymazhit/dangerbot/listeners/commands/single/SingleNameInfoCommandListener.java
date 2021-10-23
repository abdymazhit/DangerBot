package net.abdymazhit.dangerbot.listeners.commands.single;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.info.PlayerInfo;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;

/**
 * Команда посмотреть информацию о игроке по названию
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class SingleNameInfoCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();

        if(member == null) return;

        String[] command = message.getContentRaw().split(" ");

        if(command.length == 1) {
            message.reply("Ошибка! Укажите имя игрока!").queue();
            return;
        }

        if(command.length > 2) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        String playerName = command[1];

        int playerId = DangerBot.getInstance().database.getUserIdByUsername(playerName);
        if(playerId < 0) {
            message.reply("Ошибка! Игрок с таким именем не существует!").queue();
            return;
        }

        PlayerInfo playerInfo = DangerBot.getInstance().database.getSinglePlayerInfo(playerId);
        if(playerInfo == null) {
            message.reply("Ошибка! Игрок не владеет статусом Single Rating!").queue();
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
