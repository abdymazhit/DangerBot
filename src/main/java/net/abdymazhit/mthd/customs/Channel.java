package net.abdymazhit.mthd.customs;

import net.abdymazhit.mthd.MTHD;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Представляет собой канал
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class Channel {

    /** Id канала */
    public String channelId;

    /** Id сообщения канала */
    public String channelMessageId;

    /**
     * Удаляет канал
     * @param category Категория
     * @param channelName Название канала
     */
    public void deleteChannel(Category category, String channelName) {
        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals(channelName)) {
                textChannel.delete().queue();
                return;
            }
        }
    }

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
     * @param categoryId Id категория
     * @param channelName Название канала
     * @param position Позиция канала
     */
    public ChannelAction<TextChannel> createChannel(String categoryId, String channelName, @Nullable Integer position) {
        Category category = MTHD.getInstance().guild.getCategoryById(categoryId);
        if(category != null) {
            return category.createTextChannel(channelName).setPosition(position);
        }
        return null;
    }

    /**
     * Создает канал
     * @param channelName Название канала
     * @param position Позиция канала
     */
    public void createChannel(String channelName, @Nullable Integer position) {
        MTHD.getInstance().guild.createTextChannel(channelName).setPosition(position).queue(textChannel -> channelId = textChannel.getId());
    }
}