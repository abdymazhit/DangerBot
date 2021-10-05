package net.abdymazhit.mthd.channels.single;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Канал поиска игры игроков
 *
 * @version   05.10.2021
 * @author    Islam Abdymazhit
 */
public class SingleFindGameChannel extends Channel {

    /** Id сообщения о доступных помощниках */
    public String channelAvailableAssistantsMessageId;

    /**
     * Инициализирует канал поиска игры
     */
    public SingleFindGameChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Single Rating", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Single Rating не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals("find-game")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("find-game").setPosition(2)
                .setSlowmode(5)
                .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.SINGLE_RATING.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    channelId = textChannel.getId();
                    updatePlayersInGameSearchCountMessage();
                    updateAvailableAssistantsMessage();
                });
    }

    /**
     * Обновляет количество игроков в поиске игры
     */
    public void updatePlayersInGameSearchCountMessage() {
        TextChannel textChannel = MTHD.getInstance().guild.getTextChannelById(channelId);
        if(textChannel == null) {
            System.out.println("Критическая ошибка! Канал find-game не существует!");
            return;
        }

        removeInactivePlayers("4x2");
        removeInactivePlayers("6x2");

        int count4x2 = getPlayersCountInGameSearchByFormat("4x2");
        int count6x2 = getPlayersCountInGameSearchByFormat("6x2");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Поиск игры");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
            Доступные форматы игры: 4x2 , 6x2
            
            Обратите внимание, если в течении 20 минут не нашлась игра, Вы будете удалены из поиска! Вам придется заново зайти в поиск игры.

            Игроков в поиске игры 4x2: `%players4x2Count%`
            Игроков в поиске игры 6x2: `%players6x2Count%`

            Войти в поиск игры
            `!find game <FORMAT>`

            Выйти из поиска игры
            `!find leave`"""
                .replace("%players4x2Count%", String.valueOf(count4x2))
                .replace("%players6x2Count%", String.valueOf(count6x2)));
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

    private void removeInactivePlayers(String format) {
        try {
            PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement(
                    "SELECT player_id, joined_at FROM players_in_game_search WHERE format = ?;");
            preparedStatement.setString(1, format);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                int playerId = resultSet.getInt("player_id");

                Timestamp timestamp = resultSet.getTimestamp("joined_at");
                Timestamp currentTimestamp = Timestamp.from(Instant.now());

                if(currentTimestamp.getTime() - timestamp.getTime() >= 1200000) {
                    PreparedStatement statement = MTHD.getInstance().database.getConnection().prepareStatement(
                            "DELETE FROM players_in_game_search WHERE player_id = ?;");
                    statement.setInt(1, playerId);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает количество игроков в поиске игры по формату
     * @param format Формат игры
     * @return Количество игроков в поиске игры по формату
     */
    public int getPlayersCountInGameSearchByFormat(String format) {
        int count = 0;
        try {
            PreparedStatement preparedStatement = MTHD.getInstance().database.getConnection().prepareStatement(
                    "SELECT COUNT(*) as count FROM players_in_game_search WHERE format = ?;");
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