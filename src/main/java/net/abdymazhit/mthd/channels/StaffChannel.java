package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.EnumSet;
import java.util.List;

/**
 * Канал помощников
 *
 * @version   21.10.2021
 * @author    Islam Abdymazhit
 */
public class StaffChannel extends Channel {

    /**
     * Инициализирует канал помощников
     */
    public StaffChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Staff", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Staff не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals("staff")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("staff").setPosition(1)
                .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    channel = textChannel;
                    sendChannelMessage();
                });
    }

    /**
     * Отправляет сообщение о доступных командах для помощников
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Доступные команды");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
                Стать готовым для проведения игры
                `!ready`

                Стать недоступным для проведения игры
                `!unready`""");
        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
        embedBuilder.clear();
    }
}