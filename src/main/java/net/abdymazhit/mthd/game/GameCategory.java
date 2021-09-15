package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Game;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Категория игры
 *
 * @version   15.09.2021
 * @author    Islam Abdymazhit
 */
public class GameCategory {

    /** Игра */
    public Game game;

    /** Категория игры */
    public Category category;

    /** Роль первой команды */
    public Role firstTeamRole;

    /** Роль второй команды */
    public Role secondTeamRole;

    /** Канал чата */
    private TextChannel chatChannel;

    /** Канал выбора игроков */
    public PlayersChoiceChannel playersChoiceChannel;

    /** Канал выбора карты */
    public MapChoiceChannel mapChoiceChannel;

    /** Голосовой канал первой команды */
    private VoiceChannel firstTeamVoiceChannel;

    /** Голосовой канал второй команды */
    private VoiceChannel secondTeamVoiceChannel;

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
        try {
            category = MTHD.getInstance().guild.createCategory(categoryName)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
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
        playersChoiceChannel.channel.delete().queue();
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
        mapChoiceChannel.channel.delete().queue();
        mapChoiceChannel = null;
    }

    /**
     * Создает канал чата
     */
    private void createChatChannel() {
        try {
            chatChannel = category.createTextChannel("chat").setPosition(0)
                    .addPermissionOverride(firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает голосовой канал первой команды
     */
    private void createFirstTeamVoiceChannel() {
        try {
            firstTeamVoiceChannel = category.createVoiceChannel(firstTeamRole.getName()).setPosition(2)
                    .addPermissionOverride(firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает голосовой канал второй команды
     */
    private void createSecondTeamVoiceChannel() {
        try {
            secondTeamVoiceChannel = category.createVoiceChannel(secondTeamRole.getName()).setPosition(3)
                    .addPermissionOverride(secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
