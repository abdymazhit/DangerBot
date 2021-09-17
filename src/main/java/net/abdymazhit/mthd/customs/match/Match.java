package net.abdymazhit.mthd.customs.match;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Матч
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public class Match {

    @SerializedName("version")
    @Expose
    private Integer version;

    @SerializedName("game")
    @Expose
    private String game;

    @SerializedName("server")
    @Expose
    private String server;

    @SerializedName("start")
    @Expose
    private Integer start;

    @SerializedName("winner")
    @Expose
    private Winner winner;

    @SerializedName("mapName")
    @Expose
    private String mapName;

    @SerializedName("mapId")
    @Expose
    private String mapId;

    @SerializedName("end")
    @Expose
    private Integer end;

    @SerializedName("players")
    @Expose
    private List<Player> players = null;

    @SerializedName("teams")
    @Expose
    private List<Team> teams = null;

    @SerializedName("events")
    @Expose
    private List<Event> events = null;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Winner getWinner() {
        return winner;
    }

    public void setWinner(Winner winner) {
        this.winner = winner;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }
}
