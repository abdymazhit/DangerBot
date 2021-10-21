package net.abdymazhit.mthd.listeners.commands.single;

import net.abdymazhit.mthd.MTHD;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Команды игроков
 *
 * @version   21.10.2021
 * @author    Islam Abdymazhit
 */
public class SingleCommandsListener extends ListenerAdapter {

    /** Команда посмотреть информацию о игроке */
    private final SingleInfoCommandListener singleInfoCommandListener;

    /** Команда посмотреть информацию о игроке по названию */
    private final SingleNameInfoCommandListener singleNameInfoCommandListener;

    /**
     * Инициализирует команды
     */
    public SingleCommandsListener() {
        this.singleInfoCommandListener = new SingleInfoCommandListener();
        this.singleNameInfoCommandListener = new SingleNameInfoCommandListener();
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

        if(MTHD.getInstance().playersChannel.channel.equals(messageChannel)) {
            if(contentRaw.equals("!info")) {
                singleInfoCommandListener.onCommandReceived(event);
            } else if(contentRaw.startsWith("!info")) {
                singleNameInfoCommandListener.onCommandReceived(event);
            } else
                message.reply("Ошибка! Неверная команда!").queue();
        }
    }
}