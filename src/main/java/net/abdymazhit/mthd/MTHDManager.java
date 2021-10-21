package net.abdymazhit.mthd;

import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static net.dv8tion.jda.api.exceptions.ErrorResponseException.ignore;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_ROLE;

/**
 * Менеджер сервера
 *
 * @version   21.10.2021
 * @author    Islam Abdymazhit
 */
public class MTHDManager {

    /**
     * Инициализирует менеджер сервера
     */
    public MTHDManager() {
        List<String> teamsNames = getTeamsNames();
        configureVoiceChannelsForTeams(teamsNames);

        Map<String, String> usersDiscordIdsNames = getUsersDiscordIdsNames();
        Map<String, String> playersDiscordIdsNames = getPlayersDiscordIdsNames();

        List<UserAccount> teamMembers = getUsersAsTeamMembers();
        List<UserAccount> teamLeader = getUsersAsTeamLeaders();

        configureUsersRoles(usersDiscordIdsNames, playersDiscordIdsNames, teamMembers, teamLeader);
    }

    /**
     * Получает имена существующих команд
     * @return Имена существующих команд
     */
    private List<String> getTeamsNames() {
        List<String> teamsNames = new ArrayList<>();
        try {
            ResultSet resultSet = MTHD.getInstance().database.getConnection().createStatement()
                    .executeQuery("SELECT name FROM teams WHERE is_deleted is null;");
            while(resultSet.next()) {
                String name = resultSet.getString("name");
                teamsNames.add(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teamsNames;
    }

    /**
     * Настраивает голосовые каналы для существующих команд
     * @param teamsNames Имена существующих команд
     */
    private void configureVoiceChannelsForTeams(List<String> teamsNames) {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Team Rating не существует!");
        }

        Map<String, Role> teamsNamesRoles = new HashMap<>();
        for(String teamName : teamsNames) {
            List<Role> roles = MTHD.getInstance().guild.getRolesByName(teamName, true);
            if(roles.isEmpty()) {
                MTHD.getInstance().guild.createCopyOfRole(UserRole.TEST.getRole())
                        .setName(teamName).setColor(10070709).queue(role -> teamsNamesRoles.put(teamName, role));
            } else {
                if(roles.size() == 1) {
                    teamsNamesRoles.put(teamName, roles.get(0));
                } else {
                    for(int i = 0; i < roles.size() - 1; i++) {
                        roles.get(i).delete().queue();
                    }
                    teamsNamesRoles.put(teamName, roles.get(roles.size() - 1));
                }
            }
        }

        for(Role role : MTHD.getInstance().guild.getRoles()) {
            if(!role.equals(MTHD.getInstance().guild.getBotRole()) && !role.equals(MTHD.getInstance().guild.getPublicRole())
               && !role.equals(MTHD.getInstance().guild.getBoostRole()) && !role.equals(UserRole.SINGLE_RATING.getRole())
               && !role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())
               && !role.equals(UserRole.LEADER.getRole()) && !role.equals(UserRole.MEMBER.getRole())
               && !role.equals(UserRole.AUTHORIZED.getRole()) && !role.equals(UserRole.TEST.getRole())) {
                if(!teamsNamesRoles.containsKey(role.getName())) {
                    role.delete().queue();
                }
            }
        }

        Category category = categories.get(0);

        List<String> voiceChannelsIds = new ArrayList<>();
        for(VoiceChannel voiceChannel : category.getVoiceChannels()) {
            voiceChannelsIds.add(voiceChannel.getId());
        }

        for(String teamName : teamsNames) {
            boolean hasVoiceChannel = false;

            for(VoiceChannel voiceChannel : category.getVoiceChannels()) {
                if(voiceChannel.getName().equals(teamName)) {
                    voiceChannelsIds.remove(voiceChannel.getId());
                    hasVoiceChannel = true;
                }
            }

            if(!hasVoiceChannel) {
                List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(teamName, true);
                if(teamRoles.size() == 1) {
                    category.createVoiceChannel(teamName)
                            .addPermissionOverride(teamRoles.get(0), EnumSet.of(Permission.VIEW_CHANNEL), null)
                            .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                            .queue();
                } else {
                    throw new IllegalArgumentException("Критическая ошибка! Команда %team% не имеет роли или имеет больше 1 роли!"
                            .replace("%team%", teamName));
                }
            }
        }

        for(String id : voiceChannelsIds) {
            VoiceChannel voiceChannel = MTHD.getInstance().guild.getVoiceChannelById(id);
            if(voiceChannel != null) {
                voiceChannel.delete().queue();
            }
        }
    }

    /**
     * Получает discord id и имена всех пользователей
     * @return Discord id и имена всех пользователей
     */
    private Map<String, String> getUsersDiscordIdsNames() {
        Map<String, String> usersDiscordIdsNames = new HashMap<>();
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery(
                    "SELECT discord_id, username FROM users WHERE discord_id is not null;");
            while(resultSet.next()) {
                String discordId = resultSet.getString("discord_id");
                String username = resultSet.getString("username");
                usersDiscordIdsNames.put(discordId, username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usersDiscordIdsNames;
    }

    /**
     * Получает discord id и имена всех игроков Single Rating
     * @return Discord id и имена всех игроков Single Rating
     */
    private Map<String, String> getPlayersDiscordIdsNames() {
        Map<String, String> playersDiscordIdsNames = new HashMap<>();
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT discord_id, username FROM users as u
                INNER JOIN players as p ON p.player_id = u.id AND p.is_deleted is null;""");
            while(resultSet.next()) {
                String discordId = resultSet.getString("discord_id");
                String username = resultSet.getString("username");
                playersDiscordIdsNames.put(discordId, username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playersDiscordIdsNames;
    }

    /**
     * Получает всех участников команд
     * @return Все участники команд
     */
    private List<UserAccount> getUsersAsTeamMembers() {
        List<UserAccount> userAccounts = new ArrayList<>();
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT tm.member_id id, u.discord_id discord_id, t.name team_name
                FROM teams_members as tm
                INNER JOIN teams as t ON t.id = tm.team_id
                INNER JOIN users as u ON u.id = tm.member_id AND u.discord_id is not null;""");
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String discordId = resultSet.getString("discord_id");
                String teamName = resultSet.getString("team_name");

                UserAccount userAccount = new UserAccount(id);
                userAccount.discordId = discordId;
                userAccount.teamName = teamName;

                userAccounts.add(userAccount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userAccounts;
    }

    /**
     * Получает всех лидеров команд
     * @return Все лидеры команд
     */
    private List<UserAccount> getUsersAsTeamLeaders() {
        List<UserAccount> userAccounts = new ArrayList<>();
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT t.leader_id id, u.discord_id discord_id, t.name team_name FROM teams as t
                INNER JOIN users as u ON u.id = t.leader_id AND t.is_deleted is null AND u.discord_id is not null;""");
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                String discordId = resultSet.getString("discord_id");
                String teamName = resultSet.getString("team_name");

                UserAccount userAccount = new UserAccount(id);
                userAccount.discordId = discordId;
                userAccount.teamName = teamName;

                userAccounts.add(userAccount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userAccounts;
    }

    /**
     * Настраивает роли всех пользователей
     * @param usersDiscordIdsNames Discord id и имена всех пользователей
     * @param playersDiscordIdsNames Discord id и имена игроков Single Rating
     * @param teamMembers Все участники команд
     * @param teamLeaders Все лидеры команд
     */
    private void configureUsersRoles(Map<String, String> usersDiscordIdsNames, Map<String, String> playersDiscordIdsNames, List<UserAccount> teamMembers, List<UserAccount> teamLeaders) {
        MTHD.getInstance().guild.loadMembers().onSuccess(members -> {
            for(Member member : members) {
                if(!member.getUser().isBot()) {
                    String discordId = member.getId();
                    if(!usersDiscordIdsNames.containsKey(discordId)) {
                        for(Role role : member.getRoles()) {
                            if(!role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())) {
                                if(member.getRoles().contains(role)) {
                                    MTHD.getInstance().guild.removeRoleFromMember(member, role).queue();
                                }
                            }
                        }
                    } else {
                        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
                            MTHD.getInstance().guild.addRoleToMember(member, UserRole.AUTHORIZED.getRole()).queue();
                        }

                        // Изменить ник пользователю
                        if(MTHD.getInstance().guild.getSelfMember().canInteract(member)) {
                            MTHD.getInstance().guild.modifyNickname(member, usersDiscordIdsNames.get(discordId)).queue();
                        }

                        if(!playersDiscordIdsNames.containsKey(discordId)) {
                            if(member.getRoles().contains(UserRole.SINGLE_RATING.getRole())) {
                                MTHD.getInstance().guild.removeRoleFromMember(member, UserRole.SINGLE_RATING.getRole()).queue();
                            }
                        } else {
                            if(!member.getRoles().contains(UserRole.SINGLE_RATING.getRole())) {
                                MTHD.getInstance().guild.addRoleToMember(member, UserRole.SINGLE_RATING.getRole()).queue();
                            }
                        }

                        configureUserTeamRole(member, teamMembers, teamLeaders);
                    }
                }
            }
        });
    }

    /**
     * Настраивает роли пользователя
     * @param member Пользователь
     * @param teamMembers Все участники команд
     * @param teamLeaders Все лидеры команд
     */
    private void configureUserTeamRole(Member member, List<UserAccount> teamMembers, List<UserAccount> teamLeaders) {
        for(UserAccount userAccount : teamMembers) {
            if(userAccount.discordId.equals(member.getId())) {
                List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(userAccount.teamName, true);
                if(teamRoles.size() == 1) {
                    if(!member.getRoles().contains(UserRole.MEMBER.getRole())) {
                        MTHD.getInstance().guild.addRoleToMember(member, UserRole.MEMBER.getRole()).queue();
                    }
                    if(!member.getRoles().contains(teamRoles.get(0))) {
                        MTHD.getInstance().guild.addRoleToMember(member, teamRoles.get(0)).queue();
                    }
                } else {
                    throw new IllegalArgumentException("Критическая ошибка! Команда %team% не имеет роли или имеет больше 1 роли!"
                            .replace("%team%", userAccount.teamName));
                }

                for(Role role : member.getRoles()) {
                    if(!role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())
                       && !role.equals(UserRole.AUTHORIZED.getRole()) && !role.equals(UserRole.MEMBER.getRole())
                       && !role.equals(UserRole.SINGLE_RATING.getRole())) {
                        if(!role.getName().equals(userAccount.teamName)) {
                            if(member.getRoles().contains(role)) {
                                MTHD.getInstance().guild.removeRoleFromMember(member, role).queue();
                            }
                        }
                    }
                }
                return;
            }
        }

        for(UserAccount userAccount : teamLeaders) {
            if(userAccount.discordId.equals(member.getId())) {
                List<Role> teamRoles = MTHD.getInstance().guild.getRolesByName(userAccount.teamName, true);
                if(teamRoles.size() == 1) {
                    if(!member.getRoles().contains(UserRole.LEADER.getRole())) {
                        MTHD.getInstance().guild.addRoleToMember(member, UserRole.LEADER.getRole()).queue();
                    }
                    if(!member.getRoles().contains(teamRoles.get(0))) {
                        MTHD.getInstance().guild.addRoleToMember(member, teamRoles.get(0)).queue();
                    }
                } else {
                    throw new IllegalArgumentException("Критическая ошибка! Команда %team% не имеет роли или имеет больше 1 роли!"
                            .replace("%team%", userAccount.teamName));
                }

                for(Role role : member.getRoles()) {
                    if(!role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())
                       && !role.equals(UserRole.AUTHORIZED.getRole()) && !role.equals(UserRole.LEADER.getRole())
                       && !role.equals(UserRole.SINGLE_RATING.getRole())) {
                        if(!role.getName().equals(userAccount.teamName)) {
                            if(member.getRoles().contains(role)) {
                                MTHD.getInstance().guild.removeRoleFromMember(member, role).queue();
                            }
                        }
                    }
                }
                return;
            }
        }

        for(Role role : member.getRoles()) {
            if(!role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())
               && !role.equals(UserRole.AUTHORIZED.getRole()) && !role.equals(UserRole.SINGLE_RATING.getRole())) {
                if(member.getRoles().contains(role)) {
                    MTHD.getInstance().guild.removeRoleFromMember(member, role).queue(null, ignore(UNKNOWN_ROLE));
                }
            }
        }
    }
}
