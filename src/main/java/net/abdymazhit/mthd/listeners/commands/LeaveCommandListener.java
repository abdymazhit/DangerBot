package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Команда выхода
 *
 * @version   17.10.2021
 * @author    Islam Abdymazhit
 */
public class LeaveCommandListener extends ListenerAdapter {

    /**
     * Событие отправки команды
     */
    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!event.getName().equals("leave")) return;
        if(!messageChannel.getId().equals(MTHD.getInstance().authChannel.channelId)) return;
        if(member == null) return;

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            event.reply("Ошибка! Вы не авторизованы!").setEphemeral(true).queue();
            return;
        }

        int playerId = MTHD.getInstance().database.getUserId(member.getId());
        if(playerId < 0) {
            List<Role> rolesToAdd = new ArrayList<>();
            List<Role> rolesToRemove = new ArrayList<>();
            for(Role role : member.getRoles()) {
                if(!role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())
                   && !role.equals(UserRole.BANNED.getRole())) {
                    rolesToRemove.add(role);
                } else {
                    rolesToAdd.add(role);
                }
            }
            MTHD.getInstance().guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).submit();

            event.reply("Вы успешно вышли с аккаунта!").setEphemeral(true).queue();
            return;
        }

        if(isInGame(playerId)) {
            event.reply("Ошибка! Вы не можете выйти, пока находитесь в поиске игры или уже находитесь в игре!").setEphemeral(true).queue();
            return;
        }

        boolean isDeleted = deleteUser(member.getId());
        if(!isDeleted) {
            event.reply("Ошибка! Попробуйте выйти позже!").setEphemeral(true).queue();
            return;
        }

        List<Role> rolesToAdd = new ArrayList<>();
        List<Role> rolesToRemove = new ArrayList<>();
        for(Role role : member.getRoles()) {
            if(!role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())
               && !role.equals(UserRole.BANNED.getRole())) {
                rolesToRemove.add(role);
            } else {
                rolesToAdd.add(role);
            }
        }
        MTHD.getInstance().guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).submit();

        // Изменить пользователю ник
        if(MTHD.getInstance().guild.getSelfMember().canInteract(member)) {
            member.modifyNickname(member.getUser().getName()).queue();
        }

        // Отправить сообщение о успешной авторизации
        event.reply("Вы успешно вышли с аккаунта!").setEphemeral(true).queue();
    }

    /**
     * Проверяет, находится ли игрок в игре
     * @param playerId Id игрока
     * @return Значение, находится ли игрок в игре
     */
    private boolean isInGame(int playerId) {
        Connection connection = MTHD.getInstance().database.getConnection();

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM players_in_game_search WHERE player_id = ?;");
            statement.setInt(1, playerId);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM single_live_games WHERE first_team_captain_id = ? OR second_team_captain_id = ?;""");
            statement.setInt(1, playerId);
            statement.setInt(2, playerId);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM single_live_games_players WHERE player_id = ?;""");
            statement.setInt(1, playerId);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Удаляет пользователя
     * @param discordId Id дискорда
     * @return Значение, удален ли пользователь
     */
    private boolean deleteUser(String discordId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement statement = connection.prepareStatement("UPDATE users SET discord_id = null WHERE discord_id = ?;");
            statement.setString(1, discordId);
            statement.executeUpdate();

            // Вернуть значение, что пользователь успешно удален
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}