package net.abdymazhit.mthd.customs;

import net.abdymazhit.mthd.MTHD;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Представляет собой канал
 *
 * @version   05.09.2021
 * @author    Islam Abdymazhit
 */
public class Channel {

    /** Канал */
    public TextChannel channel;

    /**
     * Удаляет канал
     * @param channelName Название канала
     */
    public void deleteChannel(String channelName) {
        List<TextChannel> textChannels = MTHD.getInstance().guild.getTextChannelsByName(channelName, true);
        if(!textChannels.isEmpty()) {
            textChannels.get(0).delete().queue();
        }
    }

    /**
     * Создает канал
     * @param channelName Название канала
     * @param position Позиция канала
     */
    public void createChannel(String channelName, @Nullable Integer position) {
        try {
            channel = MTHD.getInstance().guild.createTextChannel(channelName).setPosition(position).submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}