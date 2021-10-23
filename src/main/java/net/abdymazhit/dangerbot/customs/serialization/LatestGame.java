package net.abdymazhit.dangerbot.customs.serialization;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Последняя игра
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class LatestGame {

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("game")
    @Expose
    private String game;

    @SerializedName("map")
    @Expose
    private Map map;

    @SerializedName("date")
    @Expose
    private Integer date;

    @SerializedName("duration")
    @Expose
    private Integer duration;

    @SerializedName("players")
    @Expose
    private Integer players;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public Integer getDate() {
        return date;
    }

    public void setDate(Integer date) {
        this.date = date;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getPlayers() {
        return players;
    }

    public void setPlayers(Integer players) {
        this.players = players;
    }
}