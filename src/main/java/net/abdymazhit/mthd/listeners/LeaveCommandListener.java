package net.abdymazhit.mthd.listeners;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Команда выхода
 *
 * @version   17.09.2021
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

        if (!event.getName().equals("leave")) return;
        if (!messageChannel.getId().equals(MTHD.getInstance().authChannel.channelId)) return;
        if (member == null) return;

        if (!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            event.reply("Ошибка! Вы не авторизованы!").setEphemeral(true).queue();
            return;
        }

        boolean isDeleted = deleteUser(member.getId());
        if (!isDeleted) {
            event.reply("Ошибка! Попробуйте выйти позже!").setEphemeral(true).queue();
            return;
        }

        if(MTHD.getInstance().guild.getSelfMember().canInteract(member)) {
            for(Role role : member.getRoles()) {
                if(!role.equals(UserRole.ADMIN.getRole()) && !role.equals(UserRole.ASSISTANT.getRole())) {
                    MTHD.getInstance().guild.removeRoleFromMember(member, role).queue();
                }
            }
        }

        if(MTHD.getInstance().guild.getSelfMember().canInteract(member)) {
            member.modifyNickname(member.getId()).queue();
        }

        // Отправить сообщение о успешной авторизации
        event.reply("Вы успешно вышли с аккаунта!").setEphemeral(true).queue();
    }

    private boolean deleteUser(String discordId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement statement = connection.prepareStatement("UPDATE users SET discord_id = null WHERE discord_id = ?;");
            statement.setString(1, discordId);
            statement.executeUpdate();
            statement.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();

            // Вернуть значение, что произошла ошибка
            return false;
        }
    }
}