package com.nexskin;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ServerRegistrar {

    private final NexSkin plugin;
    private static final String API = "https://nex-skin.blockchain-core-group.com/api/register_server.php";

    public ServerRegistrar(NexSkin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        try {
            // Читаем server.properties
            Properties props = new Properties();
            File propsFile = new File("server.properties");
            if (propsFile.exists()) {
                try (FileReader fr = new FileReader(propsFile)) {
                    props.load(fr);
                }
            }

            String motd = props.getProperty("motd", "NexSkin Server");
            // MOTD может быть в unicode — декодируем
            motd = decodeUnicode(motd);

            String version = Bukkit.getBukkitVersion();
            int online = plugin.getServer().getOnlinePlayers().size();

            URL url = new URL(API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "NexSkin/1.0");

            String data = "version=" + java.net.URLEncoder.encode(version, "UTF-8")
                    + "&name=" + java.net.URLEncoder.encode(motd, "UTF-8")
                    + "&online=" + online;

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (sb.toString().contains("\"blocked\":true")) {
                    plugin.getLogger().severe("Этот сервер заблокирован!");
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                } else {
                    plugin.getLogger().info("Сервер зарегистрирован на NexSkin");
                }
            } else {
                plugin.getLogger().warning("Регистрация сервера: HTTP " + code);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось зарегистрировать сервер: " + e.getMessage());
        }
    }

    private String decodeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == 'u') {
                if (i + 6 <= input.length()) {
                    try {
                        int code = Integer.parseInt(input.substring(i + 2, i + 6), 16);
                        sb.append((char) code);
                        i += 6;
                        continue;
                    } catch (NumberFormatException ignored) {}
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}