package net.abdymazhit.mthd.managers;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.channels.game.GameChannel;
import net.abdymazhit.mthd.channels.game.MapChoiceChannel;
import net.abdymazhit.mthd.channels.game.PlayersChoiceChannel;
import net.abdymazhit.mthd.channels.game.PlayersPickChannel;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.enums.GameMap;
import net.abdymazhit.mthd.enums.GameState;
import net.abdymazhit.mthd.enums.Rating;
import net.abdymazhit.mthd.enums.UserRole;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Категория игры
 *
 * @version   09.10.2021
 * @author    Islam Abdymazhit
 */
public class GameCategoryManager {

    /** Игра */
    public Game game;

    /** Id категория игры */
    public String categoryId;

    /** Роль первой команды */
    public Role firstTeamRole;

    /** Роль второй команды */
    public Role secondTeamRole;

    /** Discord id игроков */
    public List<String> playersDiscordIds;

    /** Игроки первой команды */
    public List<Member> firstTeamMembers;

    /** Игроки второй команды */
    public List<Member> secondTeamMembers;

    /** Игроки */
    public List<Member> players;

    /** Канал выбора игроков на игру */
    public PlayersChoiceChannel playersChoiceChannel;

    /** Канал выбора игроков в команду */
    public PlayersPickChannel playersPickChannel;

    /** Канал выбора карты */
    public MapChoiceChannel mapChoiceChannel;

    /** Канал создания серверов */
    public GameChannel gameChannel;

    /**
     * Инициализирует категорию игры
     * @param game Игра
     */
    public GameCategoryManager(Game game) {
        game.getData();
        this.game = game;

        if(game.rating.equals(Rating.TEAM_RATING)) {
            getTeamRoles(game.firstTeam.name, game.secondTeam.name);
        } else {
            players = new ArrayList<>();
            playersDiscordIds = new ArrayList<>();
            firstTeamMembers = new ArrayList<>();
            secondTeamMembers = new ArrayList<>();

            try {
                Connection connection = MTHD.getInstance().database.getConnection();
                for(int playerId : game.playersIds) {
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "SELECT discord_id FROM users WHERE id = ?;");
                    preparedStatement.setInt(1, playerId);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    while(resultSet.next()) {
                        String discordId = resultSet.getString("discord_id");
                        playersDiscordIds.add(discordId);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        MTHD.getInstance().guild.createCategory("Game-" + game.id)
            .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .queue(category -> {
                categoryId = category.getId();

                createChatChannel();
                createFirstTeamVoiceChannel();
                createSecondTeamVoiceChannel();

                if(game.rating.equals(Rating.TEAM_RATING)) {
                    createPlayersChoiceChannel();
                } else {
                    createPlayersPickChannel();
                }
            });
    }

    /**
     * Инициализирует категорию игры
     * @param game Игра
     * @param category Категория
     */
    public GameCategoryManager(Game game, Category category) {
        game.getData();
        this.game = game;
        this.categoryId = category.getId();

        if(game.rating.equals(Rating.TEAM_RATING)) {
            getTeamRoles(game.firstTeam.name, game.secondTeam.name);
        } else {
            players = new ArrayList<>();
            playersDiscordIds = new ArrayList<>();
            firstTeamMembers = new ArrayList<>();
            secondTeamMembers = new ArrayList<>();

            try {
                Connection connection = MTHD.getInstance().database.getConnection();
                for(int playerId : game.playersIds) {
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "SELECT discord_id FROM users WHERE id = ?;");
                    preparedStatement.setInt(1, playerId);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    while(resultSet.next()) {
                        String discordId = resultSet.getString("discord_id");
                        playersDiscordIds.add(discordId);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                Connection connection = MTHD.getInstance().database.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("""
                        SELECT discord_id FROM users as u
                        INNER JOIN single_live_games_players as slgp
                        ON slgp.live_game_id = ?
                        AND u.id = slgp.player_id
                        AND slgp.team_id = 0;""");
                preparedStatement.setInt(1, game.id);
                ResultSet resultSet = preparedStatement.executeQuery();
                while(resultSet.next()) {
                    String discordId = resultSet.getString("discord_id");
                    Member member = MTHD.getInstance().guild.getMemberById(discordId);
                    firstTeamMembers.add(member);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                Connection connection = MTHD.getInstance().database.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("""
                        SELECT discord_id FROM users as u
                        INNER JOIN single_live_games_players as slgp
                        ON slgp.live_game_id = ?
                        AND u.id = slgp.player_id
                        AND slgp.team_id = 1;""");
                preparedStatement.setInt(1, game.id);
                ResultSet resultSet = preparedStatement.executeQuery();
                while(resultSet.next()) {
                    String discordId = resultSet.getString("discord_id");
                    Member member = MTHD.getInstance().guild.getMemberById(discordId);
                    secondTeamMembers.add(member);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        boolean hasChatChannel = false;
        boolean hasFirstTeamVoiceChannel = false;
        boolean hasSecondTeamVoiceChannel = false;

        for(GuildChannel channel : category.getChannels()) {
            if(channel.getName().equals("players-pick")) {
                channel.delete().queue();
            } else if(channel.getName().equals("players-choice")) {
                channel.delete().queue();
            } else if(channel.getName().equals("map-choice")) {
                channel.delete().queue();
            } else if(channel.getName().equals("game")) {
                channel.delete().queue();
            } else if(channel.getName().equals("chat")) {
                hasChatChannel = true;
            } else {
                if(game.rating.equals(Rating.TEAM_RATING)) {
                    if(channel.getName().equals(game.firstTeam.name)) {
                        hasFirstTeamVoiceChannel = true;
                    } else if(channel.getName().equals(game.secondTeam.name)) {
                        hasSecondTeamVoiceChannel = true;
                    } else {
                        channel.delete().queue();
                    }
                } else {
                    if(game.gameState.equals(GameState.PLAYERS_CHOICE)) {
                        if(channel.getName().equals("team_" + game.firstTeamCaptain.username)) {
                            channel.delete().queue();
                        } else if(channel.getName().equals("team_" + game.secondTeamCaptain.username)) {
                            channel.delete().queue();
                        } else {
                            channel.delete().queue();
                        }
                    } else {
                        if(channel.getName().equals("team_" + game.firstTeamCaptain.username)) {
                            hasFirstTeamVoiceChannel = true;
                        } else if(channel.getName().equals("team_" + game.secondTeamCaptain.username)) {
                            hasSecondTeamVoiceChannel = true;
                        } else {
                            channel.delete().queue();
                        }
                    }
                }
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
            if(game.rating.equals(Rating.TEAM_RATING)) {
                try {
                    Connection connection = MTHD.getInstance().database.getConnection();
                    PreparedStatement firstTeamStatement = connection.prepareStatement(
                            "DELETE FROM team_live_games_players WHERE team_id = ?;");
                    firstTeamStatement.setInt(1, game.firstTeam.id);
                    firstTeamStatement.executeUpdate();

                    PreparedStatement secondTeamStatement = connection.prepareStatement(
                            "DELETE FROM team_live_games_players WHERE team_id = ?;");
                    secondTeamStatement.setInt(1, game.secondTeam.id);
                    secondTeamStatement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                createPlayersChoiceChannel();
            } else {
                try {
                    Connection connection = MTHD.getInstance().database.getConnection();
                    PreparedStatement deleteStatement = connection.prepareStatement(
                            "UPDATE single_live_games_players SET team_id = null WHERE live_game_id = ?;");
                    deleteStatement.setInt(1, game.id);
                    deleteStatement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                createPlayersPickChannel();
            }
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
     * Создает канал выбора игроков в команду
     */
    private void createPlayersPickChannel() {
        setGameState(GameState.PLAYERS_CHOICE);
        playersPickChannel = new PlayersPickChannel(this);
    }

    /**
     * Удаляет канал выбора игроков на игру
     */
    private void deletePlayersChoicePickChannel() {
        if(playersChoiceChannel != null) {
            TextChannel channel = MTHD.getInstance().guild.getTextChannelById(playersChoiceChannel.channelId);
            if(channel != null) {
                channel.delete().queue();
            }
            playersChoiceChannel = null;
        } else if(playersPickChannel != null) {
            TextChannel channel = MTHD.getInstance().guild.getTextChannelById(playersPickChannel.channelId);
            if(channel != null) {
                channel.delete().queue();
            }
            playersPickChannel = null;
        }
    }

    /**
     * Создает канал выбора карты
     */
    public void createMapsChoiceChannel() {
        deletePlayersChoicePickChannel();
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
        if(category == null) return;
        if(game.rating.equals(Rating.TEAM_RATING)) {
            ChannelAction<TextChannel> createAction = category.createTextChannel("chat").setPosition(0)
                    .addPermissionOverride(firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            MTHD.getInstance().guild.retrieveMemberById(game.assistantAccount.discordId).queue(member ->
                    createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue());
        } else {
            ChannelAction<TextChannel> createAction = category.createTextChannel("chat").setPosition(0)
                    .addPermissionOverride(game.firstTeamCaptainMember, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.secondTeamCaptainMember, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));

            if(players == null) {
                players = new ArrayList<>();
            }

            for(String discordId : playersDiscordIds) {
                if(discordId != null) {
                    MTHD.getInstance().guild.retrieveMemberById(discordId).queue(players::add);
                }
            }
            for(Member member : players) {
                createAction = createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null);
            }
            ChannelAction<TextChannel> finalCreateAction = createAction;
            MTHD.getInstance().guild.retrieveMemberById(game.assistantAccount.discordId).queue(member ->
                    finalCreateAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue());
        }
    }

    /**
     * Создает голосовой канал первой команды
     */
    private void createFirstTeamVoiceChannel() {
        Category category = MTHD.getInstance().guild.getCategoryById(categoryId);
        if(category == null) return;
        if(game.rating.equals(Rating.TEAM_RATING)) {
            category.createVoiceChannel(firstTeamRole.getName()).setPosition(2)
                    .addPermissionOverride(firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();
        } else {
            ChannelAction<VoiceChannel> createAction = category.createVoiceChannel("team_" + game.firstTeamCaptain.username).setPosition(0)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(game.firstTeamCaptainMember, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.secondTeamCaptainMember, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT));

            for(Member member : firstTeamMembers) {
                createAction = createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null);
            }
            ChannelAction<VoiceChannel> finalCreateAction = createAction;
            MTHD.getInstance().guild.retrieveMemberById(game.assistantAccount.discordId).queue(member ->
                    finalCreateAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue(voiceChannel -> {
                for(Member member1 : firstTeamMembers) {
                    MTHD.getInstance().guild.moveVoiceMember(member1, voiceChannel).queue();
                }
                MTHD.getInstance().guild.moveVoiceMember(game.firstTeamCaptainMember, voiceChannel).queue();
            }));
        }
    }

    /**
     * Добавляет участника в голосовой канал первой команды
     * @param member Участник
     */
    public void addToFirstTeamVoiceChannel(Member member) {
        Category category = MTHD.getInstance().guild.getCategoryById(categoryId);
        if(category == null) return;
        for(VoiceChannel voiceChannel : category.getVoiceChannels()) {
            if(voiceChannel.getName().equals("team_" + game.firstTeamCaptain.username)) {
                voiceChannel.getManager().putPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
                MTHD.getInstance().guild.moveVoiceMember(member, voiceChannel).queue();
                break;
            }
        }
    }

    /**
     * Создает голосовой канал второй команды
     */
    private void createSecondTeamVoiceChannel() {
        Category category = MTHD.getInstance().guild.getCategoryById(categoryId);
        if(category == null) return;
        if(game.rating.equals(Rating.TEAM_RATING)) {
            category.createVoiceChannel(secondTeamRole.getName()).setPosition(3)
                    .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(secondTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(firstTeamRole, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();
        } else {
            ChannelAction<VoiceChannel> createAction = category.createVoiceChannel("team_" + game.secondTeamCaptain.username).setPosition(0)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(game.secondTeamCaptainMember, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.firstTeamCaptainMember, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT));

            for(Member member : secondTeamMembers) {
                createAction = createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null);
            }
            ChannelAction<VoiceChannel> finalCreateAction = createAction;
            MTHD.getInstance().guild.retrieveMemberById(game.assistantAccount.discordId).queue(member ->
                    finalCreateAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue(voiceChannel -> {
                        for(Member member1 : secondTeamMembers) {
                            MTHD.getInstance().guild.moveVoiceMember(member1, voiceChannel).queue();
                        }
                        MTHD.getInstance().guild.moveVoiceMember(game.firstTeamCaptainMember, voiceChannel).queue();
                    }));
        }
    }

    /**
     * Добавляет участника в голосовой канал второй команды
     * @param member Участник
     */
    public void addToSecondTeamVoiceChannel(Member member) {
        Category category = MTHD.getInstance().guild.getCategoryById(categoryId);
        if(category == null) return;
        for(VoiceChannel voiceChannel : category.getVoiceChannels()) {
            if(voiceChannel.getName().equals("team_" + game.secondTeamCaptain.username)) {
                voiceChannel.getManager().putPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
                MTHD.getInstance().guild.moveVoiceMember(member, voiceChannel).queue();
                break;
            }
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
            PreparedStatement preparedStatement;
            if(game.rating.equals(Rating.TEAM_RATING)) {
                preparedStatement = connection.prepareStatement("UPDATE team_live_games SET game_state = ? WHERE id = ?;");
            } else {
                preparedStatement = connection.prepareStatement("UPDATE single_live_games SET game_state = ? WHERE id = ?;");
            }
            preparedStatement.setInt(1, gameState.getId());
            preparedStatement.setInt(2, game.id);
            preparedStatement.executeUpdate();

            MTHD.getInstance().teamLiveGamesChannel.updateLiveGamesMessages();
            MTHD.getInstance().singleLiveGamesChannel.updateLiveGamesMessages();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Устанавливает карту игры
     * @param gameMap Карта
     */
    public void setGameMap(GameMap gameMap) {
        game.gameMap = gameMap;
        try {
            Connection connection = MTHD.getInstance().database.getConnection();
            PreparedStatement preparedStatement;
            if(game.rating.equals(Rating.TEAM_RATING)) {
                preparedStatement = connection.prepareStatement("UPDATE team_live_games SET map_name = ? WHERE id = ?;");
            } else {
                preparedStatement = connection.prepareStatement("UPDATE single_live_games SET map_name = ? WHERE id = ?;");
            }
            preparedStatement.setString(1, gameMap.getName());
            preparedStatement.setInt(2, game.id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
