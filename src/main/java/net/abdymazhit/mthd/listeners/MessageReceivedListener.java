package net.abdymazhit.mthd.listeners;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.game.GameCategory;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

/**
 * Очищает сообщения канала
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class MessageReceivedListener extends ListenerAdapter {

    /**
     * Событие получения сообщений
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        MessageChannel messageChannel = event.getChannel();

        if(messageChannel.getId().equals(MTHD.getInstance().adminChannel.channelId)) {
            if(!MTHD.getInstance().adminChannel.channelMessageId.equals(message.getId())) {
                message.delete().queueAfter(30, TimeUnit.SECONDS);
            }
        } else if(messageChannel.getId().equals(MTHD.getInstance().myTeamChannel.channelId)) {
            if(!MTHD.getInstance().myTeamChannel.channelMessageId.equals(message.getId())) {
                message.delete().queueAfter(30, TimeUnit.SECONDS);
            }
        } else if(messageChannel.getId().equals(MTHD.getInstance().staffChannel.channelId)) {
            if(!MTHD.getInstance().staffChannel.channelMessageId.equals(message.getId())) {
                message.delete().queueAfter(30, TimeUnit.SECONDS);
            }
        } else if(messageChannel.getId().equals(MTHD.getInstance().teamsChannel.channelId)) {
            if(!MTHD.getInstance().teamsChannel.channelMessageId.equals(message.getId()) &&
                    !MTHD.getInstance().teamsChannel.channelTopTeamsMessageId.equals(message.getId())) {
                message.delete().queueAfter(30, TimeUnit.SECONDS);
            }
        } else if(messageChannel.getId().equals(MTHD.getInstance().authChannel.channelId)) {
            if(!MTHD.getInstance().authChannel.channelMessageId.equals(message.getId())) {
                message.delete().queue();
            }
        } else if(messageChannel.getId().equals(MTHD.getInstance().findGameChannel.channelId)) {
            if(!MTHD.getInstance().findGameChannel.channelMessageId.equals(message.getId()) &&
                    !MTHD.getInstance().findGameChannel.channelAvailableAssistantsMessageId.equals(message.getId())) {
                message.delete().queueAfter(15, TimeUnit.SECONDS);
            }
        }

        for(GameCategory gameCategory : MTHD.getInstance().gameManager.getGameCategories()) {
            if(gameCategory.playersChoiceChannel != null) {
                if(messageChannel.getId().equals(gameCategory.playersChoiceChannel.channelId)) {
                    if(!gameCategory.playersChoiceChannel.channelMessageId.equals(message.getId()) &&
                            !gameCategory.playersChoiceChannel.channelGamePlayersMessageId.equals(message.getId())) {
                        if(gameCategory.playersChoiceChannel.channelGameCancelMessageId != null) {
                            if(!gameCategory.playersChoiceChannel.channelGameCancelMessageId.equals(message.getId())) {
                                message.delete().queueAfter(7, TimeUnit.SECONDS);
                                break;
                            }
                        } else {
                            message.delete().queueAfter(7, TimeUnit.SECONDS);
                            break;
                        }
                    }
                }
            } else if(gameCategory.mapChoiceChannel != null) {
                if(messageChannel.getId().equals(gameCategory.mapChoiceChannel.channelId)) {
                    if(!gameCategory.mapChoiceChannel.channelMessageId.equals(message.getId()) &&
                            !gameCategory.mapChoiceChannel.channelMapsMessageId.equals(message.getId())) {
                        message.delete().queueAfter(7, TimeUnit.SECONDS);
                        break;
                    }
                }
            } else if(gameCategory.gameChannel != null) {
                if(messageChannel.getId().equals(gameCategory.gameChannel.channelId)) {
                    if(!gameCategory.gameChannel.channelMessageId.equals(message.getId())) {
                        message.delete().queueAfter(7, TimeUnit.SECONDS);
                        break;
                    }
                }
            }
        }
    }
}