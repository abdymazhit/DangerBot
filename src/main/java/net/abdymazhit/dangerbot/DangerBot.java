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
 * Главный класс, отвечает за инициализацию бота
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public class DangerBot {

    /** Объект главного класса */
    private static DangerBot instance;

    /** Файл конфигурации */
    public Config config;

    /** Текущий сервер */
    public final Guild guild;

    /** База данных */
    public final Database database;

    /** Канал авторизации */
    public final AuthChannel authChannel;

    /** Канал администрации */
    public final AdminChannel adminChannel;

    /** Канал персонала */
    public final StaffChannel staffChannel;

    /** Канал трансляции */
    public final StreamsChannel streamsChannel;

    /** Канал активных трансляции */
    public final ActiveBroadcastsChannel activeBroadcastsChannel;

    /** Канал команды */
    public final TeamsChannel teamsChannel;

    /** Канал активных игр */
    public final TeamLiveGamesChannel teamLiveGamesChannel;

    /** Канал поиска игры */
    public final TeamFindGameChannel teamFindGameChannel;

    /** Канал моя команда */
    public final MyTeamChannel myTeamChannel;

    /** Канал игроков */
    public final PlayersChannel playersChannel;

    /** Канал активных игр */
    public final SingleLiveGamesChannel singleLiveGamesChannel;

    /** Канал поиска игры */
    public final SingleFindGameChannel singleFindGameChannel;

    /** Инструменты для упрощения работы */
    public final Utils utils;

    /** Менеджер ролей */
    public final RoleManager roleManager;

    /** Менеджер активных игр */
    public final LiveGamesManager liveGamesManager;

    /** Менеджер активных трансляции */
    public final LiveStreamsManager liveStreamsManager;

    /** Менеджер игры */
    public final GameManager gameManager;

    /**
     * Создает бота
     * @throws IOException Ошибка чтения файла конфигурации
     * @throws LoginException Ошибка входа
     * @throws InterruptedException Ошибка работы Discord API
     */
    public static void main(String[] args) throws IOException, LoginException, InterruptedException {
        new DangerBot();
    }

    /**
     * Инициализирует бота
     * @throws IOException Ошибка чтения файла конфигурации
     * @throws LoginException Ошибка входа
     * @throws InterruptedException Ошибка работы Discord API
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

        System.out.println("Удачное подключение к Discord API!");

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

//        Обновить команды, только при изменении/добавлении команды
//        updateCommands();

        addEventListeners(jda);
    }

    /**
     * Получает файл конфигурации
     * @return Файл конфигурации
     * @throws IOException Ошибка чтения файла конфигурации
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
     * Обновляет команды
     */
    private void updateCommands() {
        CommandListUpdateAction commandsAction = guild.updateCommands();

        commandsAction = commandsAction.addCommands(new CommandData("auth", "Авторизация")
                .addOption(OptionType.STRING, "token", "Токен авторизации", true));

        commandsAction = commandsAction.addCommands(new CommandData("leave", "Выход с аккаунта"));

        commandsAction = commandsAction.addCommands(new CommandData("ready", "Готовность к игре"));

        commandsAction.queue();
    }

    /**
     * Добавляет слушатели событий
     * @param jda Объект для работы с Discord API
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
     * Получает объект главного класса
     * @return Объект главного класса
     */
    public static DangerBot getInstance() {
        return instance;
    }
}
