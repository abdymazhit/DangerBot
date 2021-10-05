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
 * @version   05.10.2021
 * @author    Islam Abdymazhit
 */
public class Team {

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
    public List<UserAccount> members;

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
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT name, leader_id, points, games, wins, won_beds, lost_beds FROM teams WHERE id = ? AND is_deleted is null;");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

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
            PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement(
                "SELECT discord_id, username FROM users WHERE id = ?;");
            preparedStatement.setInt(1, user.id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                user.discordId = resultSet.getString("discord_id");
                user.username = resultSet.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает id игроков VimeWorld
     */
    private void getUsersVimeIds() {
        StringBuilder names = new StringBuilder(leader.username);

        for(UserAccount member : members) {
            names.append(",").append(member.username);
        }

        String info = MTHD.getInstance().utils.sendGetRequest("https://api.vimeworld.ru/user/name/" + names +
            "?token=" + MTHD.getInstance().config.vimeApiToken);
        if(info == null) return;

        JsonArray infoArray = JsonParser.parseString(info).getAsJsonArray();
        for(JsonElement infoElement : infoArray) {
            JsonObject infoObject = infoElement.getAsJsonObject();

            int vimeId = infoObject.get("id").getAsInt();
            String username = infoObject.get("username").getAsString();

            if(username.equals(leader.username)) {
                leader.vimeId = vimeId;
            } else {
                for(UserAccount user : members) {
                    if(username.equals(user.username)) {
                        user.vimeId = vimeId;
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
            jsonArray.add(user.vimeId);
        }
        jsonArray.add(leader.vimeId);

        String info = MTHD.getInstance().utils.sendPostRequest("https://api.vimeworld.ru/user/session" + "?token="
            + MTHD.getInstance().config.vimeApiToken, jsonArray);
        if(info == null) return;

        JsonArray infoArray = JsonParser.parseString(info).getAsJsonArray();
        for(JsonElement infoElement : infoArray) {
            JsonObject infoObject = infoElement.getAsJsonObject();
            String username = infoObject.get("username").getAsString();
            boolean isOnline = infoObject.get("online").getAsJsonObject().get("value").getAsBoolean();

            if(username.equals(leader.username)) {
                leader.isVimeOnline = isOnline;
            } else {
                for(UserAccount user : members) {
                    if(username.equals(user.username)) {
                        user.isVimeOnline = isOnline;
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
            if(leader.discordId != null) {
                Member leaderMember = MTHD.getInstance().guild.retrieveMemberById(leader.discordId).submit().get();
                leader.isDiscordOnline = leaderMember.getOnlineStatus().equals(OnlineStatus.ONLINE) ||
                    leaderMember.getOnlineStatus().equals(OnlineStatus.IDLE) ||
                    leaderMember.getOnlineStatus().equals(OnlineStatus.DO_NOT_DISTURB);
            }

            for(UserAccount user : members) {
                if(user.discordId != null) {
                    Member member = MTHD.getInstance().guild.retrieveMemberById(user.discordId).submit().get();
                    user.isDiscordOnline = member.getOnlineStatus().equals(OnlineStatus.ONLINE) ||
                        member.getOnlineStatus().equals(OnlineStatus.IDLE) ||
                        member.getOnlineStatus().equals(OnlineStatus.DO_NOT_DISTURB);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
