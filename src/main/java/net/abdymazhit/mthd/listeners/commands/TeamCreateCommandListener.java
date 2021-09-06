package net.abdymazhit.mthd.listeners.commands;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.*;
import java.time.Instant;

/**
 * Команда создания команды
 *
 * @version   06.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamCreateCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!contentRaw.startsWith("!team create")) return;
        if(!messageChannel.equals(MTHD.getInstance().adminChannel.channel)) return;
        if(member == null) return;

        String[] command = contentRaw.split(" ");

        if(command.length == 2) {
            message.reply("Ошибка! Укажите название команды!").queue();
            return;
        }

        if(command.length == 3) {
            message.reply("Ошибка! Укажите лидера команды!").queue();
            return;
        }

        if(command.length > 4) {
            message.reply("Ошибка! Неверная команда!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        if(!member.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        String creatorName;
        if(member.getNickname() == null) {
            creatorName = member.getEffectiveName();
        } else {
            creatorName = member.getNickname();
        }

        int creatorId = MTHD.getInstance().database.getUserId(creatorName);
        if(creatorId < 0) {
            message.reply("Ошибка! Вы не зарегистрированы в сервере!").queue();
            return;
        }

        String teamName = command[2];
        String leaderName = command[3];

        int leaderId = MTHD.getInstance().database.getUserId(leaderName);
        if(leaderId < 0) {
            message.reply("Ошибка! Лидер не зарегистрирован в сервере!").queue();
            return;
        }

        boolean isUserTeamMember = isUserTeamMember(leaderId);
        if(isUserTeamMember) {
            message.reply("Ошибка! Лидер является участником другой команды!").queue();
            return;
        }

        boolean isUserAlreadyLeader = isUserAlreadyLeader(leaderId);
        if(isUserAlreadyLeader) {
            message.reply("Ошибка! Лидер уже имеет команду!").queue();
            return;
        }

        boolean isTeamAlreadyExists = isTeamAlreadyExists(teamName);
        if(isTeamAlreadyExists) {
            message.reply("Ошибка! Команда уже существует!").queue();
            return;
        }

        boolean isCreated = createTeam(teamName, leaderId, creatorId);
        if(!isCreated) {
            message.reply("Ошибка! По неизвестной причине команда не создалась! Свяжитесь с разработчиком бота!").queue();
            return;
        }

        message.reply("Команда успешно создана! Название команды: " + teamName + ", лидер команды: " + leaderName).queue();
    }

    /**
     * Проверяет, является ли лидер участником команды
     * @param userId Id лидера
     * @return Значение, является ли лидер участником команды
     */
    private boolean isUserTeamMember(int userId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams_members WHERE user_id = ?);");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Проверяет, имеет ли уже лидер команду
     * @param userId Id лидера
     * @return Значение, имеет ли уже лидер команду
     */
    private boolean isUserAlreadyLeader(int userId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams WHERE leader_id = ?);");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Проверяет, существует ли команда по названию
     * @param teamName Название команды
     * @return Значение, существует ли команда
     */
    private boolean isTeamAlreadyExists(String teamName) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams WHERE name ILIKE ? AND is_deleted is null);");
            preparedStatement.setString(1, teamName);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Создает команду
     * @param teamName Название команды
     * @param leaderId Id лидера команды
     * @param creatorId Id создателя команды
     * @return Значение, создана ли команда
     */
    private boolean createTeam(String teamName, int leaderId, int creatorId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO teams (name, leader_id) VALUES (?, ?) RETURNING id;");
            preparedStatement.setString(1, teamName);
            preparedStatement.setInt(2, leaderId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                int teamId = resultSet.getInt("id");

                PreparedStatement historyStatement = connection.prepareStatement(
                        "INSERT INTO teams_creation_history (team_id, name, leader_id, creator_id, created_at) VALUES (?, ?, ?, ?, ?);");
                historyStatement.setInt(1, teamId);
                historyStatement.setString(2, teamName);
                historyStatement.setInt(3, leaderId);
                historyStatement.setInt(4, creatorId);
                historyStatement.setTimestamp(5, Timestamp.from(Instant.now()));
                historyStatement.executeUpdate();
                historyStatement.close();

                // Вернуть значение, что команда добавлена
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
