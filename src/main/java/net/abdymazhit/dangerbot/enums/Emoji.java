package net.abdymazhit.dangerbot.enums;

import net.abdymazhit.dangerbot.DangerBot;
import net.dv8tion.jda.api.entities.Emote;

import java.util.List;

/**
 * Представляет собой эмодзи
 *
 * @version   24.10.2021
 * @author    Islam Abdymazhit
 */
public enum Emoji {
    VIME_ONLINE("vime_online"),
    VIME_OFFLINE("vime_offline");

    private final String name;
    private Emote emote;

    /**
     * Инициализирует эмодзи
     * @param name Название эмодзи
     */
    Emoji(String name) {
        this.name = name;
        List<Emote> emotes = DangerBot.getInstance().guild.getEmotesByName(name, true);
        if(!emotes.isEmpty()) {
            this.emote = emotes.get(0);
        }
    }

    /**
     * Получает название эмодзи
     * @return Название эмодзи
     */
    public String getName() {
        return name;
    }

    /**
     * Получает эмодзи
     * @return эмодзи
     */
    public Emote getEmote() {
        return emote;
    }
}