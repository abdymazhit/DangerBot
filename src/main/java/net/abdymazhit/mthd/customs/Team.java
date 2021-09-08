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
 * @version   08.09.2021
 * @author    Islam Abdymazhit
 */
public class Team {

    /** Подключение к базе данных */
    private final Connection connection;

    /** Id команды */
    private final int id;

    /** Название команды */
    private String name;

    /** Лидер команды */
    private UserAccount leader;

    /** Количество очков команды */
    private int points;

    /** Количество сыгранных игр команды */
    private int games;

    /** Количество побед команды */
    private int wins;

    /** Количество убийств команды */
    private int kills;

    /** Количество смертей команды */
    private int deaths;

    /** Количество выигранных кроватей команды */
    private int won_beds;

    /** Количество потерянных кроватей команды */
    private int lost_beds;

    /** Участники команды */
    private final List<UserAccount> members;

    /**
     * Инициализирует команду
     * @param id Id команды
     */
    public Team(int id) {
        this.id = id;
        connection = MTHD.getInstance().database.getConnection();
        members = new ArrayList<>();

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
     * Получает название команды
     * @return Название команды
     */
    public String getName() {
        return name;
    }

    /**
     * Получает лидера команды
     * @return Лидер команды
     */
    public UserAccount getLeader() {
        return leader;
    }

    /**
     * Получает количество очков команды
     * @return Количество очков команды
     */
    public int getPoints() {
        return points;
    }

    /**
     * Получает количество сыгранных игр команды
     * @return Количество сыгранных игр команды
     */
    public int getGames() {
        return games;
    }

    /**
     * Получает количество побед команды
     * @return Количество побед команды
     */
    public int getWins() {
        return wins;
    }

    /**
     * Получает количество убийств команды
     * @return Количество убийств команды
     */
    public int getKills() {
        return kills;
    }

    /**
     * Получает количество смертей команды
     * @return Количество смертей команды
     */
    public int getDeaths() {
        return deaths;
    }

    /**
     * Получает количество выигранных кроватей команды
     * @return Количество выигранных кроватей команды
     */
    public int getWon_beds() {
        return won_beds;
    }

    /**
     * Получает количество потерянных кроватей команды
     * @return Количество потерянных кроватей команды
     */
    public int getLost_beds() {
        return lost_beds;
    }

    /**
     * Получает участников команды
     * @return Участники команды
     */
    public List<UserAccount> getMembers() {
        return members;
    }

    /**
     * Получает информацию о команде
     */
    private void getTeamInfo() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT name, leader_id, points, games, wins, kills, deaths, won_beds, lost_beds FROM teams WHERE id = ?;");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                name = resultSet.getString("name");
                leader = new UserAccount(resultSet.getInt("leader_id"));
                points = resultSet.getInt("points");
                games = resultSet.getInt("games");
                wins = resultSet.getInt("wins");
                kills = resultSet.getInt("kills");
                deaths = resultSet.getInt("deaths");
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
                    "SELECT member_id, username FROM users WHERE id = ?;");
            preparedStatement.setInt(1, user.getId());
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if(resultSet.next()) {
                user.setDiscordId(resultSet.getString("member_id"));
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
            if(leaderMember == null) return;
            if(leaderMember.getOnlineStatus().equals(OnlineStatus.ONLINE) ||
                    leaderMember.getOnlineStatus().equals(OnlineStatus.IDLE) ||
                    leaderMember.getOnlineStatus().equals(OnlineStatus.DO_NOT_DISTURB)) {
                leader.setDiscordOnline(true);
            } else {
                leader.setDiscordOnline(false);
            }

            for(UserAccount user : members) {
                Member userMember = MTHD.getInstance().guild.retrieveMemberById(user.getDiscordId()).submit().get();
                if(userMember == null) return;

                if(userMember.getOnlineStatus().equals(OnlineStatus.ONLINE) ||
                        userMember.getOnlineStatus().equals(OnlineStatus.IDLE) ||
                        userMember.getOnlineStatus().equals(OnlineStatus.DO_NOT_DISTURB)) {
                    user.setDiscordOnline(true);
                } else {
                    user.setDiscordOnline(false);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
