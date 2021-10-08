package net.abdymazhit.mthd.enums;

import java.awt.*;

/**
 * Представляет собой результат игры
 *
 * @version   08.10.2021
 * @author    Islam Abdymazhit
 */
public enum GameResult {
    WIN("W", new Color(0x17B861)),
    LOSE("L", new Color(0xC13E3E));

    String character;
    Color color;

    /**
     * Инициализирует результат игры
     * @param character Символ результата игры
     * @param color Цвет результата игры
     */
    GameResult(String character, Color color) {
        this.character = character;
        this.color = color;
    }

    /**
     * Получает символ результата игры
     * @return Символ результата игры
     */
    public String getCharacter() {
        return character;
    }

    /**
     * Получает цвет результата игры
     * @return Цвет результата игры
     */
    public Color getColor() {
        return color;
    }
}