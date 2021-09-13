package net.abdymazhit.mthd.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Отвечает за создание таблиц в базе данных
 *
 * @version   13.09.2021
 * @author    Islam Abdymazhit
 */
public class DatabaseTables {

    /** Подключение к базе данных */
    private final Connection connection;

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

        createTeamsNamesRenameHistoryTable();
        createTeamsLeadersTransferHistoryTable();

        createAvailableAssistantsTable();
        createTeamsInGameSearchTable();

        createLiveGamesTable();
        createLiveGamesPlayersTable();

        createCancelledGamesHistory();
        createCancelledGamesPlayersHistory();

        createFinishedGamesHistory();
        createFinishedGamesPlayersHistory();
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
                    "games int default 0 not null, " +
                    "wins int default 0 not null, " +
                    "kills int default 0 not null, " +
                    "deaths int default 0 not null, " +
                    "won_beds int default 0 not null, " +
                    "lost_beds int default 0 not null, " +
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
     * Создает таблицу истории передачи прав лидера команд
     */
    private void createTeamsLeadersTransferHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_leaders_transfer_history (" +
                    "id serial not null constraint teams_leaders_transfer_history_pk primary key, " +
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

    /**
     * Создает таблицу истории переименовании команд
     */
    private void createTeamsNamesRenameHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_names_rename_history (" +
                    "id serial not null constraint teams_names_rename_history_pk primary key, " +
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
     * Создает таблицу доступных помощников
     */
    private void createAvailableAssistantsTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS available_assistants (" +
                    "id serial not null constraint available_assistants_pk primary key, " +
                    "assistant_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу команд в поиске игры
     */
    private void createTeamsInGameSearchTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS teams_in_game_search (" +
                    "id serial not null constraint teams_in_game_search_pk primary key, " +
                    "team_id int not null, " +
                    "format varchar(50) not null, " +
                    "starter_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу игр в прямом эфире
     */
    private void createLiveGamesTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS live_games (" +
                    "id serial not null constraint live_games_pk primary key, " +
                    "first_team_id int not null, " +
                    "first_team_starter_id int not null, " +
                    "second_team_id int not null, " +
                    "second_team_starter_id int not null, " +
                    "format varchar(50) not null, " +
                    "map_name varchar(50), " +
                    "assistant_id int not null, " +
                    "started_at timestamp not null, " +
                    "game_state int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу игроков матчей в прямом эфире
     */
    private void createLiveGamesPlayersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS live_games_players (" +
                    "id serial not null constraint live_games_players_pk primary key, " +
                    "team_id int not null, " +
                    "player_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории отмененных игр
     */
    private void createCancelledGamesHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS cancelled_games_history (" +
                    "id serial not null constraint cancelled_games_history_pk primary key, " +
                    "first_team_id int not null, " +
                    "first_team_starter_id int not null, " +
                    "second_team_id int not null, " +
                    "second_team_starter_id int not null, " +
                    "format varchar(50) not null, " +
                    "map_name varchar(50) not null, " +
                    "canceller_id int not null, " +
                    "cancelled_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории игроков отмененных игр
     */
    private void createCancelledGamesPlayersHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS cancelled_games_players_history (" +
                    "id serial not null constraint cancelled_games_players_history_pk primary key, " +
                    "cancelled_game_id int not null, " +
                    "team_id int not null, " +
                    "player_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории завершенных игр
     */
    private void createFinishedGamesHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS finished_games_history (" +
                    "id serial not null constraint finished_games_history_pk primary key, " +
                    "first_team_id int not null, " +
                    "first_team_starter_id int not null, " +
                    "second_team_id int not null, " +
                    "second_team_starter_id int not null, " +
                    "format varchar(50) not null, " +
                    "map_name varchar(50) not null, " +
                    "finisher_id int not null, " +
                    "finished_at timestamp not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории игроков завершенных игр
     */
    private void createFinishedGamesPlayersHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS finished_games_players_history (" +
                    "id serial not null constraint finished_games_players_history_pk primary key, " +
                    "finished_game_id int not null, " +
                    "team_id int not null, " +
                    "player_id int not null);");
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
