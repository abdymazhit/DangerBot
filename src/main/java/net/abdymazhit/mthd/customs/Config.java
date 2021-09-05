package net.abdymazhit.mthd.customs;

/**
 * Представляет собой файл конфигурации
 *
 * @version   05.09.2021
 * @author    Islam Abdymazhit
 */
public class Config {

    /** Токен бота */
    public String token = "BOT-TOKEN";

    /** Токен VimeWorld.ru Public API */
    public String vimeApiToken  = "VIMEWORLD_API_TOKEN";

    /** Параметры базы данных */
    public PostgreSQL postgreSQL = new PostgreSQL();

    /** Параметры базы данных */
    public static class PostgreSQL {
        public String url = "jdbc:postgresql://host:port/database";
        public String username = "username";
        public String password = "password";
    }
}