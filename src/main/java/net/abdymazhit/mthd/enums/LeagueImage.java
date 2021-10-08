package net.abdymazhit.mthd.enums;

import net.abdymazhit.mthd.utils.BufferedImageTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Представляет собой изображения лиг
 *
 * @version   08.10.2021
 * @author    Islam Abdymazhit
 */
public enum LeagueImage {
    LEAGUE_1("1"),
    LEAGUE_2("2"),
    LEAGUE_3("3"),
    LEAGUE_4("4"),
    LEAGUE_5("5"),
    LEAGUE_6("6"),
    LEAGUE_7("7"),
    LEAGUE_8("8"),
    LEAGUE_9("9"),
    LEAGUE_10("10");

    private BufferedImage image;

    /**
     * Инициализирует изображение карты
     * @param level Уровень лиги
     */
    LeagueImage(String level) {
        BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
        try(InputStream file = new FileInputStream("./leagues/"  + level + ".svg")) {
            TranscoderInput transIn = new TranscoderInput(file);
            transcoder.transcode(transIn, null);
            image = transcoder.getBufferedImage();
        } catch (IOException | TranscoderException io) {
            io.printStackTrace();
        }
    }

    /**
     * Получает изображение лиги
     * @return Изображение лиги
     */
    public BufferedImage getImage() {
        return image;
    }
}
