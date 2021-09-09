package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Канал команды
 *
 * @version   09.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamsChannel extends Channel {

    /**
     * Инициализирует канал команды
     */
    public TeamsChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            deleteChannel(category, "teams");
            createChannel(category, "teams", 0);
            sendChannelMessage();
        }
    }

    /**
     * Отправляет сообщение канала команды
     */
    private void sendChannelMessage() {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Доступные команды");
            embedBuilder.setColor(0xFF58B9FF);
            embedBuilder.addField("Посмотреть информацию о команде",
                    "`!team info <NAME>`", false);
            embedBuilder.addField("TOP 100 команд",
                    "`!team top`", false);
            channelMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
