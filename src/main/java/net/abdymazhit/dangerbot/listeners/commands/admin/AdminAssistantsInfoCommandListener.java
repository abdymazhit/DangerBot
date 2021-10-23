package net.abdymazhit.dangerbot.listeners.commands.admin;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.customs.info.AssistantInfo;
import net.abdymazhit.dangerbot.enums.UserRole;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
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
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminAssistantsInfoCommandListener {

    /**
     * Событие получения команды
     */
    public void onCommandReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Member adminMember = event.getMember();

        if(event.getAuthor().isBot()) return;
        if(adminMember == null) return;

        if(!adminMember.getRoles().contains(UserRole.ADMIN.getRole())) {
            message.reply("Ошибка! У вас нет прав для этого действия!").queue();
            return;
        }

        if(!adminMember.getRoles().contains(UserRole.AUTHORIZED.getRole())) {
            message.reply("Ошибка! Вы не авторизованы!").queue();
            return;
        }

        Map<Integer, AssistantInfo> assistantsInfo = getAssistantsInfo();

        // Получает discord id помощников
        List<Long> discordIds = new ArrayList<>();
        for(AssistantInfo assistantInfo : assistantsInfo.values()) {
            if(assistantInfo.discordId != null) {
                discordIds.add(Long.valueOf(assistantInfo.discordId));
            }
        }

        // Получает пользователей по discord id
        DangerBot.getInstance().guild.retrieveMembersByIds(discordIds).onSuccess(members -> {
            Map<Integer, AssistantInfo> assistants = new HashMap<>();

            // Добавляет текущих помощников в список
            for(Member member : members) {
                if(member.getRoles().contains(UserRole.ASSISTANT.getRole())
                   || member.getRoles().contains(UserRole.ADMIN.getRole())) {
                    for(AssistantInfo assistantInfo : new HashMap<>(assistantsInfo).values()) {
                        if(assistantInfo.discordId != null) {
                            if(assistantInfo.discordId.equals(member.getId())) {
                                assistants.put(assistantInfo.id, assistantInfo);
                            }
                        }
                    }
                }
            }

            // Сортирует помощников по проведенным сегодняшним играм
            List<AssistantInfo> assistantInfoList = assistants.values().stream()
                    .sorted(Comparator.comparing(AssistantInfo::getTodayGames)).collect(Collectors.toList());
            List<AssistantInfo> assistantInfoListSorted = new ArrayList<>();
            for(int i = assistantInfoList.size() - 1; i >= 0; i--) {
                assistantInfoListSorted.add(assistantInfoList.get(i));
            }

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("""
                    ```              Информация о помощниках              ```""");
            embedBuilder.setColor(3092790);

            StringBuilder namesString = new StringBuilder();
            for(AssistantInfo assistantInfo : assistantInfoListSorted) {
                namesString.append(assistantInfo.username.replace("_", "\\_")).append("\n");
            }
            embedBuilder.addField("Name", namesString.toString(), true);

            StringBuilder allGamesString = new StringBuilder();
            for(AssistantInfo assistantInfo : assistantInfoListSorted) {
                allGamesString.append(assistantInfo.games).append("-")
                        .append(assistantInfo.weeklyGames).append("-")
                        .append(assistantInfo.todayGames).append("\n");
            }
            embedBuilder.addField("All-Weekly-Today", allGamesString.toString(), true);

            StringBuilder lastGameTimestampString = new StringBuilder();
            for(AssistantInfo assistantInfo : assistantInfoListSorted) {
                Timestamp lastGameTimestamp = assistantInfo.lastGameTimestamp;

                int hours;
                if(lastGameTimestamp != null) {
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
        });
    }

    /**
     * Получает информацию о помощниках
     * @return Информация о помощниках
     */
    public Map<Integer, AssistantInfo> getAssistantsInfo() {
        Connection connection = DangerBot.getInstance().database.getConnection();
        Map<Integer, AssistantInfo> assistants = new HashMap<>();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                SELECT assistant_id, finished_at FROM single_finished_games_history""");
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                int assistantId = resultSet.getInt("assistant_id");
                Timestamp finishedAt = resultSet.getTimestamp("finished_at");

                Timestamp weekAgo = Timestamp.from(Instant.now().minus(Period.ofWeeks(1)));
                Timestamp dayAgo = Timestamp.from(Instant.now().minus(Period.ofDays(1)));

                AssistantInfo assistantInfo;
                if(assistants.containsKey(assistantId)) {
                    assistantInfo = assistants.get(assistantId);
                } else {
                    String username = DangerBot.getInstance().database.getUserName(assistantId);
                    String discordId = DangerBot.getInstance().database.getUserDiscordId(assistantId);
                    assistantInfo = new AssistantInfo(assistantId, username, discordId);
                }

                if(finishedAt.after(weekAgo)) {
                    assistantInfo.weeklyGames++;
                }

                if(finishedAt.after(dayAgo)) {
                    assistantInfo.todayGames++;
                }

                assistantInfo.games++;

                if(assistantInfo.lastGameTimestamp == null) {
                    assistantInfo.lastGameTimestamp = finishedAt;
                } else {
                    if(finishedAt.after(assistantInfo.lastGameTimestamp)) {
                        assistantInfo.lastGameTimestamp = finishedAt;
                    }
                }

                if(!assistants.containsKey(assistantId)) {
                    assistants.put(assistantId, assistantInfo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return assistants;
    }
}