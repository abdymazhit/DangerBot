package net.abdymazhit.dangerbot.enums;

import net.abdymazhit.dangerbot.DangerBot;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

/**
 * Представляет собой роль пользователя
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public enum UserRole {
    TEST("Test"),
    AUTHORIZED("Authorized"),
    MEMBER("Member"),
    LEADER("Leader"),
    SINGLE_RATING("Single Rating"),
    ASSISTANT("Assistant"),
    YOUTUBE("YouTube"),
    ADMIN("Admin");

    private final String name;
    private Role role;

    /**
     * Инициализирует роль
     * @param name Название роли
     */
    UserRole(String name) {
        this.name = name;
        List<Role> roles = DangerBot.getInstance().guild.getRolesByName(name, true);
        if(!roles.isEmpty()) {
            this.role = roles.get(0);
        }
    }

    /**
     * Получает название роли
     * @return Название роли
     */
    public String getName() {
        return name;
    }

    /**
     * Получает роль
     * @return Роль
     */
    public Role getRole() {
        return role;
    }
}