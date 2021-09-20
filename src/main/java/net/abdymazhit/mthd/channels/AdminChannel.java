package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.EnumSet;
import java.util.List;

/**
 * Канал администрации
 *
 * @version   20.09.2021
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

            for(GuildChannel channel : category.getChannels()) {
                if(channel.getName().equals("admin")) {
                    channel.delete().queue();
                }
            }

            ChannelAction<TextChannel> createAction = category.createTextChannel("admin").setPosition(0);
            createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));

            createAction.queue(textChannel -> {
                channelId = textChannel.getId();

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Команды администратора");
                embedBuilder.setColor(3092790);

                embedBuilder.setDescription(
                        "Создание команды\n" +
                        "`!adminteam create <TEAM_NAME> <LEADER_NAME>`\n" +
                        "\n" +
                        "Удаление команды\n" +
                        "`!adminteam disband <TEAM_NAME>`\n" +
                        "\n" +
                        "Добавление участника в команду\n" +
                        "`!adminteam add <TEAM_NAME> <MEMBER_NAME>`\n" +
                        "\n" +
                        "Удаление участника из команды\n" +
                        "`!adminteam delete <TEAM_NAME> <MEMBER_NAME>`\n" +
                        "\n" +
                        "Передача прав лидера команды\n" +
                        "`!adminteam transfer <TEAM_NAME> <FROM_NAME> <TO_NAME>`\n" +
                        "\n" +
                        "Переименование команды\n" +
                         "`!adminteam rename <TEAM_NAME> <TO_NAME>`\n"
                );
                textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
                embedBuilder.clear();
            });
        }
    }
}
