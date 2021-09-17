package net.abdymazhit.mthd.customs.match;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Событие игры
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public class Event {

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("time")
    @Expose
    private Integer time;

    @SerializedName("killer")
    @Expose
    private Integer killer;

    @SerializedName("target")
    @Expose
    private Integer target;

    @SerializedName("killerHealth")
    @Expose
    private String killerHealth;

    @SerializedName("player")
    @Expose
    private Integer player;

    @SerializedName("team")
    @Expose
    private String team;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Integer getKiller() {
        return killer;
    }

    public void setKiller(Integer killer) {
        this.killer = killer;
    }

    public Integer getTarget() {
        return target;
    }

    public void setTarget(Integer target) {
        this.target = target;
    }

    public String getKillerHealth() {
        return killerHealth;
    }

    public void setKillerHealth(String killerHealth) {
        this.killerHealth = killerHealth;
    }

    public Integer getPlayer() {
        return player;
    }

    public void setPlayer(Integer player) {
        this.player = player;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}