package net.abdymazhit.mthd.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Отвечает за создание таблиц в базе данных
 *
 * @version   07.09.2021
 * @author    Islam Abdymazhit
 */
public record DatabaseTables(Connection connection) {

    /**
     * Создает таблицы в базе данных
     * @param connection Подключение к базе данных
     */
    public DatabaseTables(Connection connection) {
        this.connection = connection;

        createUsersTable();
        createUsersAuthHistoryTable();

        createSingleRatingTable();
        createTeamRatingTable();

        createTeamsTable();
        createTeamsCreationHistoryTable();
        createTeamsDeletionHistoryTable();

        createTeamsMembersTable();
        createTeamsMembersAdditionHistoryTable();
        createTeamsMembersDeletionHistoryTable();

        createTeamsNamesChangeHistory();
        createTeamsLeadersChangeHistory();
    }

    /**
     * Создает таблицу пользователей
     */
    private void createUsersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS users (" +
                    "id serial not null constraint users_pk primary key, " +
                    "member_id varchar(50) not null, " +
                    "username varchar(50) not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории авторизации пользователей
     */
    private void createUsersAuthHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS users_auth_history (" +
                    "id serial not null constraint users_auth_history_pk primary key, " +
                    "member_id varchar(50) not null, " +
                    "user_id int not null, " +
                    "authorized_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу обладетелей Single Rating
     */
    private void createSingleRatingTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS single_rating (" +
                    "id serial not null constraint single_rating_pk primary key, " +
                    "user_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу обладетелей Team Rating
     */
    private void createTeamRatingTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS team_rating (" +
                    "id serial not null constraint team_rating_pk primary key, " +
                    "user_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу команд
     */
    private void createTeamsTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams (" +
                    "id serial not null constraint teams_pk primary key, " +
                    "name varchar(50) not null, " +
                    "leader_id int not null, " +
                    "points int default 0 not null, " +
                    "is_deleted boolean);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории создания команд
     */
    private void createTeamsCreationHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_creation_history (" +
                    "id serial not null constraint teams_creation_history_pk primary key, " +
                    "team_id int not null, " +
                    "name varchar(50) not null, " +
                    "leader_id int not null, " +
                    "creator_id int not null, " +
                    "created_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории удаления команд
     */
    private void createTeamsDeletionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_deletion_history (" +
                    "id serial not null constraint teams_deletion_history_pk primary key, " +
                    "team_id int not null, " +
                    "deleter_id int not null, " +
                    "deleted_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу участников команд
     */
    private void createTeamsMembersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_members (" +
                    "id serial not null constraint teams_members_pk primary key, " +
                    "team_id int not null, " +
                    "member_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории добавления участников команд
     */
    private void createTeamsMembersAdditionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_members_addition_history (" +
                    "id serial not null constraint teams_members_addition_history_pk primary key, " +
                    "team_id int not null, " +
                    "member_id int not null, " +
                    "adder_id int not null, " +
                    "added_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории удаления участников команд
     */
    private void createTeamsMembersDeletionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_members_deletion_history (" +
                    "id serial not null constraint teams_members_deletion_history_pk primary key, " +
                    "team_id int not null, " +
                    "member_id int not null, " +
                    "deleter_id int not null, " +
                    "deleted_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории изменения названии команд
     */
    private void createTeamsNamesChangeHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_names_change_history (" +
                    "id serial not null constraint teams_names_change_history_pk primary key, " +
                    "team_id int not null, " +
                    "from_name varchar(50) not null, " +
                    "to_name varchar(50) not null, " +
                    "changer_id int not null, " +
                    "changed_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории изменения лидеров команд
     */
    private void createTeamsLeadersChangeHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_leaders_change_history (" +
                    "id serial not null constraint teams_leaders_change_history_pk primary key, " +
                    "team_id int not null, " +
                    "from_id int not null, " +
                    "to_id int not null, " +
                    "changer_id int not null, " +
                    "changed_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
