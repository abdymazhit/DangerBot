package net.abdymazhit.mthd;

import com.google.gson.Gson;
import net.abdymazhit.mthd.customs.Config;

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

    /**
     * Создает бота
     * @throws IOException Ошибка чтения файла конфигурации
     */
    public static void main(String[] args) throws IOException {
        new MTHD();
    }

    /**
     * Инициализирует бота
     * @throws IOException Ошибка чтения файла конфигурации
     */
    public MTHD() throws IOException {
        instance = this;
        config = getConfig();
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
