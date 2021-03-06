package net.abdymazhit.dangerbot.listeners.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Команда авторизации
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class AuthCommandListener extends ListenerAdapter {

    /**
     * Событие отправки команды
     */
    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!event.getName().equals("auth")) return;
        if(!DangerBot.getInstance().authChannel.channel.equals(messageChannel)) return;
        if(member == null) return;

        OptionMapping tokenOption = event.getOption("token");
        if(tokenOption == null) {
            event.reply("Ошибка! Токен авторизации не найден!").setEphemeral(true).queue();
            return;
        }

        if(member.getRoles().contains(UserRole.AUTHORIZED.getRole()))  {
            event.reply("Ошибка! Вы уже авторизованы!").setEphemeral(true).queue();
            return;
        }

        String token = tokenOption.getAsString().replace("https://api.vime.world/web/token/", "")
                .replace(" ", "");
        String authInfo = DangerBot.getInstance().utils.sendGetRequest("https://api.vimeworld.ru/misc/token/%player_token%?token=%token%"
                .replace("%player_token%", token)
                .replace("%token%", DangerBot.getInstance().config.vimeApiToken));
        if(authInfo == null) {
            event.reply("Ошибка! Неверный токен авторизации!").setEphemeral(true).queue();
            return;
        }

        JsonObject authObject = JsonParser.parseString(authInfo).getAsJsonObject();

        JsonElement validElement = authObject.get("valid");
        if(validElement == null) {
            event.reply("Ошибка! Токен авторизации не действителен!").setEphemeral(true).queue();
            return;
        }

        boolean isValid = validElement.getAsBoolean();
        if(!isValid) {
            event.reply("Ошибка! Неверный токен авторизации или время действия токена истекло!").setEphemeral(true).queue();
            return;
        }

        String type = authObject.get("type").getAsString();
        if(!type.equals("AUTH")) {
            event.reply("Ошибка! Тип токена должен быть AUTH!").setEphemeral(true).queue();
            return;
        }

        JsonElement ownerElement = authObject.get("owner");
        if(ownerElement.isJsonNull()) {

            event.reply("Ошибка! Владелец токена не найден!").setEphemeral(true).queue();
            return;
        }

        JsonObject ownerObject = ownerElement.getAsJsonObject();
        String username = ownerObject.get("username").getAsString();
        String level = ownerObject.get("level").getAsString();
        String percent = ownerObject.get("levelPercentage").getAsString();
        String rank = ownerObject.get("rank").getAsString();

        boolean isAdded = addUser(member.getId(), username);
        if(!isAdded) {
            event.reply("Ошибка! Попробуйте авторизоваться позже!").setEphemeral(true).queue();
            return;
        }

        int userId = DangerBot.getInstance().database.getUserId(member.getId());
        if(userId < 0) {
            event.reply("Ошибка! Вы не зарегистрированы на сервере!").setEphemeral(true).queue();
            return;
        }

        // Изменить пользователю ник
        if(DangerBot.getInstance().guild.getSelfMember().canInteract(member)) {
            member.modifyNickname(username).queue();
        }

        // Отправить сообщение о успешной авторизации
        event.replyEmbeds(DangerBot.getInstance().utils.getAuthInfoMessageEmbed(username, level, percent, rank)).setEphemeral(true).queue();
    }

    /**
     * Добавляет пользователя в базу данных
     * @param discordId Discord id пользователя
     * @param username Ник пользователя
     * @return Значение, добавлен ли пользователь
     */
    private boolean addUser(String discordId, String username) {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT discord_id FROM users WHERE username LIKE ?;");
            preparedStatement.setString(1, username);
            int userId = -1;
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                String discord_id = resultSet.getString("discord_id");
                if(discord_id != null) {
                    Member member = DangerBot.getInstance().guild.getMemberById(discordId);
                    if(member != null) {
                        // Удалить роли старого пользователя
                        List<Role> rolesToAdd = new ArrayList<>();
                        List<Role> rolesToRemove = new ArrayList<>();
                        for(Role role : member.getRoles()) {
                            if(!role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())) {
                                rolesToRemove.add(role);
                            } else {
                                rolesToAdd.add(role);
                            }
                        }
                        DangerBot.getInstance().guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).submit();

                        // Изменить ник старого пользователя
                        if(DangerBot.getInstance().guild.getSelfMember().canInteract(member)) {
                            member.modifyNickname(member.getUser().getName()).queue();
                        }
                    }
                }

                PreparedStatement statement = connection.prepareStatement("UPDATE users SET discord_id = ? WHERE username LIKE ? AND discord_id IS NULL;", Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, discordId);
                statement.setString(2, username);
                statement.executeUpdate();

                PreparedStatement idStatement = connection.prepareStatement("SELECT id FROM users WHERE username = ?;");
                idStatement.setString(1, username);
                ResultSet statementResultSet = idStatement.executeQuery();
                if(statementResultSet.next()) {
                    userId = statementResultSet.getInt(1);
                }
            } else {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO users (discord_id, username) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, discordId);
                statement.setString(2, username);
                statement.executeUpdate();
                ResultSet statementResultSet = statement.getGeneratedKeys();
                if(statementResultSet.next()) {
                    userId = statementResultSet.getInt(1);
                }
            }

            PreparedStatement statement = connection.prepareStatement("INSERT INTO users_auth_history (discord_id, user_id, authorized_at) VALUES (?, ?, ?);");
            statement.setString(1, discordId);
            statement.setInt(2, userId);
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.executeUpdate();

            List<Role> teamRolesIsLeader = getTeamRoleIsLeader(userId);
            List<Role> teamRolesIsMember = getTeamRoleIsMember(userId);
            List<Role> singleRatingRole = getSingleRatingRole(userId);
            List<Role> youtubeRole = getYoutuberRole(userId);

            List<Role> rolesToAdd = new ArrayList<>();
            rolesToAdd.addAll(teamRolesIsLeader);
            rolesToAdd.addAll(teamRolesIsMember);
            rolesToAdd.addAll(singleRatingRole);
            rolesToAdd.addAll(youtubeRole);
            rolesToAdd.add(UserRole.AUTHORIZED.getRole());

            Member member = DangerBot.getInstance().guild.getMemberById(discordId);
            if(member != null) {
                DangerBot.getInstance().guild.modifyMemberRoles(member, rolesToAdd, new ArrayList<>()).submit();
            }

            // Вернуть значение, что пользователь добавлен
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Установить роль команды, если пользователь является лидером
     * @param userId Id пользователя
     * @return Добавляемые роли
     */
    public List<Role> getTeamRoleIsLeader(int userId) {
        List<Role> roles = new ArrayList<>();
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT name FROM teams WHERE leader_id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                List<Role> teamRoles = DangerBot.getInstance().guild.getRolesByName(resultSet.getString("name"), true);
                if(teamRoles.size() == 1) {
                    roles.add(UserRole.LEADER.getRole());
                    roles.add(teamRoles.get(0));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }

    /**
     * Установить роль команды, если пользователь является участником
     * @param userId Id пользователя
     * @return Добавляемые роли
     */
    public List<Role> getTeamRoleIsMember(int userId) {
        List<Role> roles = new ArrayList<>();
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT name FROM teams WHERE id = (SELECT team_id FROM teams_members WHERE member_id = ?) AND is_deleted is null;");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                List<Role> teamRoles = DangerBot.getInstance().guild.getRolesByName(resultSet.getString("name"), true);
                if(teamRoles.size() == 1) {
                    roles.add(UserRole.MEMBER.getRole());
                    roles.add(teamRoles.get(0));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }

    /**
     * Установить роль Single Rating
     * @param userId Id пользователя
     * @return Добавляемые роли
     */
    public List<Role> getSingleRatingRole(int userId) {
        List<Role> roles = new ArrayList<>();
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT 1 FROM players WHERE player_id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                roles.add(UserRole.SINGLE_RATING.getRole());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }

    /**
     * Установить роль YouTube
     * @param userId Id пользователя
     * @return Добавляемые роли
     */
    public List<Role> getYoutuberRole(int userId) {
        List<Role> roles = new ArrayList<>();
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT 1 FROM youtubers WHERE youtuber_id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                roles.add(UserRole.YOUTUBE.getRole());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }
}