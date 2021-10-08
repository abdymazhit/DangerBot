package net.abdymazhit.mthd.utils;

import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import java.awt.image.BufferedImage;

/**
 * Утилита для работы с SVG изображениями
 *
 * @version   08.10.2021
 * @author    Islam Abdymazhit
 */
public class BufferedImageTranscoder extends ImageTranscoder {

    /** Изображение */
    private BufferedImage bufferedImage = null;

    /**
     * Создает изображение
     * @param width Ширина
     * @param height Высота
     * @return Изображение
     */
    @Override
    public BufferedImage createImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Записывает изображение
     * @param bufferedImage Изображение
     * @param transcoderOutput Местоположение записи
     */
    @Override
    public void writeImage(BufferedImage bufferedImage, TranscoderOutput transcoderOutput) {
        this.bufferedImage = bufferedImage;
    }

    /**
     * Получает изображение
     * @return Изображение
     */
    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }
}