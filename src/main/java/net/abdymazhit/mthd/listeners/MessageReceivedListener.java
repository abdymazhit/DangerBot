package net.abdymazhit.mthd.listeners;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.managers.GameCategoryManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.exceptions.ErrorResponseException.ignore;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_CHANNEL;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_MESSAGE;

/**
 * Очищает сообщения канала
 *
 * @version   21.10.2021
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
            if(MTHD.getInstance().adminChannel.channelMessage != null) {
                if(!message.equals(MTHD.getInstance().adminChannel.channelMessage)) {
                    message.delete().queueAfter(120, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(MTHD.getInstance().myTeamChannel.channel)) {
            if(MTHD.getInstance().myTeamChannel.channelMessage != null) {
                if(!message.equals(MTHD.getInstance().myTeamChannel.channelMessage)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(MTHD.getInstance().staffChannel.channel)) {
            if(MTHD.getInstance().staffChannel.channelMessage != null) {
                if(!message.equals(MTHD.getInstance().staffChannel.channelMessage)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(MTHD.getInstance().teamsChannel.channel)) {
            if(MTHD.getInstance().teamsChannel.channelMessage != null) {
                if(!message.equals(MTHD.getInstance().teamsChannel.channelMessage)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(MTHD.getInstance().playersChannel.channel)) {
            if(MTHD.getInstance().playersChannel.channelMessage != null) {
                if(!message.equals(MTHD.getInstance().playersChannel.channelMessage)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(MTHD.getInstance().authChannel.channel)) {
            if(MTHD.getInstance().authChannel.channelMessage != null) {
                if(!message.equals(MTHD.getInstance().authChannel.channelMessage)) {
                    if(!message.isEphemeral()) {
                        message.delete().queue(null, ignore(UNKNOWN_MESSAGE));
                    }
                }
            }
        }

        if(messageChannel.equals(MTHD.getInstance().teamFindGameChannel.channel)) {
            if(MTHD.getInstance().teamFindGameChannel.channelMessage != null &&
               MTHD.getInstance().teamFindGameChannel.channelAvailableAssistantsMessage != null) {
                if(!message.equals(MTHD.getInstance().teamFindGameChannel.channelMessage) &&
                   !message.equals(MTHD.getInstance().teamFindGameChannel.channelAvailableAssistantsMessage)) {
                    message.delete().queueAfter(3, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(MTHD.getInstance().singleFindGameChannel.channel)) {
            if(MTHD.getInstance().singleFindGameChannel.channelMessage != null &&
               MTHD.getInstance().singleFindGameChannel.channelAvailableAssistantsMessage != null) {
                if(!message.equals(MTHD.getInstance().singleFindGameChannel.channelMessage) &&
                   !message.equals(MTHD.getInstance().singleFindGameChannel.channelAvailableAssistantsMessage)) {
                    message.delete().queueAfter(3, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        for(GameCategoryManager gameCategoryManager : MTHD.getInstance().gameManager.gameCategories) {
            if(gameCategoryManager.playersPickChannel.channelMessage != null) {
                if(messageChannel.equals(gameCategoryManager.playersPickChannel.channel)) {
                    if(!message.equals(gameCategoryManager.playersPickChannel.channelMessage) &&
                       !message.equals(gameCategoryManager.playersPickChannel.channelPlayersMessage)) {
                        if(gameCategoryManager.playersPickChannel.channelPlayersMessage != null) {
                            if(!message.equals(gameCategoryManager.playersPickChannel.channelPlayersMessage)) {
                                message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                            }
                        } else {
                            message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                            break;
                        }
                    }
                }
            } else if(gameCategoryManager.playersChoiceChannel.channelMessage != null) {
                if(messageChannel.equals(gameCategoryManager.playersChoiceChannel.channel)) {
                    if(!message.equals(gameCategoryManager.playersChoiceChannel.channelMessage)) {
                        if(gameCategoryManager.playersChoiceChannel.channelGameCancelMessage != null) {
                            if(!message.equals(gameCategoryManager.playersChoiceChannel.channelGameCancelMessage)) {
                                message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                            }
                        } else {
                            message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                            break;
                        }
                    }
                }
            } else if(gameCategoryManager.mapChoiceChannel.channelMessage != null) {
                if(messageChannel.equals(gameCategoryManager.mapChoiceChannel.channel)) {
                    if(gameCategoryManager.mapChoiceChannel.channelMapsMessage != null) {
                        if(!message.equals(gameCategoryManager.mapChoiceChannel.channelMessage) &&
                           !message.equals(gameCategoryManager.mapChoiceChannel.channelMapsMessage)) {
                            message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                            break;
                        }
                    } else {
                        if(!event.getAuthor().isBot()) message.delete().queue(null, ignore(UNKNOWN_MESSAGE));
                    }
                }
            } else if(gameCategoryManager.gameChannel.channelMessage != null) {
                if(messageChannel.equals(gameCategoryManager.gameChannel.channel)) {
                    if(!message.equals(gameCategoryManager.gameChannel.channelMessage)) {
                        message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                        break;
                    }
                }
            } else if(gameCategoryManager.readyChannel.channelMessage != null) {
                if(messageChannel.equals(gameCategoryManager.readyChannel.channel)) {
                    if(!message.equals(gameCategoryManager.readyChannel.channelMessage)) {
                        if(!message.equals(gameCategoryManager.readyChannel.channelReadyMessage)) {
                            if(event.getAuthor().isBot()) {
                                if(!message.isEphemeral()) {
                                    message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                                }
                            } else {
                                message.delete().queue(null, ignore(UNKNOWN_MESSAGE));
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}