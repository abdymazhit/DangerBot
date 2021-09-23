package net.abdymazhit.mthd.game;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;

/**
 * Категория игры
 *
 * @version   23.09.2021
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

        MTHD.getInstance().guild.createCategory("Game-" + game.id)
            .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .queue(category -> {
                categoryId = category.getId();
                getTeamRoles(game.firstTeamName, game.secondTeamName);

                createChatChannel();
                createFirstTeamVoiceChannel();
                createSecondTeamVoiceChannel();

                createPlayersChoiceChannel();
            });
    }

    /**
     * Инициализирует категорию игры
     * @param game Игра
     * @param category Категория
     */
    public GameCategory(Game game, Category category) {
        game.getData();
        this.game = game;
        this.categoryId = category.getId();

        getTeamRoles(game.firstTeamName, game.secondTeamName);

        boolean hasChatChannel = false;
        boolean hasFirstTeamVoiceChannel = false;
        boolean hasSecondTeamVoiceChannel = false;

        for(GuildChannel channel : category.getChannels()) {
            if(channel.getName().equals("players-choice")) {
                channel.delete().queue();
            } else if(channel.getName().equals("map-choice")) {
                channel.delete().queue();
            } else if(channel.getName().equals("game")) {
                channel.delete().queue();
            } else if(channel.getName().equals("chat")) {
                hasChatChannel = true;
            } else if(channel.getName().equals(game.firstTeamName)) {
                hasFirstTeamVoiceChannel = true;
            } else if(channel.getName().equals(game.secondTeamName)) {
                hasSecondTeamVoiceChannel = true;
            } else {
                channel.delete().queue();
            }
        }

        if(!hasChatChannel) {
            createChatChannel();
        }

        if(!hasFirstTeamVoiceChannel) {
            createFirstTeamVoiceChannel();
        }

        if(!hasSecondTeamVoiceChannel) {
            createSecondTeamVoiceChannel();
        }

        if(game.gameState.equals(GameState.PLAYERS_CHOICE)) {
            try {
                Connection connection = MTHD.getInstance().database.getConnection();
                PreparedStatement firstTeamStatement = connection.prepareStatement(
                    "DELETE FROM live_games_players WHERE team_id = ?;");
                firstTeamStatement.setInt(1, game.firstTeamId);
                firstTeamStatement.executeUpdate();

                PreparedStatement secondTeamStatement = connection.prepareStatement(
                    "DELETE FROM live_games_players WHERE team_id = ?;");
                secondTeamStatement.setInt(1, game.secondTeamId);
                secondTeamStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            createPlayersChoiceChannel();
        } else if(game.gameState.equals(GameState.MAP_CHOICE)) {
            createMapsChoiceChannel();
        } else if(game.gameState.equals(GameState.GAME_CREATION) || game.gameState.equals(GameState.GAME)) {
            createGameChannel();
        }
    }

    /**
     * Создает каналы категории
     * @param firstTeamName Название первой команды
     * @param secondTeamName Название второй команды
     */
    private void getTeamRoles(String firstTeamName, String secondTeamName) {
        Role firstTeamRole = null;
        List<Role> firstTeamRoles = MTHD.getInstance().guild.getRolesByName(firstTeamName, true);
        if(!firstTeamRoles.isEmpty()) {
            firstTeamRole = firstTeamRoles.get(0);
        }

        Role secondTeamRole = null;
        List<Role> secondTeamRoles = MTHD.getInstance().guild.getRolesByName(secondTeamName, true);
        if(!secondTeamRoles.isEmpty()) {
            secondTeamRole = secondTeamRoles.get(0);
        }

        if(firstTeamRole == null || secondTeamRole == null) return;

        this.firstTeamRole = firstTeamRole;
        this.secondTeamRole = secondTeamRole;
    }

    /**
     * Создает канал выбора игроков на игру
     */
    private void createPlayersChoiceChannel() {
        setGameState(GameState.PLAYERS_CHOICE);
        playersChoiceChannel = new PlayersChoiceChannel(this);
    }

    /**
     * Удаляет канал выбора игроков на игру
     */
    private void deletePlayersChoiceChannel() {
        if(playersChoiceChannel != null) {
            TextChannel channel = MTHD.getInstance().guild.getTextChannelById(playersChoiceChannel.channelId);
            if(channel != null) {
                channel.delete().queue();
            }
            playersChoiceChannel = null;
        }
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
        if(mapChoiceChannel != null) {
            if(mapChoiceChannel.timer != null) {
                mapChoiceChannel.timer.cancel();
                mapChoiceChannel.timer = null;
            }

            TextChannel channel = MTHD.getInstance().guild.getTextChannelById(mapChoiceChannel.channelId);
            if(channel != null) {
                channel.delete().queue();
            }
            mapChoiceChannel = null;
        }
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

    /**
     * Устанавливает стадию игры
     * @param gameState Стадия игры
     */
    public void setGameState(GameState gameState) {
        game.gameState = gameState;
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE live_games SET game_state = ? WHERE id = ?;");
            preparedStatement.setInt(1, gameState.getId());
            preparedStatement.setInt(2, game.id);
            preparedStatement.executeUpdate();
            MTHD.getInstance().liveGamesChannel.updateLiveGamesMessages();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Устанавливает стадию игры
     * @param gameMap Выбранная карта
     */
    public void setGameMap(GameMap gameMap) {
        game.gameMap = gameMap;
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE live_games SET map_name = ? WHERE id = ?;");
            preparedStatement.setString(1, gameMap.getName());
            preparedStatement.setInt(2, game.id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
