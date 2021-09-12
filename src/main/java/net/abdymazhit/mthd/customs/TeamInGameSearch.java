package net.abdymazhit.mthd.customs;

/**
 * Представляет собой команду в поиске игры
 *
 * @version   12.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamInGameSearch {

    /** Название команды */
    public String teamName;

    /** Формат игры */
    public String format;

    /** Имя начавшего */
    public String starterUsername;

    /**
     * Инициализирует команду в поиске игры
     * @param teamName Название команды
     * @param format Формат игры
     * @param starterUsername Имя начавшего
     */
    public TeamInGameSearch(String teamName, String format, String starterUsername) {
        this.teamName = teamName;
        this.format = format;
        this.starterUsername = starterUsername;
    }
}
