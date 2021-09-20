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
 * Канал моя команда
 *
 * @version   20.09.2021
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

            for(GuildChannel channel : category.getChannels()) {
                if(channel.getName().equals("my-team")) {
                    channel.delete().queue();
                }
            }

            ChannelAction<TextChannel> createAction = category.createTextChannel("my-team").setPosition(3);
            createAction = createAction.addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(UserRole.LEADER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(UserRole.MEMBER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null);
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            createAction.queue(textChannel -> {
                channelId = textChannel.getId();

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Доступные команды");
                embedBuilder.setColor(3092790);

                embedBuilder.setDescription(
                        "Исключить участника из команды\n" +
                        "`!team kick <NAME>`\n" +
                        "\n" +
                        "Передать права лидера команды\n" +
                        "`!team transfer <NAME>`\n" +
                        "\n" +
                        "Удалить команду\n" +
                        "`!team disband`\n" +
                        "\n" +
                        "Посмотреть информацию о команде\n" +
                        "`!team info`\n" +
                        "\n" +
                        "Покинуть команду\n" +
                        "`!team leave`"
                );

                textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                embedBuilder.clear();
            });
        }
    }
}
