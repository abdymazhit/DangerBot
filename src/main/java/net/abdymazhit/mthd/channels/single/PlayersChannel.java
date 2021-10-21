package net.abdymazhit.mthd.channels.single;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.abdymazhit.mthd.customs.info.PlayerInfo;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Канал игроков
 *
 * @version   21.10.2021
 * @author    Islam Abdymazhit
 */
public class PlayersChannel extends Channel {

    /** Информационное сообщение о лучших игроках */
    public Message channelTopPlayersMessage;

    /**
     * Инициализирует канал игроков
     */
    public PlayersChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Single Rating", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Single Rating не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equals("players")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("players").setPosition(0).setSlowmode(30)
                .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(UserRole.AUTHORIZED.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL)).queue(textChannel -> {
            channel = textChannel;
            updateTopMessage();
            sendChannelMessage();
        });
    }

    /**
     * Отправляет сообщение о доступных командах для авторизованных пользователей
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Доступные команды");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
                Посмотреть информацию о игроке
                `!info <NAME>`
            
                Посмотреть информацию о себе
                `!info`""");
        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
        embedBuilder.clear();
    }

    /**
     * Обновляет информационное сообщение о лучших игроках
     */
    public void updateTopMessage() {
        List<PlayerInfo> players = getTopPlayers();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        String title = "Топ 20 игроков";
        embedBuilder.setTitle(title);
        embedBuilder.setColor(3092790);

        StringBuilder playersPlaceString = new StringBuilder();
        for(int i = 1; i <= players.size(); i++) {
            playersPlaceString.append("> ").append(i).append("\n");
        }
        embedBuilder.addField("Place", playersPlaceString.toString(), true);

        StringBuilder playersNamesString = new StringBuilder();
        for(PlayerInfo playerInfo : players) {
            playersNamesString.append(playerInfo.username.replace("_", "\\_")).append("\n");
        }
        embedBuilder.addField("Name", playersNamesString.toString(), true);

        StringBuilder playersPointsString = new StringBuilder();
        for(PlayerInfo playerInfo : players) {
            playersPointsString.append(playerInfo.points).append("\n");
        }
        embedBuilder.addField("Points", playersPointsString.toString(), true);

        if(channelTopPlayersMessage == null) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelTopPlayersMessage = message);
        } else {
            channel.editMessageEmbedsById(channelTopPlayersMessage.getId(), embedBuilder.build()).queue();
        }
        embedBuilder.clear();
    }

    /**
     * Получает лучших игроков
     * @return Лучшие игроки
     */
    public List<PlayerInfo> getTopPlayers() {
        List<PlayerInfo> players = new ArrayList<>();
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("""
                WITH PLAYERS AS(SELECT *, RANK() OVER(ORDER BY points DESC) RATING FROM players)
                SELECT player_id, points FROM PLAYERS WHERE RATING <= 20 AND is_deleted IS NULL;""");
            while(resultSet.next()) {
                int id = resultSet.getInt("player_id");
                int points = resultSet.getInt("points");

                PreparedStatement preparedStatement = connection.prepareStatement("SELECT username FROM users WHERE id = ?;");
                preparedStatement.setInt(1, id);
                ResultSet usernameResultSet = preparedStatement.executeQuery();
                if(usernameResultSet.next()) {{
                    String username = usernameResultSet.getString("username");
                    PlayerInfo playerInfo = new PlayerInfo(id, username, points);
                    players.add(playerInfo);
                }}
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }
}
