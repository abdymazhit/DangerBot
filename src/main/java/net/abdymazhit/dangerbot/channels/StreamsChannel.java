package net.abdymazhit.dangerbot.channels;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Channel;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.EnumSet;
import java.util.List;

/**
 * Канал трансляции
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class StreamsChannel extends Channel {

    /**
     * Инициализирует канал трансляции
     */
    public StreamsChannel() {
        List<Category> categories = DangerBot.getInstance().guild.getCategoriesByName("Staff", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Staff не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals("streams")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("streams").setPosition(2)
                .addPermissionOverride(UserRole.YOUTUBE.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(DangerBot.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    channel = textChannel;
                    sendChannelMessage();
                });
    }

    /**
     * Отправляет сообщение о доступных командах для ютуберов
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Доступные команды");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
                Обратите внимание! В названии трансляции обязательно должно присутствовать название `DangerZone` или `Danger Zone`!
                
                Добавить трансляцию
                `!stream add <LINK>`""");
        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
        embedBuilder.clear();
    }
}