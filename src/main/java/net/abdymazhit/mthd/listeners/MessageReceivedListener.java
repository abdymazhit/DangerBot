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
 * @version   11.09.2021
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

        if(messageChannel.equals(MTHD.getInstance().adminChannel.channel) ||
                messageChannel.equals(MTHD.getInstance().myTeamChannel.channel) ||
                messageChannel.equals(MTHD.getInstance().staffChannel.channel) ||
                messageChannel.equals(MTHD.getInstance().teamsChannel.channel) ||
                messageChannel.equals(MTHD.getInstance().topTeamsChannel.channel)) {
            if(!MTHD.getInstance().adminChannel.channelMessage.equals(message) &&
                    !MTHD.getInstance().myTeamChannel.channelMessage.equals(message) &&
                    !MTHD.getInstance().staffChannel.channelMessage.equals(message) &&
                    !MTHD.getInstance().teamsChannel.channelMessage.equals(message) &&
                    !MTHD.getInstance().topTeamsChannel.channelMessage.equals(message)) {
                message.delete().submitAfter(30, TimeUnit.SECONDS);
            }
        }

        if(messageChannel.equals(MTHD.getInstance().authChannel.channel)) {
            if(!MTHD.getInstance().authChannel.channelMessage.equals(message)) {
                message.delete().submit();
            }
        }
    }
}