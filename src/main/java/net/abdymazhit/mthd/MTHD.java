package net.abdymazhit.mthd;

import com.google.gson.Gson;
import net.abdymazhit.mthd.channels.*;
import net.abdymazhit.mthd.customs.Config;
import net.abdymazhit.mthd.database.Database;
import net.abdymazhit.mthd.game.GameManager;
import net.abdymazhit.mthd.game.LiveGamesManager;
import net.abdymazhit.mthd.listeners.commands.AuthCommandListener;
import net.abdymazhit.mthd.listeners.commands.LeaveCommandListener;
import net.abdymazhit.mthd.listeners.MessageReceivedListener;
import net.abdymazhit.mthd.listeners.commands.FindGameCommandListener;
import net.abdymazhit.mthd.listeners.commands.StaffCommandListener;
import net.abdymazhit.mthd.listeners.commands.admin.AdminCommandsListener;
import net.abdymazhit.mthd.listeners.commands.game.GameCommandsListener;
import net.abdymazhit.mthd.listeners.commands.game.MapChoiceCommandListener;
import net.abdymazhit.mthd.listeners.commands.game.PlayersChoiceCommandListener;
import net.abdymazhit.mthd.listeners.commands.team.TeamCommandsListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.Compression;
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
 * @version   21.09.2021
 * @author    Islam Abdymazhit
 */
public class MTHD {

    /** Объект главного класса */
    private static MTHD instance;

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

    /** Канал команды */
    public final TeamsChannel teamsChannel;

    /** Канал активных игр */
    public final LiveGamesChannel liveGamesChannel;

    /** Канал поиска игры */
    public final FindGameChannel findGameChannel;

    /** Канал моя команда */
    public final MyTeamChannel myTeamChannel;

    /** Менеджер активных игр */
    public final LiveGamesManager liveGamesManager;

    /** Менеджер игры */
    public final GameManager gameManager;

    /** Инструменты для упрощения работы */
    public final Utils utils;

    /**
     * Создает бота
     * @throws IOException Ошибка чтения файла конфигурации
     * @throws LoginException Ошибка входа
     * @throws InterruptedException Ошибка работы Discord API
     */
    public static void main(String[] args) throws IOException, LoginException, InterruptedException {
        new MTHD();
    }

    /**
     * Инициализирует бота
     * @throws IOException Ошибка чтения файла конфигурации
     * @throws LoginException Ошибка входа
     * @throws InterruptedException Ошибка работы Discord API
     */
    public MTHD() throws IOException, LoginException, InterruptedException {
        instance = this;
        config = getConfig();

        JDABuilder builder = JDABuilder.createDefault(config.token);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES);
        builder.enableCache(CacheFlag.CLIENT_STATUS);
        builder.disableCache(CacheFlag.VOICE_STATE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setCompression(Compression.ZLIB);
        JDA jda = builder.build().awaitReady();
        guild = jda.getGuilds().get(0);

        System.out.println("Удачное подключение к Discord API!");

        database = new Database();
        authChannel = new AuthChannel();
        adminChannel = new AdminChannel();
        staffChannel = new StaffChannel();
        teamsChannel = new TeamsChannel();
        liveGamesChannel = new LiveGamesChannel();
        findGameChannel = new FindGameChannel();
        myTeamChannel = new MyTeamChannel();
        utils = new Utils();
        liveGamesManager = new LiveGamesManager();
        gameManager = new GameManager();

//        Обновить команды, только при изменении/добавлении команды
        updateCommands();

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

        commandsAction.queue();
    }

    /**
     * Добавляет слушатели событий
     * @param jda Объект для работы с Discord API
     */
    private void addEventListeners(JDA jda) {
        jda.addEventListener(new AuthCommandListener());
        jda.addEventListener(new LeaveCommandListener());
        jda.addEventListener(new MessageReceivedListener());

        jda.addEventListener(new AdminCommandsListener());
        jda.addEventListener(new TeamCommandsListener());
        jda.addEventListener(new FindGameCommandListener());
        jda.addEventListener(new StaffCommandListener());

        jda.addEventListener(new PlayersChoiceCommandListener());
        jda.addEventListener(new MapChoiceCommandListener());
        jda.addEventListener(new GameCommandsListener());
    }

    /**
     * Получает объект главного класса
     * @return Объект главного класса
     */
    public static MTHD getInstance() {
        return instance;
    }
}
