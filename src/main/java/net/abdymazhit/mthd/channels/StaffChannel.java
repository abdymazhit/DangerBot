package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.EnumSet;
import java.util.List;

/**
 * Канал персонала
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public class StaffChannel extends Channel {

    /**
     * Инициализирует канал персонала
     */
    public StaffChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Staff", true);
        if(!categories.isEmpty()) {
            Category category = categories.get(0);

            for(GuildChannel channel : category.getChannels()) {
                if(channel.getName().equals("staff")) {
                    channel.delete().queue();
                }
            }

            ChannelAction<TextChannel> createAction = category.createTextChannel("staff").setPosition(1);
            createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            createAction.queue(textChannel -> {
                channelId = textChannel.getId();

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Доступные команды");
                embedBuilder.setColor(0xFF58B9FF);
                embedBuilder.addField("Стать готовым для проведения игры",
                        "`!ready`", false);
                embedBuilder.addField("Стать недоступным для проведения игры",
                        "`!unready`", false);

                TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
                if(channel != null) {
                    channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                }

                embedBuilder.clear();
            });
        }
    }
}