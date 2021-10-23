package net.abdymazhit.dangerbot.customs.serialization;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Победитель
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class Winner {

    @SerializedName("team")
    @Expose
    private String team;

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}