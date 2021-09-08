package net.abdymazhit.mthd.customs;

/**
 * Представляет собой аккаунт пользователя
 *
 * @version   08.09.2021
 * @author    Islam Abdymazhit
 */
public class UserAccount {

    /** Id пользователя */
    private final int id;

    /** Id пользователя в VimeWorld */
    private int vimeId;

    /** Id пользователя в Discord */
    private String discordId;

    /** Имя пользователя */
    private String username;

    /** Статус онлайна пользователя в VimeWorld */
    private boolean isVimeOnline;

    /** Статус онлайна пользователя в Discord */
    private boolean isDiscordOnline;

    /**
     * Инициализирует аккаунт пользователя
     * @param id Id пользователя
     */
    public UserAccount(int id) {
        this.id = id;
    }

    /**
     * Получает id пользователя
     * @return Id пользователя
     */
    public int getId() {
        return id;
    }

    /**
     * Устанавливает id пользователя в VimeWorld
     * @param vimeId Id пользователя в VimeWorld
     */
    public void setVimeId(int vimeId) {
        this.vimeId = vimeId;
    }

    /**
     * Получает id пользователя в VimeWorld
     * @return Id пользователя в VimeWorld
     */
    public int getVimeId() {
        return vimeId;
    }

    /**
     * Устанавливает id пользователя в Discord
     * @param discordId Id пользователя в Discord
     */
    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    /**
     * Получает id пользователя в Discord
     * @return Id пользователя в Discord
     */
    public String getDiscordId() {
        return discordId;
    }

    /**
     * Устанавливает имя пользователя
     * @param username Имя пользователя
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Получает имя пользователя
     * @return Имя пользователя
     */
    public String getUsername() {
        return username;
    }

    /**
     * Устанавливает статус онлайна пользователя в VimeWorld
     * @param vimeOnline Статус онлайна пользователя в VimeWorld
     */
    public void setVimeOnline(boolean vimeOnline) {
        isVimeOnline = vimeOnline;
    }

    /**
     * Получает статус онлайна пользователя в VimeWorld
     * @return Статус онлайна пользователя в VimeWorld
     */
    public boolean isVimeOnline() {
        return isVimeOnline;
    }

    /**
     * Устанавливает статус онлайна пользователя в Discord
     * @param discordOnline Статус онлайна пользователя в Discord
     */
    public void setDiscordOnline(boolean discordOnline) {
        isDiscordOnline = discordOnline;
    }

    /**
     * Получает статус онлайна пользователя в Discord
     * @return Статус онлайна пользователя в Discord
     */
    public boolean isDiscordOnline() {
        return isDiscordOnline;
    }
}
