package net.abdymazhit.dangerbot.customs;

import net.abdymazhit.dangerbot.DangerBot;
import net.dv8tion.jda.api.entities.Member;

/**
 * Представляет собой аккаунт пользователя
 *
 * @version   24.10.2021
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

    /**
     * Инициализирует аккаунт пользователя
     * @param id Id пользователя
     */
    public UserAccount(int id) {
        this.id = id;
        username = DangerBot.getInstance().database.getUserName(id);
        discordId = DangerBot.getInstance().database.getUserDiscordId(username);
        if(discordId != null) {
            member = DangerBot.getInstance().guild.getMemberById(discordId);
        }
    }

    /**
     * Инициализирует аккаунт пользователя
     */
    public UserAccount(String username) {
        this.username = username;
        discordId = DangerBot.getInstance().database.getUserDiscordId(username);
        if(discordId != null) {
            member = DangerBot.getInstance().guild.getMemberById(discordId);
        }
    }
}
