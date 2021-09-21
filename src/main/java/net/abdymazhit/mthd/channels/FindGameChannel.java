package net.abdymazhit.mthd.channels;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Канал поиска игры
 *
 * @version   21.09.2021
 * @author    Islam Abdymazhit
 */
public class FindGameChannel extends Channel {

    /** Id сообщения о доступных помощниках */
    public String channelAvailableAssistantsMessageId;

    /**
     * Инициализирует канал поиска игры
     */
    public FindGameChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Team Rating не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals("find-game")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("find-game").setPosition(2)
                .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.LEADER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.MEMBER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
        .queue(textChannel -> {
            channelId = textChannel.getId();
            updateTeamsInGameSearchCountMessage();
            updateAvailableAssistantsMessage();
        });
    }

    /**
     * Обновляет количество команд в поиске игры
     */
    public void updateTeamsInGameSearchCountMessage() {
        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал find-game не существует!");
            return;
        }

        int count4x2 = getTeamsCountInGameSearchByFormat("4x2");
        int count6x2 = getTeamsCountInGameSearchByFormat("6x2");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Поиск игры");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
            Доступные форматы игры: 4x2 , 6x2

            Команд в поиске игры 4x2: `%teams4x2Count%`
            Команд в поиске игры 6x2: `%teams6x2Count%`

            Войти в поиск игры
            `!find game <FORMAT>`

            Выйти из поиска игры
            `!find leave`"""
                .replace("%teams4x2Count%", String.valueOf(count4x2))
                .replace("%teams6x2Count%", String.valueOf(count6x2)));
        if(channelMessageId == null) {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());
        } else {
            textChannel.editMessageEmbedsById(channelMessageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Обновляет список доступных помощников
     */
    public void updateAvailableAssistantsMessage() {
        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал find-game не существует!");
            return;
        }

        List<String> assistants = MTHD.getInstance().database.getAvailableAssistants();

        StringBuilder assistantsString = new StringBuilder();
        if(assistants.isEmpty()) {
            assistantsString.append("Сейчас нет доступных помощников");
        } else {
            for(String assistant : assistants) {
                assistantsString.append("> ").append(assistant).append("\n");
            }
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Доступные помощники");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription(assistantsString);
        if(channelAvailableAssistantsMessageId == null) {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelAvailableAssistantsMessageId = message.getId());
        } else {
            textChannel.editMessageEmbedsById(channelAvailableAssistantsMessageId, embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Получает количество команд в поиске игры по формату
     * @param format Формат игры
     * @return Количество команд в поиске игры по формату
     */
    public int getTeamsCountInGameSearchByFormat(String format) {
        int count = 0;
        try {
            PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement(
                "SELECT COUNT(*) as count FROM teams_in_game_search WHERE format = ?;");
            preparedStatement.setString(1, format);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                count = resultSet.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
}