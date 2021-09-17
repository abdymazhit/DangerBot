package net.abdymazhit.mthd.customs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.abdymazhit.mthd.MTHD;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Представляет собой команду
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public class Team {

    /** Подключение к базе данных */
    private Connection connection;

    /** Id команды */
    public final int id;

    /** Название команды */
    public String name;

    /** Лидер команды */
    public UserAccount leader;

    /** Количество очков команды */
    public int points;

    /** Количество сыгранных игр команды */
    public int games;

    /** Количество побед команды */
    public int wins;

    /** Количество выигранных кроватей команды */
    public int won_beds;

    /** Количество потерянных кроватей команды */
    public int lost_beds;

    /** Участники команды */
    public final List<UserAccount> members;

    /**
     * Инициализирует команду
     * @param id Id команды
     */
    public Team(int id) {
        this.id = id;
        members = new ArrayList<>();
    }

    /**
     * Получить информацию о команде из базы данных
     */
    public void getTeamInfoByDatabase() {
        connection = MTHD.getInstance().database.getConnection();
        getTeamInfo();
        for(UserAccount user : members) {
            getUsersInfo(user);
        }
        getUsersInfo(leader);
        getUsersVimeIds();
        getUsersVimeOnline();
        getUsersDiscordOnline();
    }

    /**
     * Получает информацию о команде
     */
    private void getTeamInfo() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT name, leader_id, points, games, wins, won_beds, lost_beds FROM teams WHERE id = ?;");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                name = resultSet.getString("name");
                leader = new UserAccount(resultSet.getInt("leader_id"));
                points = resultSet.getInt("points");
                games = resultSet.getInt("games");
                wins = resultSet.getInt("wins");
                won_beds = resultSet.getInt("won_beds");
                lost_beds = resultSet.getInt("lost_beds");

                PreparedStatement membersStatement = connection.prepareStatement(
                        "SELECT member_id FROM teams_members WHERE team_id = ?;");
                membersStatement.setInt(1, id);
                ResultSet membersResultSet = membersStatement.executeQuery();
                membersStatement.close();

                while(membersResultSet.next()) {
                    members.add(new UserAccount(membersResultSet.getInt("member_id")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает информацию о игроках команды
     * @param user Игрок
     */
    public void getUsersInfo(UserAccount user) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT discord_id, username FROM users WHERE id = ?;");
            preparedStatement.setInt(1, user.getId());
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                user.setDiscordId(resultSet.getString("discord_id"));
                user.setUsername(resultSet.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает id игроков VimeWorld
     */
    private void getUsersVimeIds() {
        StringBuilder names = new StringBuilder(leader.getUsername());
        for(UserAccount user : members) {
            names.append(",").append(user.getUsername());
        }

        String info = MTHD.getInstance().utils.sendGetRequest("https://api.vimeworld.ru/user/name/" + names);
        if(info == null) return;

        JsonArray infoArray = JsonParser.parseString(info).getAsJsonArray();
        for(JsonElement infoElement : infoArray) {
            JsonObject infoObject = infoElement.getAsJsonObject();

            int vimeId = infoObject.get("id").getAsInt();
            String username = infoObject.get("username").getAsString();

            if(username.equals(leader.getUsername())) {
                leader.setVimeId(vimeId);
            } else {
                for(UserAccount user : members) {
                    if(username.equals(user.getUsername())) {
                        user.setVimeId(vimeId);
                    }
                }
            }
        }
    }

    /**
     * Получает статус онлайна игроков в VimeWorld
     */
    private void getUsersVimeOnline() {
        JsonArray jsonArray = new JsonArray();
        for(UserAccount user : members) {
            jsonArray.add(user.getVimeId());
        }
        jsonArray.add(leader.getVimeId());

        String info = MTHD.getInstance().utils.sendPostRequest("https://api.vimeworld.ru/user/session", jsonArray);
        if(info == null) return;

        JsonArray infoArray = JsonParser.parseString(info).getAsJsonArray();
        for(JsonElement infoElement : infoArray) {
            JsonObject infoObject = infoElement.getAsJsonObject();
            String username = infoObject.get("username").getAsString();
            boolean isOnline = infoObject.get("online").getAsJsonObject().get("value").getAsBoolean();

            if(username.equals(leader.getUsername())) {
                leader.setVimeOnline(isOnline);
            } else {
                for(UserAccount user : members) {
                    if(username.equals(user.getUsername())) {
                        user.setVimeOnline(isOnline);
                    }
                }
            }
        }
    }

    /**
     * Получает статус онлайна игроков в Discord
     */
    private void getUsersDiscordOnline() {
        try {
            Member leaderMember = MTHD.getInstance().guild.retrieveMemberById(leader.getDiscordId()).submit().get();

            leader.setDiscordOnline(leaderMember.getOnlineStatus().equals(OnlineStatus.ONLINE) ||
                    leaderMember.getOnlineStatus().equals(OnlineStatus.IDLE) ||
                    leaderMember.getOnlineStatus().equals(OnlineStatus.DO_NOT_DISTURB));

            for(UserAccount user : members) {
                MTHD.getInstance().guild.retrieveMemberById(user.getDiscordId()).queue(member1 ->
                        user.setDiscordOnline(member1.getOnlineStatus().equals(OnlineStatus.ONLINE) ||
                                member1.getOnlineStatus().equals(OnlineStatus.IDLE) ||
                                member1.getOnlineStatus().equals(OnlineStatus.DO_NOT_DISTURB)));
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
