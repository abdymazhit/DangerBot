package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Game;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.EnumSet;
import java.util.List;

/**
 * Категория игры
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class GameCategory {

    /** Игра */
    public Game game;

    /** Id категория игры */
    public String categoryId;

    /** Роль первой команды */
    public Role firstTeamRole;

    /** Роль второй команды */
    public Role secondTeamRole;

    /** Канал выбора игроков */
    public PlayersChoiceChannel playersChoiceChannel;

    /** Канал выбора карты */
    public MapChoiceChannel mapChoiceChannel;

    /** Канал создания серверов */
    public GameChannel gameChannel;

    /**
     * Инициализирует категорию игры
     * @param game Игра
     */
    public GameCategory(Game game) {
        game.getData();
        this.game = game;
        createCategory("Game-" + game.id);
        createChannels(game.firstTeamName, game.secondTeamName);
        createPlayersChoiceChannel();
    }

    /**
     * Создает категорию игры
     * @param categoryName Название категории
     */
    private void createCategory(String categoryName) {
        MTHD.getInstance().guild.createCategory(categoryName)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(category -> categoryId = category.getId());
    }

    /**
     * Создает каналы категории
     * @param firstTeamName Название первой команды
     * @param secondTeamName Название второй команды
     */
    private void createChannels(String firstTeamName, String secondTeamName) {
        Role firstTeamRole = null;
        List<Role> firstTeamRoleCategories = MTHD.getInstance().guild.getRolesByName(firstTeamName, true);
        if(!firstTeamRoleCategories.isEmpty()) {
            firstTeamRole = firstTeamRoleCategories.get(0);
        }

        Role secondTeamRole = null;
        List<Role> secondTeamRoleCategories = MTHD.getInstance().guild.getRolesByName(secondTeamName, true);
        if(!secondTeamRoleCategories.isEmpty()) {
            secondTeamRole = secondTeamRoleCategories.get(0);
        }

        if(firstTeamRole == null || secondTeamRole == null) return;

        this.firstTeamRole = firstTeamRole;
        this.secondTeamRole = secondTeamRole;

        createChatChannel();
        createFirstTeamVoiceChannel();
        createSecondTeamVoiceChannel();
    }

    /**
     * Создает канал выбора игроков на игру
     */
    private void createPlayersChoiceChannel() {
        playersChoiceChannel = new PlayersChoiceChannel(this);
    }

    /**
     * Удаляет канал выбора игроков на игру
     */
    private void deletePlayersChoiceChannel() {
        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(playersChoiceChannel.channelId);
        if(channel != null) {
            channel.delete().queue();
        }
        playersChoiceChannel = null;
    }

    /**
     * Создает канал выбора карты
     */
    public void createMapsChoiceChannel() {
        deletePlayersChoiceChannel();
        mapChoiceChannel = new MapChoiceChannel(this);
    }

    /**
     * Удаляет канал выбора карты
     */
    private void deleteMapChoiceChannel() {
        TextChannel channel = MTHD.getInstance().guild.getTextChannelById(mapChoiceChannel.channelId);
        if(channel != null) {
            channel.delete().queue();
        }
        mapChoiceChannel = null;
    }

    /**
     * Создает канал игры
     */
    public void createGameChannel() {
        deleteMapChoiceChannel();
        gameChannel = new GameChannel(this);
    }

    /**
     * Создает канал чата
     */
    private void createChatChannel() {
        Category category = MTHD.getInstance().guild.getCategoryById(categoryId);
        if(category != null) {
            category.createTextChannel("chat").setPosition(0)
                    .addPermissionOverride(firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();
        }
    }

    /**
     * Создает голосовой канал первой команды
     */
    private void createFirstTeamVoiceChannel() {
        Category category = MTHD.getInstance().guild.getCategoryById(categoryId);
        if(category != null) {
            category.createVoiceChannel(firstTeamRole.getName()).setPosition(2)
                    .addPermissionOverride(firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();
        }
    }

    /**
     * Создает голосовой канал второй команды
     */
    private void createSecondTeamVoiceChannel() {
        Category category = MTHD.getInstance().guild.getCategoryById(categoryId);
        if(category != null) {
            category.createVoiceChannel(secondTeamRole.getName()).setPosition(3)
                    .addPermissionOverride(secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();
        }
    }
}
