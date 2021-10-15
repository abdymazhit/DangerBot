package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Assistant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.*;
import java.time.Instant;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Администраторская команда просмотра информации о помощниках
 *
 * @version   15.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminAssistantsInfoCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();

        if (event.getAuthor().isBot()) return;

        Map<Integer, Assistant> assistants = getAssistantsInfo();

        List<Assistant> assistantList = assistants.values().stream()
                .sorted(Comparator.comparing(Assistant::getTodayGames)).collect(Collectors.toList());

        List<Assistant> assistantListSorted = new ArrayList<>();
        for(int i = assistantList.size() - 1; i >= 0; i--) {
            assistantListSorted.add(assistantList.get(i));
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("""
            ```              Информация о помощниках              ```""");
        embedBuilder.setColor(3092790);

        StringBuilder namesString = new StringBuilder();
        for(Assistant assistant : assistantListSorted) {
            namesString.append(assistant.username.replace("_", "\\_")).append("\n");
        }
        embedBuilder.addField("Name", namesString.toString(), true);

        StringBuilder allGamesString = new StringBuilder();
        for(Assistant assistant : assistantListSorted) {
            allGamesString.append(assistant.games).append("-")
                    .append(assistant.weeklyGames).append("-")
                    .append(assistant.todayGames).append("\n");
        }
        embedBuilder.addField("All-Weekly-Today", allGamesString.toString(), true);

        StringBuilder lastGameTimestampString = new StringBuilder();
        for(Assistant assistant : assistantListSorted) {
            Timestamp lastGameTimestamp = assistant.lastGameTimestamp;

            int hours;
            if (lastGameTimestamp != null) {
                Timestamp timestamp = Timestamp.from(Instant.now());
                long milliseconds = timestamp.getTime() - lastGameTimestamp.getTime();
                hours = (int) (milliseconds / (60 * 60 * 1000));
            } else {
                hours = 0;
            }

            lastGameTimestampString.append(hours).append("h ago").append("\n");
        }
        embedBuilder.addField("Latest", lastGameTimestampString.toString(), true);

        message.replyEmbeds(embedBuilder.build()).queue();
        embedBuilder.clear();
    }

    /**
     * Получает информацию о помощниках
     * @return Информация о помощниках
     */
    public Map<Integer, Assistant> getAssistantsInfo() {
        Connection connection = MTHD.getInstance().database.getConnection();
        Map<Integer, Assistant> assistants = new HashMap<>();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                SELECT assistant_id, finished_at FROM single_finished_games_history""");
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                int assistantId = resultSet.getInt("assistant_id");
                Timestamp finishedAt = resultSet.getTimestamp("finished_at");

                Timestamp weekAgo = Timestamp.from(Instant.now().minus(Period.ofWeeks(1)));
                Timestamp dayAgo = Timestamp.from(Instant.now().minus(Period.ofDays(1)));

                Assistant assistant;
                if(assistants.containsKey(assistantId)) {
                    assistant = assistants.get(assistantId);
                } else {
                    String username = MTHD.getInstance().database.getUserName(assistantId);
                    assistant = new Assistant(assistantId, username);
                }

                if(finishedAt.after(weekAgo)) {
                    assistant.weeklyGames++;
                }

                if(finishedAt.after(dayAgo)) {
                    assistant.todayGames++;
                }

                assistant.games++;

                if(assistant.lastGameTimestamp == null) {
                    assistant.lastGameTimestamp = finishedAt;
                } else {
                    if(finishedAt.after(assistant.lastGameTimestamp)) {
                        assistant.lastGameTimestamp = finishedAt;
                    }
                }

                if(!assistants.containsKey(assistantId)) {
                    assistants.put(assistantId, assistant);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return assistants;
    }
}