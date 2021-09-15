package net.abdymazhit.mthd.listeners.commands.team;

import net.abdymazhit.mthd.MTHD;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Команды команд
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamCommandsListener extends ListenerAdapter {

    /** Команда удалить команду */
    private final TeamDisbandCommandListener teamDisbandCommandListener;

    /** Команда посмотреть информацию о команде */
    private final TeamInfoCommandListener teamInfoCommandListener;

    /** Команда исключить участника из команды */
    private final TeamKickCommandListener teamKickCommandListener;

    /** Команда покинуть команду */
    private final TeamLeaveCommandListener teamLeaveCommandListener;

    /** Команда посмотреть информацию о команде по названию */
    private final TeamNameInfoCommandListener teamNameInfoCommandListener;

    /** Команда передать права лидера */
    private final TeamTransferCommandListener teamTransferCommandListener;

    /**
     * Инициализирует команды
     */
    public TeamCommandsListener() {
        this.teamDisbandCommandListener = new TeamDisbandCommandListener();
        this.teamInfoCommandListener = new TeamInfoCommandListener();
        this.teamKickCommandListener = new TeamKickCommandListener();
        this.teamLeaveCommandListener = new TeamLeaveCommandListener();
        this.teamNameInfoCommandListener = new TeamNameInfoCommandListener();
        this.teamTransferCommandListener = new TeamTransferCommandListener();
    }

    /**
     * Событие получения сообщений
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        String contentRaw = message.getContentRaw();

        if(event.getAuthor().isBot()) return;

        if(MTHD.getInstance().myTeamChannel.channelId.equals(messageChannel.getId())) {
            if(contentRaw.startsWith("!team disband")) {
                teamDisbandCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!team kick")) {
                teamKickCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!team leave")) {
                teamLeaveCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!team transfer")) {
                teamTransferCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!team info")) {
                teamInfoCommandListener.onCommandReceived(event);
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        } else if(MTHD.getInstance().teamsChannel.channelId.equals(messageChannel.getId())) {
            if(contentRaw.startsWith("!team info")) {
                teamNameInfoCommandListener.onCommandReceived(event);
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }
}