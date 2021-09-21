package net.abdymazhit.mthd.customs.serialization;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Игрок
 *
 * @version   21.09.2021
 * @author    Islam Abdymazhit
 */
public class Player {

    @SerializedName("id")
    @Expose
    private Integer id;

    @SerializedName("aliveTime")
    @Expose
    private Integer aliveTime;

    @SerializedName("kills")
    @Expose
    private Integer kills;

    @SerializedName("brokenBeds")
    @Expose
    private Integer brokenBeds;

    @SerializedName("spentGold")
    @Expose
    private Integer spentGold;

    @SerializedName("spentBronze")
    @Expose
    private Integer spentBronze;

    @SerializedName("spentIron")
    @Expose
    private Integer spentIron;

    @SerializedName("dead")
    @Expose
    private Boolean dead;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAliveTime() {
        return aliveTime;
    }

    public void setAliveTime(Integer aliveTime) {
        this.aliveTime = aliveTime;
    }

    public Integer getKills() {
        return kills;
    }

    public void setKills(Integer kills) {
        this.kills = kills;
    }

    public Integer getBrokenBeds() {
        return brokenBeds;
    }

    public void setBrokenBeds(Integer brokenBeds) {
        this.brokenBeds = brokenBeds;
    }

    public Integer getSpentGold() {
        return spentGold;
    }

    public void setSpentGold(Integer spentGold) {
        this.spentGold = spentGold;
    }

    public Integer getSpentBronze() {
        return spentBronze;
    }

    public void setSpentBronze(Integer spentBronze) {
        this.spentBronze = spentBronze;
    }

    public Integer getSpentIron() {
        return spentIron;
    }

    public void setSpentIron(Integer spentIron) {
        this.spentIron = spentIron;
    }

    public Boolean getDead() {
        return dead;
    }

    public void setDead(Boolean dead) {
        this.dead = dead;
    }
}