package net.abdymazhit.mthd.database;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Config;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.customs.UserAccount;

import java.sql.*;

/**
 * Отвечает за работу с базой данных
 *
 * @version   09.09.2021
 * @author    Islam Abdymazhit
 */
public class Database {

    /** Подключение к базе данных */
    private Connection connection;

    /**
     * Подключается к базе данных
     */
    public Database() {
        Config.PostgreSQL config = MTHD.getInstance().config.postgreSQL;

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        try {
            connection = DriverManager.getConnection(config.url, config.username, config.password);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        if(connection == null) {
            throw new IllegalArgumentException("Не удалось подключиться к базе данных");
        }

//        Создать таблицы, только при необходимости
        new DatabaseTables(connection);
    }

    /**
     * Получает id пользователя
     * @param discordId Discord id пользователя
     * @return Id пользователя
     */
    public int getUserId(String discordId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id FROM users WHERE member_id = ?;");
            preparedStatement.setString(1, discordId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает id и discord id пользователя
     * @param username Ник пользователя
     * @return Id и discord id пользователя
     */
    public UserAccount getUserIdAndDiscordId(String username) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id, member_id FROM users WHERE username ILIKE ?;");
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                UserAccount userAccount = new UserAccount(resultSet.getInt("id"));
                userAccount.setDiscordId(resultSet.getString("member_id"));
                return userAccount;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает id команды пользователя
     * @param userId Id пользователя
     * @return Id команды пользователя
     */
    public int getUserTeamId(int userId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id team_id FROM teams WHERE leader_id = ? AND is_deleted IS NULL " +
                            "UNION SELECT team_id FROM teams_members WHERE member_id = ?;"
            );
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getInt("team_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает id команды
     * @param teamName Название команды
     * @return Id команды
     */
    public int getTeamId(String teamName) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id FROM teams WHERE name ILIKE ? AND is_deleted is null;");
            preparedStatement.setString(1, teamName);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Проверяет, является ли пользователь лидером команды
     * @param userId Id пользователя
     * @return Значение, является ли пользователь лидером команды
     */
    public boolean isUserTeamLeader(int userId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams WHERE leader_id = ? AND is_deleted is null);");
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
     * Проверяет, является ли пользователь лидером команды
     * @param userId Id пользователя
     * @param teamId Id команды
     * @return Значение, является ли пользователь лидером команды
     */
    public boolean isUserTeamLeader(int userId, int teamId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams WHERE leader_id = ? AND id = ? AND is_deleted is null);");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, teamId);
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
     * Проверяет, является ли пользователь участником команды
     * @param userId Id пользователя
     * @param teamId Id команды
     * @return Значение, является ли пользователь участником команды
     */
    public boolean isUserTeamMember(int userId, int teamId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM teams_members WHERE member_id = ? AND team_id = ?);");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, teamId);
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
     * Получает id команды
     * @param teamName Название команды
     * @return Id команды
     */
    public int getTeamIdByExactName(String teamName) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id FROM teams WHERE name = ? AND is_deleted is null;");
            preparedStatement.setString(1, teamName);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает команду лидера
     * @param leaderId Id лидера
     * @return Команда лидера
     */
    public Team getLeaderTeam(int leaderId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id, name FROM teams WHERE leader_id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, leaderId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                Team team = new Team(resultSet.getInt("id"));
                team.name = resultSet.getString("name");
                return team;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает команду участника
     * @param memberId Id участника
     * @return Команда участника
     */
    public Team getMemberTeam(int memberId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id, name FROM teams WHERE id = (SELECT team_id FROM teams_members WHERE member_id = ?) AND is_deleted is null;");
            preparedStatement.setInt(1, memberId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                Team team = new Team(resultSet.getInt("id"));
                team.name = resultSet.getString("name");
                return team;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает подключение к базе данных
     * @return Подключение к базе данных
     */
    public Connection getConnection() {
        return connection;
    }
}