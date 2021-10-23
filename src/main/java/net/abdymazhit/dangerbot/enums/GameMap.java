package net.abdymazhit.dangerbot.enums;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Представляет собой карту
 *
 * @version   23.10.2021
 * @author    Islam Abdymazhit
 */
public enum GameMap {
    ACTUON("Актуон", "Actuon", "4x2"),
    AQUARIUM("Аквариум", "Aquarium", "4x2"),
    LOCKS("Замки", "Locks", "4x2"),
    FERNIGAD("Фернигад", "Fernigad", "4x2"),
    FROKUS("Фрокус 2.0", "Frokus", "4x2"),
    JUNGLIOS("Джунглиос", "Junglios", "4x2"),
    CRIMENTIS("Криментис", "Crimentis", "4x2"),
    KRITAZ("Критаз", "Kritaz", "4x2"),
    AWAKENING("Пробуждение", "Awakening", "4x2"),
    TROSTER("Тростер", "Troster", "4x2"),
    JUNO("Юнона", "Juno", "4x2"),
    ZELNES("Зелнес", "Zelnes", "4x2"),
    REKLAS("Реклас", "Reklas", "4x2"),
    FORTIS_XL("Фортис XL", "FortisXL", "6x2"),
    CRIMENTIS_XL("Криментис XL", "CrimentisXL", "6x2"),
    ZIMPERIA_XL("Зимперия XL", "ZimperiaXL", "6x2"),
    MERBES_XL("Мэрбес", "MerbesXL", "6x2"),
    FERNIGAD_XL("Фернигад XL", "FernigadXL", "6x2");

    private final String name;
    private BufferedImage normalImage;
    private BufferedImage pickImage;
    private BufferedImage banImage;

    /**
     * Инициализирует карту
     * @param name Название карты
     * @param fileName Название файла
     * @param format Формат игры
     */
    GameMap(String name, String fileName, String format) {
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

    /**
     * Получает рандомные 5 карт 4x2 формата
     * @return Рандомные 5 карт 4x2 формата
     */
    public static List<GameMap> getRandom4x2Maps() {
        List<GameMap> maps = new ArrayList<>();
        maps.add(ACTUON);
        maps.add(AQUARIUM);
        maps.add(LOCKS);
        maps.add(FERNIGAD);
        maps.add(FROKUS);
        maps.add(JUNGLIOS);
        maps.add(CRIMENTIS);
        maps.add(KRITAZ);
        maps.add(AWAKENING);
        maps.add(TROSTER);
        maps.add(JUNO);
        maps.add(ZELNES);
        maps.add(REKLAS);

        Random random = new Random();
        while(maps.size() > 5) {
            maps.remove(random.nextInt(maps.size()));
        }

        return maps;
    }

    /**
     * Получает рандомные 3 карты 6x2 формата
     * @return Рандомные 3 карты 6x2 формата
     */
    public static List<GameMap> getRandom6x2Maps() {
        List<GameMap> maps = new ArrayList<>();
        maps.add(FORTIS_XL);
        maps.add(CRIMENTIS_XL);
        maps.add(ZIMPERIA_XL);
        maps.add(MERBES_XL);
        maps.add(FERNIGAD_XL);

        Random random = new Random();
        while(maps.size() > 3) {
            maps.remove(random.nextInt(maps.size()));
        }

        return maps;
    }
}