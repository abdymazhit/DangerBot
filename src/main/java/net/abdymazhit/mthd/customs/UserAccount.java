package net.abdymazhit.mthd.customs;

/**
 * Представляет собой аккаунт пользователя
 *
 * @version   23.09.2021
 * @author    Islam Abdymazhit
 */
public class UserAccount {

    /** Id пользователя */
    public final int id;

    /** Id пользователя в VimeWorld */
    public int vimeId;

    /** Id пользователя в Discord */
    public String discordId;

    /** Имя пользователя */
    public String username;

    /** Название команды пользователя */
    public String teamName;

    /** Статус онлайна пользователя в VimeWorld */
    public boolean isVimeOnline;

    /** Статус онлайна пользователя в Discord */
    public boolean isDiscordOnline;

    /**
     * Инициализирует аккаунт пользователя
     * @param id Id пользователя
     */
    public UserAccount(int id) {
        this.id = id;
    }
}
