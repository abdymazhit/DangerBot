package net.abdymazhit.mthd.customs;

import net.abdymazhit.mthd.MTHD;
import net.dv8tion.jda.api.entities.Member;

/**
 * Представляет собой аккаунт пользователя
 *
 * @version   22.10.2021
 * @author    Islam Abdymazhit
 */
public class UserAccount {

    /** Id пользователя */
    public int id;

    /** Id пользователя в VimeWorld */
    public int vimeId;

    /** Id пользователя в Discord */
    public String discordId;

    /** Discord member пользователя */
    public Member member;

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
        username = MTHD.getInstance().database.getUserName(id);
        discordId = MTHD.getInstance().database.getUserDiscordId(username);
        member = MTHD.getInstance().guild.getMemberById(discordId);
    }

    /**
     * Инициализирует аккаунт пользователя
     */
    public UserAccount(String username) {
        this.username = username;
        discordId = MTHD.getInstance().database.getUserDiscordId(username);
        member = MTHD.getInstance().guild.getMemberById(discordId);
    }
}
