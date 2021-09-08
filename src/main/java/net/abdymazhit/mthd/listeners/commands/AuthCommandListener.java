package net.abdymazhit.mthd.listeners.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.sql.*;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

/**
 * Команда авторизации
 *
 * @version   08.09.2021
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
        if(!messageChannel.equals(MTHD.getInstance().authChannel.channel)) return;
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

        String token = tokenOption.getAsString().replace("https://api.vime.world/web/token/", "");
        String authInfo = MTHD.getInstance().utils.sendGetRequest("https://api.vimeworld.ru/misc/token/" + token);
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

        // Изменить пользователю ник
        if(MTHD.getInstance().guild.getSelfMember().canInteract(member)) {
            member.modifyNickname(username).queue();
        }

        // Изменить роль пользователя
        MTHD.getInstance().guild.addRoleToMember(member, UserRole.AUTHORIZED.getRole()).queue();

        // Отправить сообщение о успешной авторизации
        event.replyEmbeds(MTHD.getInstance().utils.getAuthInfoMessageEmbed(username, level, percent, rank)).setEphemeral(true).queue();
    }

    /**
     * Добавляет пользователя в базу данных
     * @param memberId Id пользователя
     * @param username Ник пользователя
     * @return Значение, добавлен ли пользователь
     */
    private boolean addUser(String memberId, String username) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT member_id FROM users WHERE username = ?;");
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                String member_id = resultSet.getString("member_id");
                Member member = MTHD.getInstance().guild.retrieveMemberById(member_id).submit().get();

                if(member != null) {
                    // Удалить роли старого пользователя
                    if(MTHD.getInstance().guild.getSelfMember().canInteract(member)) {
                        for(Role role : member.getRoles()) {
                            if(!role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())) {
                                MTHD.getInstance().guild.removeRoleFromMember(member, role).queue();
                            }
                        }
                    }

                    // Изменить ник старого пользователя
                    if(MTHD.getInstance().guild.getSelfMember().canInteract(member)) {
                        member.modifyNickname(member.getId()).queue();
                    }
                }

                PreparedStatement statement = connection.prepareStatement("UPDATE users SET member_id = ? WHERE username = ?;");
                statement.setString(1, memberId);
                statement.setString(2, username);
                statement.executeUpdate();
                statement.close();
            } else {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO users (member_id, username) VALUES (?, ?);");
                statement.setString(1, memberId);
                statement.setString(2, username);
                statement.executeUpdate();
                statement.close();
            }

            int userId = MTHD.getInstance().database.getUserId(username);

            PreparedStatement statement = connection.prepareStatement("INSERT INTO users_auth_history (member_id, user_id, authorized_at) VALUES (?, ?, ?);");
            statement.setString(1, memberId);
            statement.setInt(2, userId);
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.executeUpdate();
            statement.close();

            // Вернуть значение, что пользователь добавлен
            return true;
        } catch (SQLException | ExecutionException | InterruptedException e) {
            e.printStackTrace();

            // Вернуть значение, что произошла ошибка
            return false;
        }
    }
}