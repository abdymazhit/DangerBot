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
 * @version   15.10.2021
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
                    message.delete().queueAfter(120, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
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

        if(MTHD.getInstance().playersChannel.channelId != null && MTHD.getInstance().playersChannel.channelMessageId != null) {
            if(messageChannel.getId().equals(MTHD.getInstance().playersChannel.channelId)) {
                if(!message.getId().equals(MTHD.getInstance().playersChannel.channelMessageId)) {
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

        if(MTHD.getInstance().teamFindGameChannel.channelId != null && MTHD.getInstance().teamFindGameChannel.channelMessageId != null &&
           MTHD.getInstance().teamFindGameChannel.channelAvailableAssistantsMessageId != null) {
            if(messageChannel.getId().equals(MTHD.getInstance().teamFindGameChannel.channelId)) {
                if(!message.getId().equals(MTHD.getInstance().teamFindGameChannel.channelMessageId) &&
                   !message.getId().equals(MTHD.getInstance().teamFindGameChannel.channelAvailableAssistantsMessageId)) {
                    message.delete().queueAfter(3, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        if(MTHD.getInstance().singleFindGameChannel.channelId != null && MTHD.getInstance().singleFindGameChannel.channelMessageId != null &&
           MTHD.getInstance().singleFindGameChannel.channelAvailableAssistantsMessageId != null) {
            if(messageChannel.getId().equals(MTHD.getInstance().singleFindGameChannel.channelId)) {
                if(!message.getId().equals(MTHD.getInstance().singleFindGameChannel.channelMessageId) &&
                   !message.getId().equals(MTHD.getInstance().singleFindGameChannel.channelAvailableAssistantsMessageId)) {
                    message.delete().queueAfter(3, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                }
            }
        }

        for(GameCategoryManager gameCategoryManager : MTHD.getInstance().gameManager.gameCategories) {
            if(gameCategoryManager.playersPickChannel != null) {
                if(gameCategoryManager.playersPickChannel.channelId != null) {
                    if(messageChannel.getId().equals(gameCategoryManager.playersPickChannel.channelId)) {
                        if(gameCategoryManager.playersPickChannel.channelMessageId != null &&
                           gameCategoryManager.playersPickChannel.channelPlayersMessageId != null) {
                            if(!message.getId().equals(gameCategoryManager.playersPickChannel.channelMessageId) &&
                               !message.getId().equals(gameCategoryManager.playersPickChannel.channelPlayersMessageId)) {
                                if(gameCategoryManager.playersPickChannel.channelPlayersMessageId != null) {
                                    if(!message.getId().equals(gameCategoryManager.playersPickChannel.channelPlayersMessageId)) {
                                        message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                                    }
                                } else {
                                    message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if(gameCategoryManager.playersChoiceChannel != null) {
                if(gameCategoryManager.playersChoiceChannel.channelId != null) {
                    if(messageChannel.getId().equals(gameCategoryManager.playersChoiceChannel.channelId)) {
                        if(gameCategoryManager.playersChoiceChannel.channelMessageId != null &&
                           gameCategoryManager.playersChoiceChannel.channelGamePlayersMessageId != null) {
                            if(!message.getId().equals(gameCategoryManager.playersChoiceChannel.channelMessageId) &&
                               !message.getId().equals(gameCategoryManager.playersChoiceChannel.channelGamePlayersMessageId)) {
                                if(gameCategoryManager.playersChoiceChannel.channelGameCancelMessageId != null) {
                                    if(!message.getId().equals(gameCategoryManager.playersChoiceChannel.channelGameCancelMessageId)) {
                                        message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                                    }
                                } else {
                                    message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if(gameCategoryManager.mapChoiceChannel != null) {
                if(gameCategoryManager.mapChoiceChannel.channelId != null) {
                    if(messageChannel.getId().equals(gameCategoryManager.mapChoiceChannel.channelId)) {
                        if(gameCategoryManager.mapChoiceChannel.channelMessageId != null) {
                            if(gameCategoryManager.mapChoiceChannel.channelMapsMessageId != null) {
                                if(!message.getId().equals(gameCategoryManager.mapChoiceChannel.channelMessageId) &&
                                   !message.getId().equals(gameCategoryManager.mapChoiceChannel.channelMapsMessageId)) {
                                    message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                                    break;
                                }
                            } else {
                                if(!event.getAuthor().isBot()) message.delete().queue(null, ignore(UNKNOWN_MESSAGE));
                            }
                        }
                    }
                }
            } else if(gameCategoryManager.gameChannel != null) {
                if(gameCategoryManager.gameChannel.channelId != null) {
                    if(messageChannel.getId().equals(gameCategoryManager.gameChannel.channelId)) {
                        if(gameCategoryManager.gameChannel.channelMessageId != null) {
                            if(!message.getId().equals(gameCategoryManager.gameChannel.channelMessageId)) {
                                if(gameCategoryManager.gameChannel.channelGameCancelMessageId != null) {
                                    if(!message.getId().equals(gameCategoryManager.gameChannel.channelGameCancelMessageId)) {
                                        message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                                        break;
                                    }
                                } else {
                                    message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE, UNKNOWN_CHANNEL));
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if(gameCategoryManager.readyChannel != null) {
                if(gameCategoryManager.readyChannel.channelId != null) {
                    if(messageChannel.getId().equals(gameCategoryManager.readyChannel.channelId)) {
                        if(gameCategoryManager.readyChannel.channelMessageId != null) {
                            if(!message.getId().equals(gameCategoryManager.readyChannel.channelMessageId)) {
                                if(gameCategoryManager.readyChannel.channelReadyMessageId != null) {
                                    if(!message.getId().equals(gameCategoryManager.readyChannel.channelReadyMessageId)) {
                                        if(event.getAuthor().isBot()) {
                                            if(!message.isEphemeral()) {
                                                message.delete().queueAfter(7, TimeUnit.SECONDS, null, ignore(UNKNOWN_MESSAGE));
                                            }
                                        } else {
                                            message.delete().queue(null, ignore(UNKNOWN_MESSAGE));
                                        }
                                        break;
                                    }
                                } else {
                                    if(!event.getAuthor().isBot()) {
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
    }
}