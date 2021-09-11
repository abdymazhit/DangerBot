package net.abdymazhit.mthd.listeners.commands.admin;

import net.abdymazhit.mthd.MTHD;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Администраторские команды
 *
 * @version   11.09.2021
 * @author    Islam Abdymazhit
 */
public class AdminCommandsListener extends ListenerAdapter {

    /** Команда добавления участника в команду */
    private final AdminTeamAddCommandListener adminTeamAddCommandListener;

    /** Команда создания команды */
    private final AdminTeamCreateCommandListener adminTeamCreateCommandListener;

    /** Команда удаления участника из команды */
    private final AdminTeamDeleteCommandListener adminTeamDeleteCommandListener;

    /** Команда удаления команды */
    private final AdminTeamDisbandCommandListener adminTeamDisbandCommandListener;

    /** Команда переименования команды */
    private final AdminTeamRenameCommandListener adminTeamRenameCommandListener;

    /** Команда передачи прав лидера команды */
    private final AdminTeamTransferCommandListener adminTeamTransferCommandListener;

    /**
     * Инициализирует команды
     */
    public AdminCommandsListener() {
        this.adminTeamAddCommandListener = new AdminTeamAddCommandListener();
        this.adminTeamCreateCommandListener = new AdminTeamCreateCommandListener();
        this.adminTeamDeleteCommandListener = new AdminTeamDeleteCommandListener();
        this.adminTeamDisbandCommandListener = new AdminTeamDisbandCommandListener();
        this.adminTeamRenameCommandListener = new AdminTeamRenameCommandListener();
        this.adminTeamTransferCommandListener = new AdminTeamTransferCommandListener();
    }

    /**
     * Событие получения сообщений
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();

        if(event.getAuthor().isBot()) return;

        if(MTHD.getInstance().adminChannel.channel.equals(messageChannel)) {
            String contentRaw = message.getContentRaw();
            if(contentRaw.startsWith("!adminteam add")) {
                adminTeamAddCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!adminteam create")) {
                adminTeamCreateCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!adminteam delete")) {
                adminTeamDeleteCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!adminteam disband")) {
                adminTeamDisbandCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!adminteam rename")) {
                adminTeamRenameCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!adminteam transfer")) {
                adminTeamTransferCommandListener.onCommandReceived(event);
            } else {
                message.reply("Ошибка! Неверная команда!").queue();
            }
        }
    }
}
