package net.abdymazhit.mthd.database;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Config;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.customs.UserAccount;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Отвечает за работу с базой данных
 *
 * @version   17.09.2021
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
                    "SELECT id FROM users WHERE discord_id = ?;");
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
     * Получает discord id пользователя
     * @param userId Id пользователя
     * @return Discord id пользователя
     */
    public String getUserDiscordId(int userId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT discord_id FROM users WHERE id = ?;");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getString("discord_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
                    "SELECT id, discord_id FROM users WHERE username ILIKE ?;");
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                UserAccount userAccount = new UserAccount(resultSet.getInt("id"));
                userAccount.setDiscordId(resultSet.getString("discord_id"));
                return userAccount;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает id пользователя
     * @param username Имя пользователя
     * @return Id пользователя
     */
    public int getUserIdByUsername(String username) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id FROM users WHERE username ILIKE ?;");
            preparedStatement.setString(1, username);
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
     * Получает имя пользователя
     * @param userId Id пользователя
     * @return Имя пользователя
     */
    public String getUserName(int userId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT username FROM users WHERE id = ?;");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getString("username");
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

    public int getTeamPoints(int teamId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT points FROM teams WHERE id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, teamId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getInt("points");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Получает название команды
     * @param teamId Id команды
     * @return Название команды
     */
    public String getTeamName(int teamId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT name FROM teams WHERE id = ?;");
            preparedStatement.setInt(1, teamId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                return resultSet.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
     * Добавляет помощника в таблицу доступных помощников
     * @param assistantId Id помощника
     * @return Текст ошибки добавления
     */
    public String setReady(int assistantId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO available_assistants (assistant_id) SELECT ? " +
                            "WHERE NOT EXISTS (SELECT assistant_id FROM available_assistants WHERE assistant_id = ?) " +
                            "RETURNING id;");
            preparedStatement.setInt(1, assistantId);
            preparedStatement.setInt(2, assistantId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                // Вернуть значение, что помощник успешно добавлен в таблицу доступных помощников
                return null;
            } else {
                return "Ошибка! Вы уже в списке доступных помощников!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении в таблицу доступных помощников! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Удаляет помощника из таблицы доступных помощников
     * @param assistantId Id помощника
     * @return Текст ошибки удаления
     */
    public String setUnready(int assistantId) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "DELETE FROM available_assistants WHERE assistant_id = ? RETURNING id;");
            preparedStatement.setInt(1, assistantId);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                // Вернуть значение, что помощник успешно удален из таблицы доступных помощников
                return null;
            } else {
                return "Ошибка! Вас нет в списке доступных помощников!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении из таблицы доступных помощников! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Получает названий игроков команды
     * @param teamPlayersId Список игроков по id
     * @return Список названий игроков команды
     */
    public List<String> getTeamPlayersNames(List<Integer> teamPlayersId) {
        List<String> teamPlayersNames = new ArrayList<>();
        for(int userId : teamPlayersId) {
            teamPlayersNames.add(MTHD.getInstance().database.getUserName(userId));
        }
        return teamPlayersNames;
    }

    /**
     * Получает подключение к базе данных
     * @return Подключение к базе данных
     */
    public Connection getConnection() {
        return connection;
    }
}