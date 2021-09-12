package net.abdymazhit.mthd.listeners;

import net.abdymazhit.mthd.MTHD;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

/**
 * Очищает сообщения канала
 *
 * @version   12.09.2021
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
                    !MTHD.getInstance().findGameChannel.channelTeamsInGameSearchMessage.equals(message) &&
                    !MTHD.getInstance().findGameChannel.channelAvailableAssistantsMessage.equals(message)) {
                message.delete().submitAfter(5, TimeUnit.SECONDS);
            }
        }
    }
}