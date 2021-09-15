package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.EnumSet;
import java.util.List;

/**
 * Канал администрации
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminChannel extends Channel {

    /**
     * Инициализирует канал администрации
     */
    public AdminChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Staff", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);
            deleteChannel(category, "admin");

            ChannelAction<TextChannel> createAction = createChannel(category.getId(), "admin", 0);
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            createAction.queue(textChannel -> channelId = textChannel.getId());

            sendChannelMessage();
        }
    }

    /**
     * Отправляет сообщение канала администрации
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Команды администратора");
        embedBuilder.setColor(0xFF58B9FF);
        embedBuilder.addField("Создание команды",
                "`!adminteam create <TEAM_NAME> <LEADER_NAME>`", false);
        embedBuilder.addField("Удаление команды",
                "`!adminteam disband <TEAM_NAME>`", false);
        embedBuilder.addField("Добавление участника в команду",
                "`!adminteam add <TEAM_NAME> <MEMBER_NAME>`", false);
        embedBuilder.addField("Удаление участника из команды",
                "`!adminteam delete <TEAM_NAME> <MEMBER_NAME>`", false);
        embedBuilder.addField("Передача прав лидера команды",
                "`!adminteam transfer <TEAM_NAME> <FROM_NAME> <TO_NAME>`", false);
        embedBuilder.addField("Переименование команды",
                "`!adminteam rename <TEAM_NAME> <TO_NAME>`", false);
        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(channel != null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
        }
        embedBuilder.clear();
    }
}
