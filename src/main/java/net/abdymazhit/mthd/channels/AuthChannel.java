package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

/**
 * Канал авторизации
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public class AuthChannel extends Channel{

    /**
     * Инициализирует канал авторизации
     */
    public AuthChannel() {
        List<TextChannel> textChannels = MTHD.getInstance().guild.getTextChannelsByName("authorization", true);
        for(TextChannel textChannel : textChannels) {
            textChannel.delete().queue();
        }

        MTHD.getInstance().guild.createTextChannel("authorization").setPosition(0).queue(textChannel -> {
            channelId = textChannel.getId();
                    textChannel.sendMessage("Для защиты от ботов, рейдов и прочих неприятных для сервера вещей, у нас включена принудительная привязка к аккаунту VimeWorld.ru.\n" +
                    "Ваш ник на сервере будет соответствовать нику в игре.\n" +
                    "\n" +
                    "После привязки вам станут доступны все текстовые и голосовые каналы.\n" +
                    "\n" +
                    "**Как авторизоваться?**\n" +
                    "1. Получите ваш токен авторизации на сервере **VimeWorld MiniGames**. Для получения токена авторизации введите команду `/api auth` на сервере, Вы получите *ссылку*, которая понадобится в следующем шаге.\n" +
                    "2. Введите здесь команду `/auth token` для привязки Вашего аккаунта VimeWorld.").queue(message -> channelMessageId = message.getId());
        });
    }
}
