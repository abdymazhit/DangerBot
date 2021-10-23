package net.abdymazhit.dangerbot.channels.team;

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
 * Канал моя команда
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class MyTeamChannel extends Channel {

    /**
     * Инициализирует канал моя команда
     */
    public MyTeamChannel() {
        List<Category> categories = DangerBot.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Team Rating не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals("my-team")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("my-team").setPosition(3).setSlowmode(15)
                .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.LEADER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.MEMBER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(DangerBot.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    channel = textChannel;
                    sendChannelMessage();
                });
    }

    /**
     * Отправляет сообщение о доступных командах
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Доступные команды");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
                Исключить участника из команды
                `!team kick <NAME>`

                Передать права лидера команды
                `!team transfer <NAME>`

                Удалить команду
                `!team disband`

                Посмотреть информацию о команде
                `!team info`

                Покинуть команду
                `!team leave`""");
        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
        embedBuilder.clear();
    }
}
