package net.abdymazhit.dangerbot.customs;

/**
 * Представляет собой файл конфигурации
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class Config {

    /** Токен бота */
    public String token = "BOT-TOKEN";

    /** Токен VimeWorld.ru Public API */
    public String vimeApiToken  = "VIMEWORLD_API_TOKEN";

    /** API Key YouTube */
    public String youtubeApiKey  = "YOUTUBE_API_KEY";

    /** Параметры базы данных */
    public MySQL mySQL = new MySQL();

    /** Параметры базы данных */
    public static class MySQL {
        public String url = "jdbc:mysql://host:port/database?autoReconnect=true&enabledTLSProtocols=TLSv1.2";
        public String username = "username";
        public String password = "password";
    }
}