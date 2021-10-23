package net.abdymazhit.mthd.managers;

import net.abdymazhit.mthd.MTHD;
import net.abdymazhit.mthd.channels.game.*;
import net.abdymazhit.mthd.customs.Game;
import net.abdymazhit.mthd.customs.UserAccount;
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
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class GameCategoryManager {

    /** Игра */
    public Game game;

    /** Категория игры */
    public Category category;

    /** Канал готовности к игре */
    public ReadyChannel readyChannel;

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
        this.game = game;

        if(game.rating.equals(Rating.TEAM_RATING)) {
            getTeamRoles(game.firstTeamInfo.name, game.secondTeamInfo.name);
        }

        MTHD.getInstance().guild.createCategory("Game-" + game.id)
                .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(category -> {
                    this.category = category;

                    if(game.rating.equals(Rating.TEAM_RATING)) {
                        createPlayersChoiceChannel();
                    } else {
                        game.firstTeamInfo.members = new ArrayList<>();
                        game.firstTeamInfo.members.add(new UserAccount(game.firstTeamInfo.captain.username));

                        game.secondTeamInfo.members = new ArrayList<>();
                        game.secondTeamInfo.members.add(new UserAccount(game.secondTeamInfo.captain.username));

                        createReadyChannel();
                    }
                });
    }

    /**
     * Инициализирует категорию игры
     * @param game Игра
     * @param category Категория
     */
    public GameCategoryManager(Game game, Category category) {
        this.game = game;
        this.category = category;

        if(game.rating.equals(Rating.TEAM_RATING)) {
            getTeamRoles(game.firstTeamInfo.name, game.secondTeamInfo.name);
        } else {
            game.firstTeamInfo.members = new ArrayList<>();
            game.firstTeamInfo.members.add(new UserAccount(game.firstTeamInfo.captain.username));

            game.secondTeamInfo.members = new ArrayList<>();
            game.secondTeamInfo.members.add(new UserAccount(game.secondTeamInfo.captain.username));

            try {
                Connection connection = MTHD.getInstance().database.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("""
                        SELECT username FROM users as u
                        INNER JOIN single_live_games_players as slgp
                        ON slgp.live_game_id = ?
                        AND u.id = slgp.player_id
                        AND slgp.team_id = 0;""");
                preparedStatement.setInt(1, game.id);
                ResultSet resultSet = preparedStatement.executeQuery();
                while(resultSet.next()) {
                    String username = resultSet.getString("username");
                    game.firstTeamInfo.members.add(new UserAccount(username));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                Connection connection = MTHD.getInstance().database.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("""
                        SELECT username FROM users as u
                        INNER JOIN single_live_games_players as slgp
                        ON slgp.live_game_id = ?
                        AND u.id = slgp.player_id
                        AND slgp.team_id = 1;""");
                preparedStatement.setInt(1, game.id);
                ResultSet resultSet = preparedStatement.executeQuery();
                while(resultSet.next()) {
                    String username = resultSet.getString("username");
                    game.secondTeamInfo.members.add(new UserAccount(username));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        boolean hasChatChannel = false;
        boolean hasFirstTeamVoiceChannel = false;
        boolean hasSecondTeamVoiceChannel = false;

        for(GuildChannel channel : category.getChannels()) {
            if(channel.getName().equals("ready")) {
                channel.delete().queue();
            } else if(channel.getName().equals("players-pick")) {
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
                    if(channel.getName().equals(game.firstTeamInfo.name)) {
                        hasFirstTeamVoiceChannel = true;
                    } else if(channel.getName().equals(game.secondTeamInfo.name)) {
                        hasSecondTeamVoiceChannel = true;
                    } else {
                        channel.delete().queue();
                    }
                } else {
                    if(game.gameState.equals(GameState.PLAYERS_CHOICE)) {
                        if(channel.getName().equals("team_" + game.firstTeamInfo.captain.username)) {
                            channel.delete().queue();
                        } else if(channel.getName().equals("team_" + game.secondTeamInfo.captain.username)) {
                            channel.delete().queue();
                        } else {
                            channel.delete().queue();
                        }
                    } else {
                        if(channel.getName().equals("team_" + game.firstTeamInfo.captain.username)) {
                            hasFirstTeamVoiceChannel = true;
                        } else if(channel.getName().equals("team_" + game.secondTeamInfo.captain.username)) {
                            hasSecondTeamVoiceChannel = true;
                        } else {
                            channel.delete().queue();
                        }
                    }
                }
            }
        }

        if(game.gameState.equals(GameState.READY)) {
            createReadyChannel();
        } else if(game.gameState.equals(GameState.PLAYERS_CHOICE)) {
            if(game.rating.equals(Rating.TEAM_RATING)) {
                try {
                    Connection connection = MTHD.getInstance().database.getConnection();
                    PreparedStatement firstTeamStatement = connection.prepareStatement(
                            "DELETE FROM team_live_games_players WHERE team_id = ?;");
                    firstTeamStatement.setInt(1, game.firstTeamInfo.id);
                    firstTeamStatement.executeUpdate();

                    PreparedStatement secondTeamStatement = connection.prepareStatement(
                            "DELETE FROM team_live_games_players WHERE team_id = ?;");
                    secondTeamStatement.setInt(1, game.secondTeamInfo.id);
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

                game.firstTeamInfo.members = new ArrayList<>();
                game.secondTeamInfo.members = new ArrayList<>();

                createPlayersPickChannel();
            }
        } else if(game.gameState.equals(GameState.MAP_CHOICE)) {
            createMapsChoiceChannel();
            if(!hasChatChannel) {
                createChatChannel();
            }

            if(!hasFirstTeamVoiceChannel) {
                createFirstTeamVoiceChannel();
            }

            if(!hasSecondTeamVoiceChannel) {
                createSecondTeamVoiceChannel();
            }
        } else if(game.gameState.equals(GameState.GAME_CREATION) || game.gameState.equals(GameState.GAME)) {
            createGameChannel();
            if(!hasChatChannel) {
                createChatChannel();
            }

            if(!hasFirstTeamVoiceChannel) {
                createFirstTeamVoiceChannel();
            }

            if(!hasSecondTeamVoiceChannel) {
                createSecondTeamVoiceChannel();
            }
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

        game.firstTeamInfo.role = firstTeamRole;
        game.secondTeamInfo.role = secondTeamRole;
    }

    /**
     * Создает канал готовности к игре
     */
    private void createReadyChannel() {
        setGameState(GameState.READY);
        readyChannel = new ReadyChannel(this);
    }

    /**
     * Удаляет канал ready
     */
    private void deleteReadyChannel() {
        if(readyChannel != null) {
            readyChannel.channel.delete().queue();
            readyChannel = null;
        }
    }

    /**
     * Создает канал выбора игроков на игру
     */
    public void createPlayersChoiceChannel() {
        boolean hasChatChannel = false;
        boolean hasFirstTeamVoiceChannel = false;
        boolean hasSecondTeamVoiceChannel = false;

        for(GuildChannel channel : category.getChannels()) {
            if(channel.getName().equals("chat")) {
                hasChatChannel = true;
            } else {
                if(game.rating.equals(Rating.TEAM_RATING)) {
                    if(channel.getName().equals(game.firstTeamInfo.name)) {
                        hasFirstTeamVoiceChannel = true;
                    } else if(channel.getName().equals(game.secondTeamInfo.name)) {
                        hasSecondTeamVoiceChannel = true;
                    }
                } else {
                    if(channel.getName().equals("team_" + game.firstTeamInfo.captain.username)) {
                        hasFirstTeamVoiceChannel = true;
                    } else if(channel.getName().equals("team_" + game.secondTeamInfo.captain.username)) {
                        hasSecondTeamVoiceChannel = true;
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

        deleteReadyChannel();
        setGameState(GameState.PLAYERS_CHOICE);
        playersChoiceChannel = new PlayersChoiceChannel(this);
    }

    /**
     * Создает канал выбора игроков в команду
     */
    public void createPlayersPickChannel() {
        boolean hasChatChannel = false;
        boolean hasFirstTeamVoiceChannel = false;
        boolean hasSecondTeamVoiceChannel = false;

        for(GuildChannel channel : category.getChannels()) {
            if(channel.getName().equals("chat")) {
                hasChatChannel = true;
            } else {
                if(game.rating.equals(Rating.TEAM_RATING)) {
                    if(channel.getName().equals(game.firstTeamInfo.name)) {
                        hasFirstTeamVoiceChannel = true;
                    } else if(channel.getName().equals(game.secondTeamInfo.name)) {
                        hasSecondTeamVoiceChannel = true;
                    }
                } else {
                    if(channel.getName().equals("team_" + game.firstTeamInfo.captain.username)) {
                        hasFirstTeamVoiceChannel = true;
                    } else if(channel.getName().equals("team_" + game.secondTeamInfo.captain.username)) {
                        hasSecondTeamVoiceChannel = true;
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

        deleteReadyChannel();
        setGameState(GameState.PLAYERS_CHOICE);
        playersPickChannel = new PlayersPickChannel(this);
    }

    /**
     * Удаляет канал выбора игроков на игру
     */
    private void deletePlayersChoicePickChannel() {
        if(playersChoiceChannel != null) {
            playersChoiceChannel.channel.delete().queue();
            playersChoiceChannel = null;
        } else if(playersPickChannel != null) {
            playersPickChannel.channel.delete().queue();
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

            mapChoiceChannel.channel.delete().queue();
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
        if(game.rating.equals(Rating.TEAM_RATING)) {
            ChannelAction<TextChannel> createAction = category.createTextChannel("chat").setPosition(0)
                    .addPermissionOverride(game.firstTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.secondTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));
            Member member = MTHD.getInstance().guild.getMemberById(game.assistantAccount.discordId);
            if(member != null) {
                createAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
            }
        } else {
            ChannelAction<TextChannel> createAction = category.createTextChannel("chat").setPosition(0)
                    .addPermissionOverride(game.firstTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.secondTeamInfo.captain.member, EnumSet.of(Permission.MESSAGE_WRITE, Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));

            for(UserAccount userAccount : game.playersAccounts) {
                createAction = createAction.addPermissionOverride(userAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), null);
            }
            ChannelAction<TextChannel> finalCreateAction = createAction;

            Member member = MTHD.getInstance().guild.getMemberById(game.assistantAccount.discordId);
            if(member != null) {
                finalCreateAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
            }
        }
    }

    /**
     * Создает голосовой канал первой команды
     */
    private void createFirstTeamVoiceChannel() {
        if(game.rating.equals(Rating.TEAM_RATING)) {
            category.createVoiceChannel(game.firstTeamInfo.role.getName()).setPosition(2)
                    .addPermissionOverride(game.firstTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.secondTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();
        } else {
            ChannelAction<VoiceChannel> createAction = category.createVoiceChannel("team_" + game.firstTeamInfo.captain.username).setPosition(0)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(game.firstTeamInfo.captain.member, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.secondTeamInfo.captain.member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT));

            for(UserAccount userAccount : game.firstTeamInfo.members) {
                if(userAccount.member != null) {
                    createAction = createAction.addPermissionOverride(userAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), null);
                }
            }
            ChannelAction<VoiceChannel> finalCreateAction = createAction;

            Member member = MTHD.getInstance().guild.getMemberById(game.assistantAccount.discordId);
            if(member != null) {
                finalCreateAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue(voiceChannel -> {
                    for (UserAccount userAccount : game.firstTeamInfo.members) {
                        if (userAccount.member.getVoiceState() != null) {
                            if (userAccount.member.getVoiceState().inVoiceChannel()) {
                                MTHD.getInstance().guild.moveVoiceMember(userAccount.member, voiceChannel).queue();
                            }
                        }
                    }

                    if (game.firstTeamInfo.captain.member.getVoiceState() != null) {
                        if (game.firstTeamInfo.captain.member.getVoiceState().inVoiceChannel()) {
                            MTHD.getInstance().guild.moveVoiceMember(game.firstTeamInfo.captain.member, voiceChannel).queue();
                        }
                    }
                });
            }
        }
    }

    /**
     * Добавляет участника в голосовой канал первой команды
     * @param member Участник
     */
    public void addToFirstTeamVoiceChannel(Member member) {
        for(VoiceChannel voiceChannel : category.getVoiceChannels()) {
            if(voiceChannel.getName().equals("team_" + game.firstTeamInfo.captain.username)) {
                voiceChannel.getManager().putPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
                if(member.getVoiceState() != null) {
                    if(member.getVoiceState().inVoiceChannel()) {
                        MTHD.getInstance().guild.moveVoiceMember(member, voiceChannel).queue();
                    }
                }
                break;
            }
        }
    }

    /**
     * Создает голосовой канал второй команды
     */
    private void createSecondTeamVoiceChannel() {
        if(game.rating.equals(Rating.TEAM_RATING)) {
            category.createVoiceChannel(game.secondTeamInfo.role.getName()).setPosition(3)
                    .addPermissionOverride(UserRole.ASSISTANT.getRole(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.secondTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.firstTeamInfo.role, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT))
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();
        } else {
            ChannelAction<VoiceChannel> createAction = category.createVoiceChannel("team_" + game.secondTeamInfo.captain.username).setPosition(0)
                    .addPermissionOverride(MTHD.getInstance().guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(game.secondTeamInfo.captain.member, EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .addPermissionOverride(game.firstTeamInfo.captain.member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT));

            for(UserAccount userAccount : game.secondTeamInfo.members) {
                if(userAccount.member != null) {
                    createAction = createAction.addPermissionOverride(userAccount.member, EnumSet.of(Permission.VIEW_CHANNEL), null);
                }
            }
            ChannelAction<VoiceChannel> finalCreateAction = createAction;

            Member member = MTHD.getInstance().guild.getMemberById(game.assistantAccount.discordId);
            if(member != null) {
                finalCreateAction.addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue(voiceChannel -> {
                    for(UserAccount userAccount : game.secondTeamInfo.members) {
                        if(userAccount.member.getVoiceState() != null) {
                            if(userAccount.member.getVoiceState().inVoiceChannel()) {
                                MTHD.getInstance().guild.moveVoiceMember(userAccount.member, voiceChannel).queue();
                            }
                        }
                    }
                    if(game.secondTeamInfo.captain.member.getVoiceState() != null) {
                        if(game.secondTeamInfo.captain.member.getVoiceState().inVoiceChannel()) {
                            MTHD.getInstance().guild.moveVoiceMember(game.secondTeamInfo.captain.member, voiceChannel).queue();
                        }
                    }
                });
            }
        }
    }

    /**
     * Добавляет участника в голосовой канал второй команды
     * @param member Участник
     */
    public void addToSecondTeamVoiceChannel(Member member) {
        for(VoiceChannel voiceChannel : category.getVoiceChannels()) {
            if(voiceChannel.getName().equals("team_" + game.secondTeamInfo.captain.username)) {
                voiceChannel.getManager().putPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
                if(member.getVoiceState() != null) {
                    if(member.getVoiceState().inVoiceChannel()) {
                        MTHD.getInstance().guild.moveVoiceMember(member, voiceChannel).queue();
                    }
                }
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
