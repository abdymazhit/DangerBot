package net.abdymazhit.mthd.customs;

/**
 * Представляет собой команду в поиске игры
 *
 * @version   13.09.2021
 * @author    Islam Abdymazhit
 */
public class TeamInGameSearch {

    /** Id команды */
    public int id;

    /** Формат игры */
    public String format;

    /** Id начавшего поиск игры */
    public int starterId;

    /**
     * Инициализирует команду в поиске игры
     * @param id Id команды
     * @param format Формат игры
     * @param starterId Id начавшего поиск игры
     */
    public TeamInGameSearch(int id, String format, int starterId) {
        this.id = id;
        this.format = format;
        this.starterId = starterId;
    }
}
