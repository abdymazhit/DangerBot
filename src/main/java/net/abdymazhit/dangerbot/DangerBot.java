package net.abdymazhit.dangerbot;

import com.google.gson.Gson;
import net.abdymazhit.dangerbot.channels.*;
import net.abdymazhit.dangerbot.channels.single.PlayersChannel;
import net.abdymazhit.dangerbot.channels.single.SingleFindGameChannel;
import net.abdymazhit.dangerbot.channels.single.SingleLiveGamesChannel;
import net.abdymazhit.dangerbot.channels.team.MyTeamChannel;
import net.abdymazhit.dangerbot.channels.team.TeamFindGameChannel;
import net.abdymazhit.dangerbot.channels.team.TeamLiveGamesChannel;
import net.abdymazhit.dangerbot.channels.team.TeamsChannel;
import net.abdymazhit.dangerbot.customs.Config;
import net.abdymazhit.dangerbot.database.Database;
import net.abdymazhit.dangerbot.listeners.MessageReceivedListener;
import net.abdymazhit.dangerbot.listeners.commands.*;
import net.abdymazhit.dangerbot.listeners.commands.admin.AdminCommandsListener;
import net.abdymazhit.dangerbot.listeners.commands.game.*;
import net.abdymazhit.dangerbot.listeners.commands.single.SingleCommandsListener;
import net.abdymazhit.dangerbot.listeners.commands.single.SingleFindGameCommandListener;
import net.abdymazhit.dangerbot.listeners.commands.team.TeamCommandsListener;
import net.abdymazhit.dangerbot.listeners.commands.team.TeamFindGameCommandListener;
import net.abdymazhit.dangerbot.managers.GameManager;
import net.abdymazhit.dangerbot.managers.LiveGamesManager;
import net.abdymazhit.dangerbot.managers.LiveStreamsManager;
import net.abdymazhit.dangerbot.utils.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;

/**
 * ?????????????? ??????????, ???????????????? ???? ?????????????????????????? ????????
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class DangerBot {

    /** ???????????? ???????????????? ???????????? */
    private static DangerBot instance;

    /** ???????? ???????????????????????? */
    public Config config;

    /** ?????????????? ???????????? */
    public final Guild guild;

    /** ???????? ???????????? */
    public final Database database;

    /** ?????????? ?????????????????????? */
    public final AuthChannel authChannel;

    /** ?????????? ?????????????????????????? */
    public final AdminChannel adminChannel;

    /** ?????????? ?????????????????? */
    public final StaffChannel staffChannel;

    /** ?????????? ???????????????????? */
    public final StreamsChannel streamsChannel;

    /** ?????????? ???????????????? ???????????????????? */
    public final ActiveBroadcastsChannel activeBroadcastsChannel;

    /** ?????????? ?????????????? */
    public final TeamsChannel teamsChannel;

    /** ?????????? ???????????????? ?????? */
    public final TeamLiveGamesChannel teamLiveGamesChannel;

    /** ?????????? ???????????? ???????? */
    public final TeamFindGameChannel teamFindGameChannel;

    /** ?????????? ?????? ?????????????? */
    public final MyTeamChannel myTeamChannel;

    /** ?????????? ?????????????? */
    public final PlayersChannel playersChannel;

    /** ?????????? ???????????????? ?????? */
    public final SingleLiveGamesChannel singleLiveGamesChannel;

    /** ?????????? ???????????? ???????? */
    public final SingleFindGameChannel singleFindGameChannel;

    /** ?????????????????????? ?????? ?????????????????? ???????????? */
    public final Utils utils;

    /** ???????????????? ?????????? */
    public final RoleManager roleManager;

    /** ???????????????? ???????????????? ?????? */
    public final LiveGamesManager liveGamesManager;

    /** ???????????????? ???????????????? ???????????????????? */
    public final LiveStreamsManager liveStreamsManager;

    /** ???????????????? ???????? */
    public final GameManager gameManager;

    /**
     * ?????????????? ????????
     * @throws IOException ???????????? ???????????? ?????????? ????????????????????????
     * @throws LoginException ???????????? ??????????
     * @throws InterruptedException ???????????? ???????????? Discord API
     */
    public static void main(String[] args) throws IOException, LoginException, InterruptedException {
        new DangerBot();
    }

    /**
     * ???????????????????????????? ????????
     * @throws IOException ???????????? ???????????? ?????????? ????????????????????????
     * @throws LoginException ???????????? ??????????
     * @throws InterruptedException ???????????? ???????????? Discord API
     */
    public DangerBot() throws IOException, LoginException, InterruptedException {
        instance = this;
        config = getConfig();

        JDABuilder builder = JDABuilder.createDefault(config.token);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES);
        builder.enableCache(CacheFlag.CLIENT_STATUS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setCompression(Compression.ZLIB);
        JDA jda = builder.build().awaitReady();
        guild = jda.getGuilds().get(0);

        System.out.println("?????????????? ?????????????????????? ?? Discord API!");

        database = new Database();
        authChannel = new AuthChannel();
        adminChannel = new AdminChannel();
        staffChannel = new StaffChannel();
        streamsChannel = new StreamsChannel();
        activeBroadcastsChannel = new ActiveBroadcastsChannel();
        teamsChannel = new TeamsChannel();
        teamLiveGamesChannel = new TeamLiveGamesChannel();
        teamFindGameChannel = new TeamFindGameChannel();
        myTeamChannel = new MyTeamChannel();
        playersChannel = new PlayersChannel();
        singleLiveGamesChannel = new SingleLiveGamesChannel();
        singleFindGameChannel = new SingleFindGameChannel();
        utils = new Utils();
        roleManager = new RoleManager();
        liveGamesManager = new LiveGamesManager();
        liveStreamsManager = new LiveStreamsManager();
        gameManager = new GameManager();

//        ???????????????? ??????????????, ???????????? ?????? ??????????????????/???????????????????? ??????????????
//        updateCommands();

        addEventListeners(jda);
    }

    /**
     * ???????????????? ???????? ????????????????????????
     * @return ???????? ????????????????????????
     * @throws IOException ???????????? ???????????? ?????????? ????????????????????????
     */
    private Config getConfig() throws IOException {
        Gson gson = new Gson();
        File configFile = new File("config.json");

        Config config;
        if(!configFile.exists()) {
            config = new Config();
            Writer writer = Files.newBufferedWriter(configFile.toPath());
            gson.toJson(config, writer);
            writer.close();
            System.exit(0);
        } else {
            Reader reader = Files.newBufferedReader(configFile.toPath());
            config = gson.fromJson(reader, Config.class);
            reader.close();
        }

        return config;
    }

    /**
     * ?????????????????? ??????????????
     */
    private void updateCommands() {
        CommandListUpdateAction commandsAction = guild.updateCommands();

        commandsAction = commandsAction.addCommands(new CommandData("auth", "??????????????????????")
                .addOption(OptionType.STRING, "token", "?????????? ??????????????????????", true));

        commandsAction = commandsAction.addCommands(new CommandData("leave", "?????????? ?? ????????????????"));

        commandsAction = commandsAction.addCommands(new CommandData("ready", "???????????????????? ?? ????????"));

        commandsAction.queue();
    }

    /**
     * ?????????????????? ?????????????????? ??????????????
     * @param jda ???????????? ?????? ???????????? ?? Discord API
     */
    private void addEventListeners(JDA jda) {
        jda.addEventListener(new AuthCommandListener());
        jda.addEventListener(new LeaveCommandListener());
        jda.addEventListener(new ReadyCommandListener());
        jda.addEventListener(new MessageReceivedListener());

        jda.addEventListener(new TeamCommandsListener());
        jda.addEventListener(new SingleCommandsListener());

        jda.addEventListener(new TeamFindGameCommandListener());
        jda.addEventListener(new SingleFindGameCommandListener());

        jda.addEventListener(new AdminCommandsListener());
        jda.addEventListener(new StaffCommandListener());
        jda.addEventListener(new StreamAddCommandListener());

        jda.addEventListener(new ReadyCommandsListener());
        jda.addEventListener(new PlayersPickCommandListener());
        jda.addEventListener(new PlayersChoiceCommandListener());
        jda.addEventListener(new MapChoiceCommandListener());
        jda.addEventListener(new GameCommandsListener());
    }

    /**
     * ???????????????? ???????????? ???????????????? ????????????
     * @return ???????????? ???????????????? ????????????
     */
    public static DangerBot getInstance() {
        return instance;
    }
}
