package net.abdymazhit.mthd.customs.serialization;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Команда
 *
 * @version   21.09.2021
 * @author    Islam Abdymazhit
 */
public class Team {

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("members")
    @Expose
    private List<Integer> members = null;

    @SerializedName("bedAlive")
    @Expose
    private Boolean bedAlive;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Integer> getMembers() {
        return members;
    }

    public void setMembers(List<Integer> members) {
        this.members = members;
    }

    public Boolean getBedAlive() {
        return bedAlive;
    }

    public void setBedAlive(Boolean bedAlive) {
        this.bedAlive = bedAlive;
    }
}