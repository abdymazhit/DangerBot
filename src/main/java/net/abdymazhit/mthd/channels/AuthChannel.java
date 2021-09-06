package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.customs.Channel;

/**
 * Канал авторизации
 *
 * @version   06.09.2021
 * @author    Islam Abdymazhit
 */
public class AuthChannel extends Channel {

    /**
     * Инициализирует канал авторизации
     */
    public AuthChannel() {
        deleteChannel("authorization");
        createChannel("authorization", 0);
        sendChannelMessage();
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
                2. Введите здесь команду `/auth token` для привязки Вашего аккаунта VimeWorld.""").queue();
    }
}
