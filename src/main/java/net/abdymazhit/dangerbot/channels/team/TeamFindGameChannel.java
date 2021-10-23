package net.abdymazhit.dangerbot.channels.team;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.Channel;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Канал поиска игры команд
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class TeamFindGameChannel extends Channel {

    /** Информационное сообщение о доступных помощниках */
    public Message channelAvailableAssistantsMessage;

    /**
     * Инициализирует канал поиска игры
     */
    public TeamFindGameChannel() {
        List<Category> categories = DangerBot.getInstance().guild.getCategoriesByName("Team Rating", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Team Rating не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals("find-game")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("find-game").setPosition(2).setSlowmode(15)
                .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.LEADER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.MEMBER.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(DangerBot.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    channel = textChannel;
                    updateTeamsInGameSearchCountMessage();
                    updateAvailableAssistantsMessage();
                });
    }

    /**
     * Обновляет количество команд в поиске игры
     */
    public void updateTeamsInGameSearchCountMessage() {
        removeInactiveTeams("4x2");
        removeInactiveTeams("6x2");

        int count4x2 = getTeamsCountInGameSearchByFormat("4x2");
        int count6x2 = getTeamsCountInGameSearchByFormat("6x2");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Поиск игры");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
                Доступные форматы игры: 4x2 , 6x2
            
                Обратите внимание, если в течении 20 минут не нашлась игра, Вы будете удалены из поиска! Вам придется заново зайти в поиск игры.

                Команд в поиске игры 4x2: `%teams4x2Count%`
                Команд в поиске игры 6x2: `%teams6x2Count%`

                Войти в поиск игры
                `!find game <FORMAT>`

                Выйти из поиска игры
                `!find leave`"""
                .replace("%teams4x2Count%", String.valueOf(count4x2))
                .replace("%teams6x2Count%", String.valueOf(count6x2)));
        if(channelMessage == null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
        } else {
            channel.editMessageEmbedsById(channelMessage.getId(), embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Удаляет неактивные команды из поиска
     * @param format Формат игры
     */
    private void removeInactiveTeams(String format) {
        try {
            PreparedStatement preparedStatement = DangerBot.getInstance().database.getConnection().prepareStatement(
                    "SELECT team_id, joined_at FROM teams_in_game_search WHERE format = ?;");
            preparedStatement.setString(1, format);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                int teamId = resultSet.getInt("team_id");

                Timestamp timestamp = resultSet.getTimestamp("joined_at");
                Timestamp currentTimestamp = Timestamp.from(Instant.now());

                if(currentTimestamp.getTime() - timestamp.getTime() >= 1200000) {
                    PreparedStatement statement = DangerBot.getInstance().database.getConnection().prepareStatement(
                            "DELETE FROM teams_in_game_search WHERE team_id = ?;");
                    statement.setInt(1, teamId);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Обновляет список доступных помощников
     */
    public void updateAvailableAssistantsMessage() {
        List<String> assistants = DangerBot.getInstance().database.getAvailableAssistants();

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
        if(channelAvailableAssistantsMessage == null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelAvailableAssistantsMessage = message);
        } else {
            channel.editMessageEmbedsById(channelAvailableAssistantsMessage.getId(), embedBuilder.build()).queue();
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
            PreparedStatement preparedStatement = DangerBot.getInstance().database.getConnection().prepareStatement(
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