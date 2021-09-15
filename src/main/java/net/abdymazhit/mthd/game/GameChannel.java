package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Channel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.EnumSet;
import java.util.List;

/**
 * Канал игры
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class GameChannel extends Channel {

    /** Категория игры */
    private final GameCategory gameCategory;

    /**
     * Инициализирует канал игры
     * @param gameCategory Категория игры
     */
    public GameChannel(GameCategory gameCategory) {
        this.gameCategory = gameCategory;

        ChannelAction<TextChannel> createAction = createChannel(gameCategory.categoryId, "game", 2);

        List<Member> members = MTHD.getInstance().guild.retrieveMembersByIds(gameCategory.game.firstTeamStarterDiscordId,
                gameCategory.game.secondTeamStarterDiscordId).get();

        Member firstTeamStarter = members.get(0);
        Member secondTeamStarter = members.get(1);

        if(firstTeamStarter == null || secondTeamStarter == null) {
            return;
        }

        createAction = createAction.addPermissionOverride(gameCategory.firstTeamRole,
                EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
        createAction = createAction.addPermissionOverride(gameCategory.secondTeamRole,
                EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_WRITE));
        createAction = createAction.addPermissionOverride(MTHD.getInstance().guild.getPublicRole(),
                null, EnumSet.of(Permission.VIEW_CHANNEL));
        createAction.queue(textChannel -> channelId = textChannel.getId());

        sendChannelMessage();
    }

    /**
     * Отправляет сообщение канала игры
     */
    private void sendChannelMessage() {
        MTHD.getInstance().guild.retrieveMemberById(gameCategory.game.firstTeamStarterDiscordId).queue(member -> {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Инструкция для помощника");

            StringBuilder firstTeamInviteStrings = new StringBuilder();
            for(String username : gameCategory.game.firstTeamPlayers) {
                firstTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
            }

            StringBuilder secondTeamInviteStrings = new StringBuilder();
            for(String username : gameCategory.game.secondTeamPlayers) {
                secondTeamInviteStrings.append("`/game summon ").append(username).append("`\n");
            }

            embedBuilder.setDescription("У вас (" + member.getAsMention() + ") есть 5 минут для создания игры! \n\n" +
                    "Противостояние: " + gameCategory.firstTeamRole.getAsMention() + " VS " + gameCategory.secondTeamRole.getAsMention() + "\n" +
                    "Игра: BedWars Hard\n" +
                    "\n" +
                    "Формат игры: **" + gameCategory.game.format + "**\n" +
                    "Название карты: **" + gameCategory.game.gameMap.getName() + "**\n" +
                    "\n" +
                    "Настройки сервера: \n" +
                    "allow-warp: **false**\n" +
                    "kick-on-lose: **true**\n" +
                    "final-dm: **true**\n" +
                    "\n" +
                    "Команда для создания игры: `/game create`\n\n" +
                    "Команды для приглашения игроков " + gameCategory.firstTeamRole.getAsMention() + ":\n" +
                    firstTeamInviteStrings +
                    "\n" +
                    "Команды для приглашения игроков " + gameCategory.secondTeamRole.getAsMention() + ":\n" +
                    secondTeamInviteStrings
            );

            embedBuilder.setColor(0xFF58B9FF);
            embedBuilder.addField("Начать игру", "Обратите внимание, вы должны ввести команду только после начала игры в самом VimeWorld!" +
                    " Введите `!start` для начала игры", false);
            embedBuilder.addField("Отменить игру", "Если на сервер не зашли игроки введите `!cancel`", false);

            TextChannel channel = MTHD.getInstance().guild.getTextChannelById(channelId);
            if(channel != null) {
                channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> channelMessageId = message.getId());                
            }

            embedBuilder.clear();
        });
    }
}
