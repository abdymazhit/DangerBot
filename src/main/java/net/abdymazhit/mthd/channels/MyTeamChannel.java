package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Канал моя команда
 *
 * @version   09.09.2021
 * @author    Islam Abdymazhit
 */
public class MyTeamChannel extends Channel {

    /**
     * Инициализирует канал моя команда
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
     * Отправляет сообщение канала моя команда
     */
    private void sendChannelMessage() {
        try {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Доступные команды");
            embedBuilder.setColor(0xFF58B9FF);
            embedBuilder.addField("Покинуть команду",
                    "`!team leave`", false);
            embedBuilder.addField("Исключить участника из команды",
                    "`!team kick <NAME>`", false);
            embedBuilder.addField("Передать права лидера команды",
                    "`!team transfer <NAME>`", false);
            embedBuilder.addField("Удалить команду",
                    "`!team disband`", false);
            embedBuilder.addField("Посмотреть информацию о команде",
                    "`!team info`", false);
            channelMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
