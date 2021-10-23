package net.abdymazhit.dangerbot.listeners;

import net.abdymazhit.dangerbot.DangerBot;
import net.abdymazhit.dangerbot.managers.GameCategoryManager;
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
 * @version   23.10.2021
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

        if(messageChannel.equals(DangerBot.getInstance().adminChannel.channel)) {
            if(DangerBot.getInstance().adminChannel.channelMessage != null) {
                if(!message.equals(DangerBot.getInstance().adminChannel.channelMessage)) {
                    message.delete().queueAfter(120, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(DangerBot.getInstance().myTeamChannel.channel)) {
            if(DangerBot.getInstance().myTeamChannel.channelMessage != null) {
                if(!message.equals(DangerBot.getInstance().myTeamChannel.channelMessage)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(DangerBot.getInstance().staffChannel.channel)) {
            if(DangerBot.getInstance().staffChannel.channelMessage != null) {
                if(!message.equals(DangerBot.getInstance().staffChannel.channelMessage)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(DangerBot.getInstance().streamsChannel.channel)) {
            if(DangerBot.getInstance().streamsChannel.channelMessage != null) {
                if(!message.equals(DangerBot.getInstance().streamsChannel.channelMessage)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(DangerBot.getInstance().teamsChannel.channel)) {
            if(DangerBot.getInstance().teamsChannel.channelMessage != null) {
                if(!message.equals(DangerBot.getInstance().teamsChannel.channelMessage)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(DangerBot.getInstance().playersChannel.channel)) {
            if(DangerBot.getInstance().playersChannel.channelMessage != null) {
                if(!message.equals(DangerBot.getInstance().playersChannel.channelMessage)) {
                    message.delete().queueAfter(30, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(DangerBot.getInstance().authChannel.channel)) {
            if(DangerBot.getInstance().authChannel.channelMessage != null) {
                if(!message.equals(DangerBot.getInstance().authChannel.channelMessage)) {
                    if(!message.isEphemeral()) {
                        message.delete().queue(null, ignore(UNKNOWN_MESSAGE));
                    }
                }
            }
        }

        if(messageChannel.equals(DangerBot.getInstance().teamFindGameChannel.channel)) {
            if(DangerBot.getInstance().teamFindGameChannel.channelMessage != null &&
               DangerBot.getInstance().teamFindGameChannel.channelAvailableAssistantsMessage != null) {
                if(!message.equals(DangerBot.getInstance().teamFindGameChannel.channelMessage)
                   && !message.equals(DangerBot.getInstance().teamFindGameChannel.channelAvailableAssistantsMessage)) {
                    message.delete().queueAfter(3, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(messageChannel.equals(DangerBot.getInstance().singleFindGameChannel.channel)) {
            if(DangerBot.getInstance().singleFindGameChannel.channelMessage != null &&
               DangerBot.getInstance().singleFindGameChannel.channelAvailableAssistantsMessage != null) {
                if(!message.equals(DangerBot.getInstance().singleFindGameChannel.channelMessage)
                   && !message.equals(DangerBot.getInstance().singleFindGameChannel.channelAvailableAssistantsMessage)) {
                    message.delete().queueAfter(3, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        for(GameCategoryManager gameCategoryManager : DangerBot.getInstance().gameManager.gameCategories) {
            if(gameCategoryManager.playersPickChannel != null && gameCategoryManager.playersPickChannel.channel != null) {
                if(messageChannel.equals(gameCategoryManager.playersPickChannel.channel)) {
                    if(gameCategoryManager.playersPickChannel.channelMessage != null
                       && gameCategoryManager.playersPickChannel.channelPlayersMessage != null) {
                        if(!message.equals(gameCategoryManager.playersPickChannel.channelMessage)
                           && !message.equals(gameCategoryManager.playersPickChannel.channelPlayersMessage)) {
                            message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                        }
                    }
                }
            } else if(gameCategoryManager.playersChoiceChannel != null && gameCategoryManager.playersChoiceChannel.channel != null) {
                if(messageChannel.equals(gameCategoryManager.playersChoiceChannel.channel)) {
                    if(gameCategoryManager.playersChoiceChannel.channelMessage != null
                       && gameCategoryManager.playersChoiceChannel.channelPlayersMessage != null) {
                        if(!message.equals(gameCategoryManager.playersChoiceChannel.channelMessage)
                           && !message.equals(gameCategoryManager.playersChoiceChannel.channelPlayersMessage)) {
                            message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                        }
                    }
                }
            } else if(gameCategoryManager.mapChoiceChannel != null && gameCategoryManager.mapChoiceChannel.channel != null) {
                if(messageChannel.equals(gameCategoryManager.mapChoiceChannel.channel)) {
                    if(gameCategoryManager.mapChoiceChannel.channelMessage != null
                       && gameCategoryManager.mapChoiceChannel.channelMapsMessage != null) {
                        if(!message.equals(gameCategoryManager.mapChoiceChannel.channelMessage)
                           && !message.equals(gameCategoryManager.mapChoiceChannel.channelMapsMessage)) {
                            message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                        }
                    }
                }
            } else if(gameCategoryManager.gameChannel != null && gameCategoryManager.gameChannel.channel != null) {
                if(messageChannel.equals(gameCategoryManager.gameChannel.channel)) {
                    if(gameCategoryManager.gameChannel.channelMessage != null) {
                        if(!message.equals(gameCategoryManager.gameChannel.channelMessage)) {
                            message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                        }
                    }
                }
            } else if(gameCategoryManager.readyChannel != null && gameCategoryManager.readyChannel.channel != null) {
                if(messageChannel.equals(gameCategoryManager.readyChannel.channel)) {
                    if(gameCategoryManager.readyChannel.channelMessage != null
                       && gameCategoryManager.readyChannel.channelReadyMessage != null) {
                        if(!message.equals(gameCategoryManager.readyChannel.channelMessage)
                           && !message.equals(gameCategoryManager.readyChannel.channelReadyMessage)) {
                            if(!event.getAuthor().isBot()) {
                                message.delete().queue(null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                            }
                        }
                    }
                }
            }
        }
    }
}