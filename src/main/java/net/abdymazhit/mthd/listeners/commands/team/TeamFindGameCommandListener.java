package net.abdymazhit.mthd.listeners.commands.team;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Команда поиск игры
 *
 * @version   05.10.2021
 * @author    Islam Abdymazhit
 */
public class TeamFindGameCommandListener extends ListenerAdapter {

    /**
     * Событие получения сообщений
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member member = event.getMember();

        if(!messageChannel.getId().equals(MTHD.getInstance().teamFindGameChannel.channelId)) return;
        if(member == null) return;
        if(event.getAuthor().isBot()) return;

        String contentRaw = message.getContentRaw();
        String[] command = contentRaw.split(" ");

        if(contentRaw.startsWith("!find")) {
            if(!member.getRoles().contains(UserRole.LEADER.getRole()) &&
               !member.getRoles().contains(UserRole.MEMBER.getRole()) ) {
                message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                return;
            }

            if(!member.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
                message.reply("Ошибка! Вы не авторизованы!").queue();
                return;
            }

            if(contentRaw.startsWith("!find game")) {
                if(command.length == 2) {
                    message.reply("Ошибка! Укажите формат игры!").queue();
                    return;
                }

                if(command.length > 3) {
                    message.reply("Ошибка! Неверная команда!").queue();
                    return;
                }

                String format = command[2];

                if(!format.equals("4x2") && !format.equals("6x2")) {
                    message.reply("Ошибка! Неверный формат игры!").queue();
                    return;
                }

                int memberId = MTHD.getInstance().database.getUserId(member.getId());
                if(memberId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                int teamId = MTHD.getInstance().database.getUserTeamId(memberId);
                if(teamId < 0) {
                    message.reply("Ошибка! Вы не состоите в команде!").queue();
                    return;
                }

                Team team = new Team(teamId);
                team.getTeamInfoByDatabase();
                List<UserAccount> members = new ArrayList<>();
                members.add(team.leader);
                members.addAll(team.members);

                int onlinePlayers = 0;
                for(UserAccount player : members) {
                    if(player.isDiscordOnline || player.isVimeOnline) {
                        onlinePlayers++;
                    }
                }

                if(format.equals("4x2")) {
                    if(onlinePlayers < 4) {
                        message.reply("Ошибка! Недостаточное количество игроков в сети для входа в поиск игры!").queue();
                        return;
                    }
                } else {
                    if(onlinePlayers < 6) {
                        message.reply("Ошибка! Недостаточное количество игроков в сети для входа в поиск игры!").queue();
                        return;
                    }
                }

                List<Integer> teamsInLiveGames = getTeamsInLiveGames();
                if(teamsInLiveGames == null) {
                    message.reply("Ошибка! Не удалось получить список команд в активных играх!").queue();
                    return;
                }

                if(teamsInLiveGames.contains(teamId)) {
                    message.reply("Ошибка! Ваша команда уже участвует в игре!").queue();
                    return;
                }

                String errorMessage = addTeamToTeamsInGameSearch(teamId, format, memberId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                message.reply("Ваша команда успешно добавлена в поиск игры!").queue();
                MTHD.getInstance().teamFindGameChannel.updateTeamsInGameSearchCountMessage();
                MTHD.getInstance().gameManager.teamGameManager.tryStartGame();
            } else if(contentRaw.equals("!find leave")) {
                int memberId = MTHD.getInstance().database.getUserId(member.getId());
                if(memberId < 0) {
                    message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                    return;
                }

                int teamId = MTHD.getInstance().database.getUserTeamId(memberId);
                if(teamId < 0) {
                    message.reply("Ошибка! Вы не состоите в команде!").queue();
                    return;
                }

                String errorMessage = removeTeamFromTeamsInGameSearch(teamId);
                if(errorMessage != null) {
                    message.reply(errorMessage).queue();
                    return;
                }

                message.reply("Ваша команда успешно удалена из поиска игры!").queue();
                MTHD.getInstance().teamFindGameChannel.updateTeamsInGameSearchCountMessage();
                MTHD.getInstance().gameManager.teamGameManager.tryStartGame();
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        } else {
            message.reply("Ошибка! Неверная команда!").queue();
        }
    }

    /**
     * Добавляет команду в поиск игры
     * @param teamId Id команды
     * @param format Формат игры
     * @param captainId Id начавшего
     * @return Текст ошибки добавления команды
     */
    private String addTeamToTeamsInGameSearch(int teamId, String format, int captainId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO teams_in_game_search (team_id, format, captain_id, joined_at) SELECT ?, ?, ?, ? " +
                "WHERE NOT EXISTS (SELECT 1 FROM teams_in_game_search WHERE team_id = ?);", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, teamId);
            preparedStatement.setString(2, format);
            preparedStatement.setInt(3, captainId);
            preparedStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            preparedStatement.setInt(5, teamId);
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if(resultSet.next()) {
                // Вернуть значение, что команда успешно добавлена в поиск игры
                return null;
            } else {
                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE teams_in_game_search SET joined_at = ?, captain_id = ? WHERE team_id = ? AND format = ?");
                updateStatement.setTimestamp(1, Timestamp.from(Instant.now()));
                updateStatement.setInt(2, captainId);
                updateStatement.setInt(3, teamId);
                updateStatement.setString(4, format);
                updateStatement.executeUpdate();
                return "Ваше время захода в поиск игры обновлено!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при добавлении вашей команды в поиск игры! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Удаляет команду из поиска игры
     * @param teamId Id команды
     * @return Текст ошибки удаления команды
     */
    private String removeTeamFromTeamsInGameSearch(int teamId) {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "DELETE FROM teams_in_game_search WHERE team_id = ?", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, teamId);
            preparedStatement.executeUpdate();

            // Вернуть значение, что команда успешно удалена из поиска игры
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return "Критическая ошибка при удалении вашей команды из поиска игры! Свяжитесь с разработчиком бота!";
        }
    }

    /**
     * Получает список команд в активных играх
     * @return Список команд в активных играх
     */
    private List<Integer> getTeamsInLiveGames() {
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT first_team_id, second_team_id FROM team_live_games;");
            ResultSet resultSet = preparedStatement.executeQuery();
            List<Integer> teamsInLiveGames = new ArrayList<>();
            while(resultSet.next()) {
                teamsInLiveGames.add(resultSet.getInt("first_team_id"));
                teamsInLiveGames.add(resultSet.getInt("second_team_id"));
            }
            return teamsInLiveGames;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
