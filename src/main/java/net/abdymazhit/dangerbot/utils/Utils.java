package net.abdymazhit.dangerbot.utils;

import com.google.gson.JsonArray;
import net.abdymazhit.dangerbot.customs.info.PlayerInfo;
import net.abdymazhit.dangerbot.customs.info.TeamInfo;
import net.abdymazhit.dangerbot.customs.UserAccount;
import net.abdymazhit.dangerbot.enums.GameResult;
import net.abdymazhit.dangerbot.enums.LeagueImage;
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
 * @version   24.10.2021
 * @author    Islam Abdymazhit
 */
public class Utils {

    /** Объект, отвечает за создание embed сообщений */
    private final EmbedBuilder embedBuilder;

    /** Шрифт имени игрока */
    private Font nameFont;

    /** Цвет шрифта имени игрока */
    private Color nameColor;

    /** Шрифт цифр и остальной информации */
    private Font redFont;

    /** Красный цвет */
    private Color redColor;

    /**
     * Инициализирует инструменты
     */
    public Utils() {
        embedBuilder = new EmbedBuilder();
        try {
            nameFont = Font.createFont(Font.TRUETYPE_FONT, new File("./info/Britanica-Expanded-Black.ttf")).deriveFont(34f);
            nameColor = new Color(23, 23, 23, 255);
            redFont = Font.createFont(Font.TRUETYPE_FONT, new File("./info/SFUIDisplay-Regular.ttf")).deriveFont(23f);
            redColor = new Color(218, 0, 55, 255);
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
                .replace("%username%", username)
                .replace("%level%", level)
                .replace("%percent%", String.valueOf((int) (Double.parseDouble(percent) * 100)))
                .replace("%rank%", rank);
        embedBuilder.setDescription(description);
        embedBuilder.setThumbnail("http://skin.vimeworld.ru/helm/3d/%username%.png"
                .replace("%username%", username));
        embedBuilder.setTimestamp(new Date().toInstant());

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }

    /**
     * Получает информационное embed сообщение команды
     * @param teamInfo Команда
     * @return Информационное embed сообщение команды
     */
    public MessageEmbed getTeamInfoMessageEmbed(TeamInfo teamInfo) {
        StringBuilder membersString = new StringBuilder();
        StringBuilder members2String = new StringBuilder();

        if(teamInfo.leader.isVimeOnline) {
            membersString.append("<:emote:884826184729366538> ");
        } else {
            membersString.append("<:emote:884826184641294346> ");
        }
        membersString.append("`").append(teamInfo.leader.username).append("`").append("\n");

        if(teamInfo.members.size() > 7) {
            for(int i = 0; i < 7; i++) {
                UserAccount user = teamInfo.members.get(i);

                if(user.isVimeOnline) {
                    membersString.append("<:emote:884826184729366538> ");
                } else {
                    membersString.append("<:emote:884826184641294346> ");
                }
                membersString.append("`").append(user.username).append("`").append("\n");
            }

            for(int i = 7; i < teamInfo.members.size(); i++) {
                UserAccount user = teamInfo.members.get(i);

                if(user.isVimeOnline) {
                    members2String.append("<:emote:884826184729366538> ");
                } else {
                    members2String.append("<:emote:884826184641294346> ");
                }
                members2String.append("`").append(user.username).append("`").append("\n");
            }
        } else {
            for(UserAccount user : teamInfo.members) {
                if(user.isVimeOnline) {
                    membersString.append("<:emote:884826184729366538> ");
                } else {
                    membersString.append("<:emote:884826184641294346> ");
                }
                membersString.append("`").append(user.username).append("`").append("\n");
            }
        }

        embedBuilder.setColor(3092790);

        embedBuilder.addField("Игроки", ">>> " + membersString, false);

        if(teamInfo.members.size() > 7) {
            embedBuilder.addField("Игроки", ">>> " + members2String, false);
        }

        String rating = """
                >>> ```
                %points%
                ```"""
                .replace("%points%", String.valueOf(teamInfo.points));
        embedBuilder.addField("Рейтинг", rating, true);

        String wins = """
                >>> ```
                %wins%
                ```"""
                .replace("%wins%", String.valueOf(teamInfo.wins));
        embedBuilder.addField("Побед", wins, true);

        String games = """
                >>> ```
                %games%
                ```""".replace("%games%", String.valueOf(teamInfo.games));
        embedBuilder.addField("Всего игр", games, true);

        String name = """
                ```
                \040\040\040\040\040\040\040\040\040\040\040\040\040\040\040\040%team_name%\040\040\040\040\040\040\040\040\040\040\040\040\040\040\040\040
                ```"""
                .replace("%team_name%", teamInfo.name);
        embedBuilder.setDescription(name);

        MessageEmbed messageEmbed = embedBuilder.build();
        embedBuilder.clear();

        return messageEmbed;
    }

    /**
     * Получает изображение информации игрока
     * @param playerInfo Игрок
     * @return Изображение информации игрока
     */
    public File getPlayerInfoImage(PlayerInfo playerInfo) {
        try {
            BufferedImage sourceImage = ImageIO.read(Paths.get("./info/player.jpg").toFile());
            Graphics2D graphics2D = (Graphics2D) sourceImage.getGraphics();

            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            graphics2D.setFont(nameFont);
            graphics2D.setColor(nameColor);
            graphics2D.drawString(playerInfo.username, 90, 65);

            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            graphics2D.setFont(redFont);
            graphics2D.setColor(redColor);

            int hours;
            if(playerInfo.latestActive != null) {
                Timestamp timestamp = Timestamp.from(Instant.now());
                long milliseconds = timestamp.getTime() - playerInfo.latestActive.getTime();
                hours = (int) (milliseconds / (60 * 60 * 1000));
            } else {
                hours = 0;
            }
            graphics2D.drawString(hours + "h ago", 223, 99);

            graphics2D.drawString(String.valueOf(playerInfo.points), 90, 215);
            graphics2D.drawString(String.valueOf(playerInfo.games), 225, 215);
            graphics2D.drawString(String.valueOf(playerInfo.wins), 367, 215);
            graphics2D.drawString(String.valueOf(playerInfo.games - playerInfo.wins), 485, 215);
            if(playerInfo.games == 0) {
                graphics2D.drawString("0%", 614, 215);
            } else {
                graphics2D.drawString((playerInfo.wins * 100 / playerInfo.games) + "%", 614, 215);
            }

            int x = 90;
            if(playerInfo.lastGameResults.size() >= 5) {
                for(int i = 0; i < 5; i++) {
                    graphics2D.setColor(playerInfo.lastGameResults.get(i).getColor());
                    graphics2D.drawString(playerInfo.lastGameResults.get(i).getCharacter(), x, 330);

                    if(playerInfo.lastGameResults.get(i).equals(GameResult.WIN)) {
                        x += 30;
                    } else {
                        x += 22;
                    }
                }
            } else {
                for(int i = 0; i < playerInfo.lastGameResults.size(); i++) {
                    graphics2D.setColor(playerInfo.lastGameResults.get(i).getColor());
                    graphics2D.drawString(playerInfo.lastGameResults.get(i).getCharacter(), x, 330);

                    if(playerInfo.lastGameResults.get(i).equals(GameResult.WIN)) {
                        x += 30;
                    } else {
                        x += 22;
                    }
                }
            }

            graphics2D.drawImage(getLeagueImage(playerInfo.points).getImage(), 652, 30, null);

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