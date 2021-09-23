package net.abdymazhit.mthd.listeners;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.game.GameCategory;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.exceptions.ErrorResponseException.ignore;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_MESSAGE;

/**
 * Очищает сообщения канала
 *
 * @version   23.09.2021
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

        if(MTHD.getInstance().adminChannel.channelId != null && MTHD.getInstance().adminChannel.channelMessageId != null) {
            if(messageChannel.getId().equals(MTHD.getInstance().adminChannel.channelId)) {
                if(!message.getId().equals(MTHD.getInstance().adminChannel.channelMessageId)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(MTHD.getInstance().myTeamChannel.channelId != null && MTHD.getInstance().myTeamChannel.channelMessageId != null) {
            if(messageChannel.getId().equals(MTHD.getInstance().myTeamChannel.channelId)) {
                if(!message.getId().equals(MTHD.getInstance().myTeamChannel.channelMessageId)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(MTHD.getInstance().staffChannel.channelId != null && MTHD.getInstance().staffChannel.channelMessageId != null) {
            if(messageChannel.getId().equals(MTHD.getInstance().staffChannel.channelId)) {
                if(!message.getId().equals(MTHD.getInstance().staffChannel.channelMessageId)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(MTHD.getInstance().teamsChannel.channelId != null && MTHD.getInstance().teamsChannel.channelMessageId != null) {
            if(messageChannel.getId().equals(MTHD.getInstance().teamsChannel.channelId)) {
                if(!message.getId().equals(MTHD.getInstance().teamsChannel.channelMessageId)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(MTHD.getInstance().authChannel.channelId != null && MTHD.getInstance().authChannel.channelMessageId != null) {
            if(messageChannel.getId().equals(MTHD.getInstance().authChannel.channelId)) {
                if(!message.getId().equals(MTHD.getInstance().authChannel.channelMessageId)) {
                    if(!message.isEphemeral()) {
                        message.delete().queue(null, ignore(UNKNOWN_MESSAGE));
                    }
                }
            }
        }

        if(MTHD.getInstance().findGameChannel.channelId != null && MTHD.getInstance().findGameChannel.channelMessageId != null &&
                MTHD.getInstance().findGameChannel.channelAvailableAssistantsMessageId != null) {
            if(messageChannel.getId().equals(MTHD.getInstance().findGameChannel.channelId)) {
                if(!message.getId().equals(MTHD.getInstance().findGameChannel.channelMessageId) &&
                        !message.getId().equals(MTHD.getInstance().findGameChannel.channelAvailableAssistantsMessageId)) {
                    message.delete().queueAfter(3, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        for(GameCategory gameCategory : MTHD.getInstance().gameManager.getGameCategories()) {
            if(gameCategory.playersChoiceChannel != null) {
                if(gameCategory.playersChoiceChannel.channelId != null) {
                    if(messageChannel.getId().equals(gameCategory.playersChoiceChannel.channelId)) {
                        if(gameCategory.playersChoiceChannel.channelMessageId != null &&
                                gameCategory.playersChoiceChannel.channelGamePlayersMessageId != null) {
                            if(!message.getId().equals(gameCategory.playersChoiceChannel.channelMessageId) &&
                                    !message.getId().equals(gameCategory.playersChoiceChannel.channelGamePlayersMessageId)) {
                                if(gameCategory.playersChoiceChannel.channelGameCancelMessageId != null) {
                                    if(!message.getId().equals(gameCategory.playersChoiceChannel.channelGameCancelMessageId)) {
                                        message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                                    }
                                } else {
                                    message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if(gameCategory.mapChoiceChannel != null) {
                if(gameCategory.mapChoiceChannel.channelId != null) {
                    if(messageChannel.getId().equals(gameCategory.mapChoiceChannel.channelId)) {
                        if(gameCategory.mapChoiceChannel.channelMessageId != null) {
                            if(gameCategory.mapChoiceChannel.channelMapsMessageId != null) {
                                if(!message.getId().equals(gameCategory.mapChoiceChannel.channelMessageId) &&
                                   !message.getId().equals(gameCategory.mapChoiceChannel.channelMapsMessageId)) {
                                    message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                                    break;
                                }
                            } else {
                                if(!event.getAuthor().isBot()) message.delete().queue(null, ignore(UNKNOWN_MESSAGE));
                            }
                        }
                    }
                }
            } else if(gameCategory.gameChannel != null) {
                if(gameCategory.gameChannel.channelId != null) {
                    if(messageChannel.getId().equals(gameCategory.gameChannel.channelId)) {
                        if(gameCategory.gameChannel.channelMessageId != null) {
                            if(!message.getId().equals(gameCategory.gameChannel.channelMessageId)) {
                                if(gameCategory.gameChannel.channelGameCancelMessageId != null) {
                                    if(!message.getId().equals(gameCategory.gameChannel.channelGameCancelMessageId)) {
                                        message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                                        break;
                                    }
                                } else {
                                    message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}