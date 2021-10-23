package net.abdymazhit.dangerbot.customs.serialization;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Карта
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class Map {

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("teams")
    @Expose
    private Integer teams;

    @SerializedName("playersInTeam")
    @Expose
    private Integer playersInTeam;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTeams() {
        return teams;
    }

    public void setTeams(Integer teams) {
        this.teams = teams;
    }

    public Integer getPlayersInTeam() {
        return playersInTeam;
    }

    public void setPlayersInTeam(Integer playersInTeam) {
        this.playersInTeam = playersInTeam;
    }
}