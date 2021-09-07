package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;

import java.util.List;

/**
 * Канал моя команда
 *
 * @version   07.09.2021
 * @author    Islam Abdymazhit
 */
public class MyTeamChannel extends Channel {

    /**
     * Инициализирует канал администрации
     */
    public MyTeamChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            deleteChannel(category, "my-team");
            createChannel(category, "my-team", 0);
            sendChannelMessage();
        }
    }

    /**
     * Отправляет сообщение канала администрации
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Доступные команды");
        embedBuilder.setColor(0xFF58B9FF);
        embedBuilder.addField("Покинуть команду",
                "`!team leave`", false);
        embedBuilder.addField("Исключить участника из команды",
                "`!team kick <NAME>`", false);
        embedBuilder.addField("Передать права лидера команды",
                "`!team transfer <NAME>`", false);
        channel.sendMessageEmbeds(embedBuilder.build()).queue();
        embedBuilder.clear();
    }
}
