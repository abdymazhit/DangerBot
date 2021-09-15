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

        if(messageChannel.equals(MTHD.getInstance().adminChannel.channel)) {
            if(!MTHD.getInstance().adminChannel.channelMessage.equals(message)) {
                message.delete().submitAfter(30, TimeUnit.SECONDS);
            }
        } else if(messageChannel.equals(MTHD.getInstance().myTeamChannel.channel)) {
            if(!MTHD.getInstance().myTeamChannel.channelMessage.equals(message)) {
                message.delete().submitAfter(30, TimeUnit.SECONDS);
            }
        } else if(messageChannel.equals(MTHD.getInstance().staffChannel.channel)) {
            if(!MTHD.getInstance().staffChannel.channelMessage.equals(message)) {
                message.delete().submitAfter(30, TimeUnit.SECONDS);
            }
        } else if(messageChannel.equals(MTHD.getInstance().teamsChannel.channel)) {
            if(!MTHD.getInstance().teamsChannel.channelMessage.equals(message) &&
                    !MTHD.getInstance().teamsChannel.channelTopTeamsMessage.equals(message)) {
                message.delete().submitAfter(30, TimeUnit.SECONDS);
            }
        } else if(messageChannel.equals(MTHD.getInstance().authChannel.channel)) {
            if(!MTHD.getInstance().authChannel.channelMessage.equals(message)) {
                message.delete().submit();
            }
        } else if(messageChannel.equals(MTHD.getInstance().findGameChannel.channel)) {
            if(!MTHD.getInstance().findGameChannel.channelMessage.equals(message) &&
                    !MTHD.getInstance().findGameChannel.channelAvailableAssistantsMessage.equals(message)) {
                message.delete().submitAfter(15, TimeUnit.SECONDS);
            }
        }

        for(GameCategory gameCategory : MTHD.getInstance().gameManager.getGameCategories()) {
            if(gameCategory.playersChoiceChannel != null) {
                if(messageChannel.equals(gameCategory.playersChoiceChannel.channel)) {
                    if(!gameCategory.playersChoiceChannel.channelMessage.equals(message) &&
                            !gameCategory.playersChoiceChannel.channelGamePlayersMessage.equals(message)) {
                        message.delete().submitAfter(7, TimeUnit.SECONDS);
                        break;
                    }
                }
            } else if(gameCategory.mapChoiceChannel != null) {
                if(messageChannel.equals(gameCategory.mapChoiceChannel.channel)) {
                    if(!gameCategory.mapChoiceChannel.channelMessage.equals(message) &&
                            !gameCategory.mapChoiceChannel.channelMapsMessageId.equals(message.getId())) {
                        message.delete().submitAfter(7, TimeUnit.SECONDS);
                        break;
                    }
                }
            }
        }
    }
}