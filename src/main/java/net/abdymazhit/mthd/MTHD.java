package net.abdymazhit.mthd;

import com.google.gson.Gson;
import net.abdymazhit.mthd.customs.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
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
 * @version   05.09.2021
 * @author    Islam Abdymazhit
 */
public class MTHD {

    /** Объект главного класса */
    private static MTHD instance;

    /** Файл конфигурации */
    public Config config;

    /** Текущий сервер */
    public final Guild guild;

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
}
