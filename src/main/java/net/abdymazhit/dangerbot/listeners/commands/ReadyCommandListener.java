package net.abdymazhit.dangerbot.listeners.commands;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.managers.GameCategoryManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Команда готовности к игре
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class ReadyCommandListener extends ListenerAdapter {

    /**
     * Событие отправки команды
     */
    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Member member = event.getMember();

        if(!event.getName().equals("ready")) return;
        if(member == null) return;

        for(GameCategoryManager gameCategoryManager : DangerBot.getInstance().gameManager.gameCategories) {
            ready(gameCategoryManager, messageChannel, member, event);
        }
    }

    /**
     * Обрабатывает команды
     * @param gameCategoryManager Менеджер категория игры
     * @param messageChannel Канал
     * @param member Игрок
     * @param event Событие отправки команды
     */
    private void ready(GameCategoryManager gameCategoryManager, MessageChannel messageChannel, Member member, SlashCommandEvent event) {
        if(gameCategoryManager.readyChannel == null) return;

        if(gameCategoryManager.readyChannel.channel.equals(messageChannel)) {
            int readyId = DangerBot.getInstance().database.getUserId(member.getId());
            if(readyId < 0) {
                event.reply("Ошибка! Вы не зарегистрированы на сервере!").setEphemeral(true).queue();
                return;
            }

            boolean isGamePlayer = checkGamePlayer(readyId, gameCategoryManager.game.id);
            if(!isGamePlayer) {
                event.reply("Ошибка! Вы не являетесь игроком игры!").setEphemeral(true).queue();
                return;
            }

            String readyName = DangerBot.getInstance().database.getUserName(readyId);
            if(readyName == null) {
                event.reply("Ошибка! Вы не зарегистрированы на сервере!").setEphemeral(true).queue();
                return;
            }

            if(!gameCategoryManager.readyChannel.readyList.contains(readyName)) {
                gameCategoryManager.readyChannel.unreadyList.remove(readyName);
                gameCategoryManager.readyChannel.readyList.add(readyName);
                gameCategoryManager.readyChannel.updateReadyMessage();
                event.reply("Вы успешно стали готовым к игре!").setEphemeral(true).queue();
            } else {
                event.reply("Вы уже готовы к игре!").setEphemeral(true).queue();
            }
        }
    }

    private boolean checkGamePlayer(int userId, int gameId) {
        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id FROM single_live_games WHERE first_team_captain_id = ?;");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                if(gameId == id) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT id FROM single_live_games WHERE second_team_captain_id = ?;");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                if(gameId == id) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            Connection connection = DangerBot.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT live_game_id FROM single_live_games_players WHERE player_id = ?;");
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                int id = resultSet.getInt("live_game_id");
                if(gameId == id) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}