package net.abdymazhit.dangerbot.channels;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Channel;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

/**
 * Канал авторизации
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class AuthChannel extends Channel{

    /**
     * Инициализирует канал авторизации
     */
    public AuthChannel() {
        List<TextChannel> textChannels = DangerBot.getInstance().guild.getTextChannelsByName("authorization", true);
        for(TextChannel textChannel : textChannels) {
            textChannel.delete().queue();
        }

        DangerBot.getInstance().guild.createTextChannel("authorization").setPosition(0).queue(textChannel -> {
            channel = textChannel;
            sendChannelMessage();
        });
    }

    /**
     * Отправляет сообщение канала авторизации
     */
    private void sendChannelMessage() {
        channel.sendMessage("""
                Для защиты от ботов, рейдов и прочих неприятных для сервера вещей, у нас включена принудительная привязка к аккаунту VimeWorld.ru.
                Ваш ник на сервере будет соответствовать нику в игре.

                После привязки вам станут доступны все текстовые и голосовые каналы.

                **Как авторизоваться?**
                1. Получите ваш токен авторизации на сервере **VimeWorld MiniGames**. Для получения токена авторизации введите команду `/api auth` на сервере, Вы получите *ссылку*, которая понадобится в следующем шаге.
                2. Введите здесь команду `/auth token` для привязки Вашего аккаунта VimeWorld.

                **Как выйти с аккаунта?**
                Для выхода с аккаунта используйте команду `/leave`""").queue(message -> channelMessage = message);
    }
}
