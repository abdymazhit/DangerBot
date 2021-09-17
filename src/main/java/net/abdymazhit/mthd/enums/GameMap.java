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
    ACTUON(1, "Актуон", "Actuon", "4x2"),
    AQUARIUM(2, "Аквариум", "Aquarium", "4x2"),
    LOCKS(3, "Замки", "Locks", "4x2"),
    FERNIGAD(4, "Фернигад", "Fernigad", "4x2"),
    FROKUS(5, "Фрокус 2.0", "Frokus", "4x2"),
    JUNGLIOS(6, "Джунглиос", "Junglios", "4x2"),
    CRIMENTIS(7, "Криментис", "Crimentis", "4x2"),
    KRITAZ(8, "Критаз", "Kritaz", "4x2"),
    AWAKENING(9, "Пробуждение", "Awakening", "4x2"),
    TROSTER(10, "Тростер", "Troster", "4x2"),
    JUNO(11, "Юнона", "Juno", "4x2"),
    ZELNES(12, "Зелнес", "Zelnes", "4x2"),
    REKLAS(13, "Реклас", "Reklas", "4x2"),
    FORTIS_XL(14, "Фортис XL", "FortisXL", "6x2"),
    CRIMENTIS_XL(15, "Криментис XL", "CrimentisXL", "6x2"),
    ZIMPERIA_XL(16, "Зимперия XL", "ZimperiaXL", "6x2"),
    MERBES_XL(17, "Мэрбес", "MerbesXL", "6x2"),
    FERNIGAD_XL(18, "Фернигад XL", "FernigadXL", "6x2");

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
     * @param format Формат игры
     */
    GameMap(int id, String name, String fileName, String format) {
        this.id = id;
        this.name = name;
        try {
            if(format.equals("4x2")) {
                this.normalImage = ImageIO.read(Paths.get("./maps/4x2/normals/" + fileName + ".jpg").toFile());
                this.pickImage = ImageIO.read(Paths.get("./maps/4x2/picks/" + fileName + ".jpg").toFile());
                this.banImage = ImageIO.read(Paths.get("./maps/4x2/bans/" + fileName + ".jpg").toFile());
            } else if(format.equals("6x2")) {
                this.normalImage = ImageIO.read(Paths.get("./maps/6x2/normals/" + fileName + ".jpg").toFile());
                this.pickImage = ImageIO.read(Paths.get("./maps/6x2/picks/" + fileName + ".jpg").toFile());
                this.banImage = ImageIO.read(Paths.get("./maps/6x2/bans/" + fileName + ".jpg").toFile());
            }
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

    public static GameMap[] values4x2() {
        return new GameMap[] { GameMap.ACTUON, GameMap.AQUARIUM, GameMap.LOCKS, GameMap.FERNIGAD, GameMap.FROKUS,
                GameMap.JUNGLIOS, GameMap.CRIMENTIS, GameMap.KRITAZ, GameMap.AWAKENING, GameMap.TROSTER,
                GameMap.JUNO, GameMap.ZELNES, GameMap.REKLAS};
    }

    public static GameMap[] values6x2() {
        return new GameMap[] { GameMap.FORTIS_XL, GameMap.CRIMENTIS_XL, GameMap.ZIMPERIA_XL, GameMap.MERBES_XL, GameMap.FERNIGAD_XL};
    }
}