package net.abdymazhit.mthd.enums;

import net.abdymazhit.mthd.MTHD;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

/**
 * Представляет собой роль пользователя
 *
 * @version   06.09.2021
 * @author    Islam Abdymazhit
 */
public enum UserRole {
    AUTHORIZED("Authorized"),
    LEADER("Leader"),
    TEAM_RATING("Team Rating"),
    SINGLE_RATING("Single Rating"),
    ASSISTANT("Assistant"),
    ADMIN("Admin");

    private final String name;
    private Role role;

    /**
     * Инициализирует роль
     * @param name Название роли
     */
    UserRole(String name) {
        this.name = name;
        List<Role> roles = MTHD.getInstance().guild.getRolesByName(name, true);
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