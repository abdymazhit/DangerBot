package net.abdymazhit.mthd.utils;

import com.google.gson.JsonArray;
import net.abdymazhit.mthd.customs.Player;
import net.abdymazhit.mthd.customs.Team;
import net.abdymazhit.mthd.customs.UserAccount;
import net.abdymazhit.mthd.enums.GameResult;
import net.abdymazhit.mthd.enums.LeagueImage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;

/**
 * Представляет собой инструменты для упрощения работы
 *
 * @version   09.10.2021
 * @author    Islam Abdymazhit
 */
public class Utils {

    /** Объект, отвечает за создание embed сообщений */
    private final EmbedBuilder embedBuilder;

    /** Шрифт имени игрока */
    private Font nameFont;

    /** Шрифт цифр и остальной информации */
    private Font grayFont;

    /** Серый цвет */
    private Color grayColor;

    /**
     * Инициализирует инструменты
     */
    public Utils() {
        embedBuilder = new EmbedBuilder();
        try {
            nameFont = Font.createFont(Font.TRUETYPE_FONT, new File("./info/akzidenzgroteskpro-boldex.ttf")).deriveFont(34f);
            grayFont = Font.createFont(Font.TRUETYPE_FONT, new File("./info/SFUIDisplay-Regular.ttf")).deriveFont(23f);
            grayColor = new Color(255, 255, 255, 204);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет GET запрос по URL
     * @param url URL
     * @return Результат запроса в типе String
     */
    public String sendGetRequest(String url) {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Отправляет POST запрос по URL
     * @param url URL
     * @param jsonArray JSON массив
     * @return Результат запроса в типе String
     */
    public String sendPostRequest(String url, JsonArray jsonArray) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(jsonArray.toString(), ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Получает embed сообщение о успешной авторизации
     * @param username Имя игрока
     * @param level Уровень игрока
     * @param percent Проценты игрока
     * @param rank Ранг игрока
     * @return Embed сообщение о успешной авторизации
     */
    public MessageEmbed getAuthInfoMessageEmbed(String username, String level, String percent, String rank) {
        embedBuilder.setColor(3092790);
        embedBuilder.setTitle("Успешная авторизация!");
        String description = """
            **Ваш ник:** `%username%`
            **Уровень:** `%level% [%percent%%]`
            **Статус:** `%rank%`"""
            .replace("%username%", username.replace("_", "\\_"))
            .replace("%level%", level)
            .replace("%percent%", String.valueOf((int) (Double.parseDouble(percent) * 100)))
            .replace("%rank%", rank);
        embedBuilder.setDescription(description);
        embedBuilder.setThumbnail("http://skin.vimeworld.ru/helm/3d/" + username +".png");
        embedBuilder.setTimestamp(new Date().toInstant());

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }

    /**
     * Получает информационное embed сообщение команды
     * @param team Команда
     * @return Информационное embed сообщение команды
     */
    public MessageEmbed getTeamInfoMessageEmbed(Team team) {
        StringBuilder membersString = new StringBuilder();
        StringBuilder members2String = new StringBuilder();

        if(team.leader.isVimeOnline) {
            membersString.append("<:emote:884826184729366538> ");
        } else {
            membersString.append("<:emote:884826184641294346> ");
        }

        if(team.leader.isDiscordOnline) {
            membersString.append("<:emote:884825784857010196> ");
        } else {
            membersString.append("<:emote:884825362863910962> ");
        }
        membersString.append("`").append(team.leader.username).append("`").append("\n");

        if(team.members.size() > 7) {
            for(int i = 0; i < 7; i++) {
                UserAccount user = team.members.get(i);

                if(user.isVimeOnline) {
                    membersString.append("<:emote:884826184729366538> ");
                } else {
                    membersString.append("<:emote:884826184641294346> ");
                }

                if(user.isDiscordOnline) {
                    membersString.append("<:emote:884825784857010196> ");
                } else {
                    membersString.append("<:emote:884825362863910962> ");
                }
                membersString.append("`").append(user.username).append("`").append("\n");
            }

            for(int i = 7; i < team.members.size(); i++) {
                UserAccount user = team.members.get(i);

                if(user.isVimeOnline) {
                    members2String.append("<:emote:884826184729366538> ");
                } else {
                    members2String.append("<:emote:884826184641294346> ");
                }

                if(user.isDiscordOnline) {
                    members2String.append("<:emote:884825784857010196> ");
                } else {
                    members2String.append("<:emote:884825362863910962> ");
                }
                members2String.append("`").append(user.username).append("`").append("\n");
            }
        } else {
            for(UserAccount user : team.members) {
                if(user.isVimeOnline) {
                    membersString.append("<:emote:884826184729366538> ");
                } else {
                    membersString.append("<:emote:884826184641294346> ");
                }

                if(user.isDiscordOnline) {
                    membersString.append("<:emote:884825784857010196> ");
                } else {
                    membersString.append("<:emote:884825362863910962> ");
                }
                membersString.append("`").append(user.username).append("`").append("\n");
            }
        }

        embedBuilder.setColor(3092790);

        embedBuilder.addField("Игроки", ">>> " + membersString, false);

        if(team.members.size() > 7) {
            embedBuilder.addField("Игроки", ">>> " + members2String, false);
        }

        String rating = """
            >>> ```
            %points%
            ```"""
            .replace("%points%", String.valueOf(team.points));
        embedBuilder.addField("Рейтинг", rating, true);

        String wins = """
            >>> ```
            %wins%
            ```"""
            .replace("%wins%", String.valueOf(team.wins));
        embedBuilder.addField("Побед", wins, true);

        String games = """
            >>> ```
            %games%
            ```""".replace("%games%", String.valueOf(team.games));
        embedBuilder.addField("Всего игр", games, true);

        String name = """
            ```
            \040\040\040\040\040\040\040\040\040\040\040\040\040\040\040\040%team_name%\040\040\040\040\040\040\040\040\040\040\040\040\040\040\040\040
            ```"""
            .replace("%team_name%", team.name.replace("_", "\\_"));
        embedBuilder.setDescription(name);

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }

    /**
     * Получает изображение информации игрока
     * @param player Игрок
     * @return Изображение информации игрока
     */
    public File getPlayerInfoImage(Player player) {
        try {
            BufferedImage sourceImage = ImageIO.read(Paths.get("./info/player.jpg").toFile());
            Graphics2D graphics2D = (Graphics2D) sourceImage.getGraphics();

            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            graphics2D.setFont(nameFont);
            graphics2D.drawString(player.username, 30, 65);

            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            graphics2D.setFont(grayFont);
            graphics2D.setColor(grayColor);

            int hours;
            if(player.latestActive != null) {
                Timestamp timestamp = Timestamp.from(Instant.now());
                long milliseconds = timestamp.getTime() - player.latestActive.getTime();
                hours = (int) (milliseconds / (60 * 60 * 1000));
            } else {
                hours = 0;
            }

            graphics2D.drawString(hours + "h ago", 160, 102);
            graphics2D.drawString(String.valueOf(player.points), 30, 219);
            graphics2D.drawString(String.valueOf(player.games), 165, 219);
            graphics2D.drawString(String.valueOf(player.wins), 307, 219);
            graphics2D.drawString(String.valueOf(player.games - player.wins), 425, 219);

            if(player.games == 0) {
                graphics2D.drawString("0%", 554, 219);
            } else {
                graphics2D.drawString((player.wins * 100 / player.games) + "%", 554, 219);
            }

            int x = 30;
            if(player.lastGameResults.size() >= 5) {
                for(int i = 0; i < 5; i++) {
                    graphics2D.setColor(player.lastGameResults.get(i).getColor());
                    graphics2D.drawString(player.lastGameResults.get(i).getCharacter(), x, 335);

                    if(player.lastGameResults.get(i).equals(GameResult.WIN)) {
                        x += 30;
                    } else {
                        x += 22;
                    }
                }
            } else {
                for(int i = 0; i < player.lastGameResults.size(); i++) {
                    graphics2D.setColor(player.lastGameResults.get(i).getColor());
                    graphics2D.drawString(player.lastGameResults.get(i).getCharacter(), x, 335);

                    if(player.lastGameResults.get(i).equals(GameResult.WIN)) {
                        x += 30;
                    } else {
                        x += 22;
                    }
                }
            }

            graphics2D.drawImage(getLeagueImage(player.points).getImage(), 652, 30, null);

            // Очистка памяти
            graphics2D.dispose();

            File file = new File("./info/image.png");
            ImageIO.write(sourceImage, "png", file);

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Получает изображение лиги
     * @param points Очки
     * @return Изображение лиги
     */
    private LeagueImage getLeagueImage(int points) {
        if(points <= 800) {
            return LeagueImage.LEAGUE_1;
        } else if(points <= 950) {
            return LeagueImage.LEAGUE_2;
        } else if(points <= 1100) {
            return LeagueImage.LEAGUE_3;
        } else if(points <= 1250) {
            return LeagueImage.LEAGUE_4;
        } else if(points <= 1400) {
            return LeagueImage.LEAGUE_5;
        } else if(points <= 1550) {
            return LeagueImage.LEAGUE_6;
        } else if(points <= 1700) {
            return LeagueImage.LEAGUE_7;
        } else if(points <= 1850) {
            return LeagueImage.LEAGUE_8;
        } else if(points <= 2000) {
            return LeagueImage.LEAGUE_9;
        } else {
            return LeagueImage.LEAGUE_10;
        }
    }
}