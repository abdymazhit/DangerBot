package net.abdymazhit.dangerbot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Отвечает за создание таблиц в базе данных
 *
 * @version   06.11.2021
 * @author    Islam Abdymazhit
 */
public record DatabaseTables(Connection connection) {

    /**
     * Создает таблицы в базе данных
     *
     * @param connection Подключение к базе данных
     */
    public DatabaseTables(Connection connection) {
        this.connection = connection;

        createUsersTable();
        createUsersAuthHistoryTable();

        createPlayersTable();
        createPlayersAdditionHistoryTable();
        createPlayersDeletionHistoryTable();

        createPlayersInGameSearchTable();
        createSingleLiveGamesTable();
        createSingleLiveGamesPlayersTable();
        createSingleFinishedGamesHistory();
        createSingleFinishedGamesPlayersHistory();

        createTeamsTable();
        createTeamsCreationHistoryTable();
        createTeamsDeletionHistoryTable();
        createTeamsMembersTable();
        createTeamsMembersAdditionHistoryTable();
        createTeamsMembersDeletionHistoryTable();
        createTeamsNamesRenameHistoryTable();
        createTeamsLeadersTransferHistoryTable();

        createTeamsInGameSearchTable();
        createTeamLiveGamesTable();
        createTeamLiveGamesPlayersTable();
        createTeamFinishedGamesHistory();
        createTeamFinishedGamesPlayersHistory();

        createAvailableAssistantsTable();
        createPlayersBansTable();

        createYoutubersTable();
        createYoutubersAdditionHistoryTable();
        createYoutubersDeletionHistoryTable();

        createStreamsTable();
        createStreamsAdditionHistoryTable();
        createStreamsDeletionHistoryTable();
    }

    /**
     * Создает таблицу пользователей
     */
    private void createUsersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS users (
                        id serial not null AUTO_INCREMENT,
                        discord_id varchar(50),
                        username varchar(50) not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории авторизации пользователей
     */
    private void createUsersAuthHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS users_auth_history (
                        id serial not null AUTO_INCREMENT,
                        discord_id varchar(50) not null,
                        user_id int not null,
                        authorized_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу игроков
     */
    private void createPlayersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS players (
                        id serial not null AUTO_INCREMENT,
                        player_id int not null,
                        points int default 1000 not null,
                        games int default 0 not null,
                        wins int default 0 not null,
                        is_deleted boolean,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории добавления игроков
     */
    private void createPlayersAdditionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS players_addition_history (
                        id serial not null AUTO_INCREMENT,
                        player_id int not null,
                        adder_id int not null,
                        added_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории удаления игроков
     */
    private void createPlayersDeletionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS players_deletion_history (
                        id serial not null AUTO_INCREMENT,
                        player_id int not null,
                        deleter_id int not null,
                        deleted_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу игроков в поиске игры
     */
    private void createPlayersInGameSearchTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS players_in_game_search (
                        id serial not null AUTO_INCREMENT,
                        player_id int not null,
                        format varchar(50) not null,
                        joined_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу игр игроков в прямом эфире Single рейтинга
     */
    private void createSingleLiveGamesTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS single_live_games (
                        id serial not null AUTO_INCREMENT,
                        first_team_captain_id int not null,
                        second_team_captain_id int not null,
                        format varchar(50) not null,
                        map_name varchar(50),
                        assistant_id int not null,
                        started_at timestamp not null,
                        game_state int not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу игроков матчей в прямом эфире Single рейтинга
     */
    private void createSingleLiveGamesPlayersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS single_live_games_players (
                        id serial not null AUTO_INCREMENT,
                        live_game_id int not null,
                        team_id boolean,
                        player_id int not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории завершенных игр Single рейтинга
     */
    private void createSingleFinishedGamesHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS single_finished_games_history (
                        id serial not null AUTO_INCREMENT,
                        first_team_captain_id int not null,
                        second_team_captain_id int not null,
                        format varchar(50) not null,
                        map_name varchar(50) not null,
                        match_id varchar(50) not null,
                        winner_team_id int not null,
                        assistant_id int not null,
                        first_team_rating_changes int not null,
                        second_team_rating_changes int not null,
                        finished_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории игроков завершенных игр Single рейтинга
     */
    private void createSingleFinishedGamesPlayersHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS single_finished_games_players_history (
                    id serial not null AUTO_INCREMENT,
                    finished_game_id int not null,
                    team_id boolean not null,
                    player_id int not null,
                    points int not null,
                    PRIMARY KEY (id));
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу команд
     */
    private void createTeamsTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS teams (
                        id serial not null AUTO_INCREMENT,
                        name varchar(50) not null,
                        leader_id int not null,
                        points int default 1000 not null,
                        games int default 0 not null,
                        wins int default 0 not null,
                        won_beds int default 0 not null,
                        lost_beds int default 0 not null,
                        is_deleted boolean,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории создания команд
     */
    private void createTeamsCreationHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS teams_creation_history (
                        id serial not null AUTO_INCREMENT,
                        team_id int not null,
                        name varchar(50) not null,
                        leader_id int not null,
                        creator_id int not null,
                        created_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории удаления команд
     */
    private void createTeamsDeletionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS teams_deletion_history (
                        id serial not null AUTO_INCREMENT,
                        team_id int not null,
                        deleter_id int not null,
                        deleted_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу участников команд
     */
    private void createTeamsMembersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS teams_members (
                        id serial not null AUTO_INCREMENT,
                        team_id int not null,
                        member_id int not null, PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории добавления участников команд
     */
    private void createTeamsMembersAdditionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS teams_members_addition_history (
                        id serial not null AUTO_INCREMENT,
                        team_id int not null,
                        member_id int not null,
                        adder_id int not null,
                        added_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории удаления участников команд
     */
    private void createTeamsMembersDeletionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS teams_members_deletion_history (
                        id serial not null AUTO_INCREMENT,
                        team_id int not null,
                        member_id int not null,
                        deleter_id int not null,
                        deleted_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории передачи прав лидера команд
     */
    private void createTeamsLeadersTransferHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS teams_leaders_transfer_history (
                        id serial not null AUTO_INCREMENT,
                        team_id int not null,
                        from_id int not null,
                        to_id int not null,
                        changer_id int not null,
                        changed_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории переименовании команд
     */
    private void createTeamsNamesRenameHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS teams_names_rename_history (
                        id serial not null AUTO_INCREMENT,
                        team_id int not null,
                        from_name varchar(50) not null,
                        to_name varchar(50) not null,
                        changer_id int not null,
                        changed_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу доступных помощников
     */
    private void createAvailableAssistantsTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS available_assistants (
                        id serial not null AUTO_INCREMENT,
                        assistant_id int not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу команд в поиске игры
     */
    private void createTeamsInGameSearchTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS teams_in_game_search (
                        id serial not null AUTO_INCREMENT,
                        team_id int not null,
                        format varchar(50) not null,
                        captain_id int not null,
                        joined_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу игр команд в прямом эфире
     */
    private void createTeamLiveGamesTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS team_live_games (
                        id serial not null AUTO_INCREMENT,
                        first_team_id int not null,
                        first_team_captain_id int not null,
                        second_team_id int not null,
                        second_team_captain_id int not null,
                        format varchar(50) not null,
                        map_name varchar(50),
                        assistant_id int not null,
                        started_at timestamp not null,
                        game_state int not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу игроков матчей команд в прямом эфире
     */
    private void createTeamLiveGamesPlayersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS team_live_games_players (
                        id serial not null AUTO_INCREMENT,
                        team_id int not null,
                        player_id int not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории завершенных игр команд
     */
    private void createTeamFinishedGamesHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS team_finished_games_history (
                        id serial not null AUTO_INCREMENT,
                        first_team_id int not null,
                        first_team_captain_id int not null,
                        second_team_id int not null,
                        second_team_captain_id int not null,
                        format varchar(50) not null,
                        map_name varchar(50) not null,
                        match_id varchar(50) not null,
                        winner_team_id int not null,
                        first_team_rating_changes int not null,
                        second_team_rating_changes int not null,
                        assistant_id int not null,
                        finished_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории игроков завершенных игр команд
     */
    private void createTeamFinishedGamesPlayersHistory() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS team_finished_games_players_history (
                    id serial not null AUTO_INCREMENT,
                    finished_game_id int not null,
                    team_id int not null,
                    player_id int not null,
                    PRIMARY KEY (id));
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу заблокированных пользователей
     */
    private void createPlayersBansTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS players_bans (
                    id serial not null AUTO_INCREMENT,
                    player_id int not null,
                    banner_id int,
                    finished_at timestamp not null,
                    PRIMARY KEY (id));
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу ютуберов
     */
    private void createYoutubersTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS youtubers (
                    id serial not null AUTO_INCREMENT,
                    youtuber_id int not null,
                    channel_id varchar(50) not null,
                    is_deleted boolean,
                    PRIMARY KEY (id));
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории добавления ютуберов
     */
    private void createYoutubersAdditionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS youtubers_addition_history (
                        id serial not null AUTO_INCREMENT,
                        youtuber_id int not null,
                        adder_id int not null,
                        added_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории удаления ютуберов
     */
    private void createYoutubersDeletionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS youtubers_deletion_history (
                        id serial not null AUTO_INCREMENT,
                        youtuber_id int not null,
                        deleter_id int not null,
                        deleted_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу активных трансляций
     */
    private void createStreamsTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS streams (
                    id serial not null AUTO_INCREMENT,
                    youtuber_id int not null,
                    link varchar(50) not null,
                    PRIMARY KEY (id));
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории добавления трансляции
     */
    private void createStreamsAdditionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS streams_addition_history (
                        id serial not null AUTO_INCREMENT,
                        youtuber_id int not null,
                        link varchar(50) not null,
                        adder_id int not null,
                        added_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает таблицу истории удаления трансляции
     */
    private void createStreamsDeletionHistoryTable() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS streams_deletion_history (
                        id serial not null AUTO_INCREMENT,
                        youtuber_id int not null,
                        link varchar(50) not null,
                        deleter_id int not null,
                        deleted_at timestamp not null,
                        PRIMARY KEY (id)
                    );
            """);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
