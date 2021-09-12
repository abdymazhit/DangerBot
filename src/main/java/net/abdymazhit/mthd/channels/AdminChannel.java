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
import java.util.concurrent.ExecutionException;

/**
 * Канал администрации
 *
 * @version   12.09.2021
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

            try {
                ChannelAction<TextChannel> createAction = createChannel(category, "admin", 0);
                createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
                channel = createAction.submit().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            sendChannelMessage();
        }
    }

    /**
     * Отправляет сообщение канала администрации
     */
    private void sendChannelMessage() {
        try {
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
            channelMessage = channel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            embedBuilder.clear();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
