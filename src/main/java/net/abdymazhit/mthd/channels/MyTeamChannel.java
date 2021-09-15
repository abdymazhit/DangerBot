package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.EnumSet;
import java.util.List;

/**
 * Канал моя команда
 *
 * @version   15.09.2021
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

            ChannelAction<TextChannel> createAction = createChannel(category.getId(), "my-team", 3);
            createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(UserRole.LEADER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(UserRole.MEMBER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            createAction.queue(textChannel -> channelId = textChannel.getId());

            sendChannelMessage();
        }
    }

    /**
     * Отправляет сообщение канала моя команда
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Доступные команды");
        embedBuilder.setColor(0xFF58B9FF);
        embedBuilder.addField("Исключить участника из команды",
                "`!team kick <NAME>`", false);
        embedBuilder.addField("Передать права лидера команды",
                "`!team transfer <NAME>`", false);
        embedBuilder.addField("Удалить команду",
                "`!team disband`", false);
        embedBuilder.addField("Посмотреть информацию о команде",
                "`!team info`", false);
        embedBuilder.addField("Покинуть команду",
                "`!team leave`", false);
        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(channel != null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
        }
        embedBuilder.clear();
    }
}
