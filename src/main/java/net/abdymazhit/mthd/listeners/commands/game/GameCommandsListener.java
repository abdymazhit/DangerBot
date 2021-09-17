package net.abdymazhit.mthd.listeners.commands.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.UserRole;
import net.abdymazhit.mthd.game.GameCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Команда отмены игры
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public class GameCommandsListener extends ListenerAdapter {

    /**
     * Событие получения команды
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel messageChannel = event.getChannel();
        Message message = event.getMessage();
        Member canceller = event.getMember();

        if(canceller == null) return;
        if(event.getAuthor().isBot()) return;

        for(GameCategory gameCategory : MTHD.getInstance().gameManager.getGameCategories()) {
            if(gameCategory.gameChannel == null) return;

            if(gameCategory.gameChannel.channelId.equals(messageChannel.getId())) {
                String contentRaw = message.getContentRaw();
                if(contentRaw.equals("!start")) {
                    if(!canceller.getRoles().contains(UserRole.ADMIN.getRole()) &&
                            canceller.getRoles().contains(UserRole.ASSISTANT.getRole())) {
                        message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                        return;
                    }

                    int cancellerId = MTHD.getInstance().database.getUserId(canceller.getId());
                    if(cancellerId < 0) {
                        message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                        return;
                    }

                    if(!canceller.getRoles().contains(UserRole.ADMIN.getRole())) {
                        if(cancellerId == gameCategory.game.assistantId) {
                            message.reply("Ошибка! Только помощник этой игры может начать игру!").queue();
                            return;
                        }
                    }

                    message.reply("Вы успешно начали игру!").queue();
                    gameCategory.setGameState(GameState.GAME);
                    gameCategory.gameChannel.timer.cancel();
                    MTHD.getInstance().liveGamesManager.addLiveGame(gameCategory.game);

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle(gameCategory.game.firstTeamName + " vs " + gameCategory.game.secondTeamName);

                    StringBuilder firstTeamPlayersStrings = new StringBuilder();
                    for(String username : gameCategory.game.firstTeamPlayers) {
                        firstTeamPlayersStrings.append(username).append("\n");
                    }

                    StringBuilder secondTeamPlayersStrings = new StringBuilder();
                    for(String username : gameCategory.game.secondTeamPlayers) {
                        secondTeamPlayersStrings.append(username).append("\n");
                    }

                    embedBuilder.setColor(0xFF58B9FF);
                    embedBuilder.addField("Команда " + gameCategory.game.firstTeamName, String.valueOf(firstTeamPlayersStrings), true);
                    embedBuilder.addField("Команда " + gameCategory.game.secondTeamName, String.valueOf(secondTeamPlayersStrings), true);
                    embedBuilder.addField("Отмены игры", "Данная команда доступна только для администрации. " +
                            "Для отмены игры введите `!cancel`", false);

                    messageChannel.editMessageEmbedsById(gameCategory.gameChannel.channelMessageId, embedBuilder.build()).queue();
                    embedBuilder.clear();
                } else if(contentRaw.equals("!cancel")) {
                    if(!canceller.getRoles().contains(UserRole.ADMIN.getRole())) {
                        message.reply("Ошибка! У вас нет прав для этого действия!").queue();
                        return;
                    }

                    int cancellerId = MTHD.getInstance().database.getUserId(canceller.getId());
                    if(cancellerId < 0) {
                        message.reply("Ошибка! Вы не зарегистрированы на сервере!").queue();
                        return;
                    }

                    message.reply("Вы успешно отменили игру!").queue();
                    MTHD.getInstance().gameManager.deleteGame(gameCategory.game);

                    if(gameCategory.gameChannel.timer != null) {
                        gameCategory.gameChannel.timer.cancel();
                    }
                    MTHD.getInstance().liveGamesManager.removeLiveGame(gameCategory.game);

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            MTHD.getInstance().gameManager.deleteGame(gameCategory.categoryId);
                        }
                    }, 7000);
                } else {
                    message.reply("Ошибка! Неверная команда!").queue();
                }
                break;
            }
        }
    }
}
