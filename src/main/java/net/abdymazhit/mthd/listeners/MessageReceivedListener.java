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
 * @version   09.09.2021
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

        if(messageChannel.equals(MTHD.getInstance().authChannel.channel) ||
                messageChannel.equals(MTHD.getInstance().myTeamChannel.channel) ||
                        messageChannel.equals(MTHD.getInstance().adminChannel.channel)
                ) {
            if(!MTHD.getInstance().authChannel.channelMessage.equals(message)) {
                message.delete().queue();
            } else if(!MTHD.getInstance().myTeamChannel.channelMessage.equals(message)) {
                message.delete().queueAfter(30, TimeUnit.SECONDS);
            } else if(!MTHD.getInstance().adminChannel.channelMessage.equals(message)) {
                message.delete().queueAfter(30, TimeUnit.SECONDS);
            }
        }
    }
}