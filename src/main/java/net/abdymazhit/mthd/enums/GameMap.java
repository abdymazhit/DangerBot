package net.abdymazhit.mthd.enums;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Представляет собой карту
 *
 * @version   17.09.2021
 * @author    Islam Abdymazhit
 */
public enum GameMap {
    ACTUON(1, "Актуон", "Actuon"),
    AQUARIUM(2, "Аквариум", "Aquarium"),
    LOCKS(3, "Замки", "Locks"),
    FERNIGAD(4, "Фернигад", "Fernigad"),
    FROKUS(5, "Фрокус 2.0", "Frokus"),
    JUNGLIOS(6, "Джунглиос", "Junglios"),
    CRIMENTIS(7, "Криментис", "Crimentis"),
    KRITAZ(8, "Критаз", "Kritaz"),
    AWAKENING(9, "Пробуждение", "Awakening"),
    TROSTER(10, "Тростер", "Troster"),
    JUNO(11, "Юнона", "Juno"),
    ZELNES(12, "Зелнес", "Zelnes"),
    REKLAS(13, "Реклас", "Reklas");

    private final int id;
    private final String name;
    private BufferedImage normalImage;
    private BufferedImage pickImage;
    private BufferedImage banImage;

    /**
     * Инициализирует карту
     * @param id Id карты
     * @param name Название карты
     * @param fileName Название файла
     */
    GameMap(int id, String name, String fileName) {
        this.id = id;
        this.name = name;
        try {
            this.normalImage = ImageIO.read(Paths.get("./maps/normals/" + fileName + ".jpg").toFile());
            this.pickImage = ImageIO.read(Paths.get("./maps/picks/" + fileName + ".jpg").toFile());
            this.banImage = ImageIO.read(Paths.get("./maps/bans/" + fileName + ".jpg").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает id карты
     * @return Id карты
     */
    public int getId() {
        return id;
    }

    /**
     * Получает название карты
     * @return Название карты
     */
    public String getName() {
        return name;
    }

    /**
     * Получает обычное изображение карты
     * @return Обычное изображение карты
     */
    public BufferedImage getNormalImage() {
        return normalImage;
    }

    /**
     * Получает изображение пика карты
     * @return Изображение пика карты
     */
    public BufferedImage getPickImage() {
        return pickImage;
    }

    /**
     * Получает изображение бана карты
     * @return Изображение бана карты
     */
    public BufferedImage getBanImage() {
        return banImage;
    }
}