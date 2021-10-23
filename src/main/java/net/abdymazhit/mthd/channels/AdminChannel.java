package net.abdymazhit.mthd.channels;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.EnumSet;
import java.util.List;

/**
 * Канал администрации
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class AdminChannel extends Channel {

    /**
     * Инициализирует канал администрации
     */
    public AdminChannel() {
        List<Category> categories = MTHD.getInstance().guild.getCategoriesByName("Staff", true);
        if(categories.isEmpty()) {
            throw new IllegalArgumentException("Критическая ошибка! Категория Staff не существует!");
        }

        Category category = categories.get(0);

        for(TextChannel textChannel : category.getTextChannels()) {
            if(textChannel.getName().equalsIgnoreCase("admin")) {
                textChannel.delete().queue();
            }
        }

        category.createTextChannel("admin").setPosition(0)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(textChannel -> {
                    channel = textChannel;
                    sendChannelMessage();
                });
    }

    /**
     * Отправляет сообщение о доступных командах для администрации
     */
    private void sendChannelMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Команды администратора");
        embedBuilder.setColor(3092790);
        embedBuilder.setDescription("""
                Создание команды
                `!adminteam create <TEAM_NAME> <LEADER_NAME>`
                
                Удаление команды
                `!adminteam disband <TEAM_NAME>`
                
                Добавление участника в команду
                `!adminteam add <TEAM_NAME> <MEMBER_NAME>`

                Удаление участника из команды
                `!adminteam delete <TEAM_NAME> <MEMBER_NAME>`

                Передача прав лидера команды
                `!adminteam transfer <TEAM_NAME> <FROM_NAME> <TO_NAME>`

                Переименование команды
                `!adminteam rename <TEAM_NAME> <TO_NAME>`
            
                Добавить игрока в Single Rating
                `!adminsingle add <PLAYER_NAME>`
            
                Удалить игрока из Single Rating
                `!adminsingle delete <PLAYER_NAME>`
            
                Заблокировать игрока
                `!ban <PLAYER_NAME> <TIME>m/h/d`
            
                Посмотреть информацию о помощниках
                `!staff`
                
                Добавить ютубера
                `!youtube add <PLAYER_NAME> <CHANNEL_LINK>`
                
                Удалить ютубера
                `!youtube delete <PLAYER_NAME>`
                
                Удалить трансляцию ютубера
                `!stream delete <PLAYER_NAME>`""");
        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessage = message);
        embedBuilder.clear();
    }
}
