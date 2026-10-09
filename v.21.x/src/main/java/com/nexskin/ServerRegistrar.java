package com.nexskin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;

public class ServerRegistrar {

    private final NexSkin plugin;

    public ServerRegistrar(NexSkin plugin) {
        this.plugin = plugin;
    }

    public String registerAndGetKey() {
        return register();
    }

    public String register() {
        HttpURLConnection conn = null;
        try {
            int core = detectCore();
            String serverVersion = getServerVersion();
            String pluginVersion = plugin.getDescription().getVersion();
            String name = getMotd();
            int online = Bukkit.getOnlinePlayers().size();

            plugin.getLogger().info("Регистрация: core=" + core
                    + ", server=" + serverVersion
                    + ", plugin=" + pluginVersion);

            String apiUrl = plugin.getApiUrl() + "register_server.php";
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "NexSkin/1.0");

            String data = "core=" + core
                    + "&server_version=" + URLEncoder.encode(serverVersion, "UTF-8")
                    + "&plugin_version=" + URLEncoder.encode(pluginVersion, "UTF-8")
                    + "&name=" + URLEncoder.encode(name, "UTF-8")
                    + "&online=" + online;

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                plugin.getLogger().warning("Регистрация: HTTP " + code);
                return null;
            }

            String json = readResponse(conn);
            JsonObject resp = JsonParser.parseString(json).getAsJsonObject();

            if (resp.has("blocked") && !resp.get("blocked").isJsonNull()
                    && resp.get("blocked").getAsBoolean()) {
                plugin.getLogger().severe("Сервер заблокирован: " +
                        (resp.has("reason") && !resp.get("reason").isJsonNull()
                                ? resp.get("reason").getAsString() : "?"));
                return null;
            }

            String serverKey = null;
            if (resp.has("server_key") && !resp.get("server_key").isJsonNull()) {
                serverKey = resp.get("server_key").getAsString();
                plugin.getConfigManager().setServerKey(serverKey);
                plugin.getLogger().info("server-key сохранён в data.yml");
            } else {
                plugin.getLogger().warning("Сайт вернул null server_key");
            }

            if (resp.has("platform_raw") && !resp.get("platform_raw").isJsonNull()) {
                plugin.getConfigManager().setPlatformRaw(resp.get("platform_raw").getAsInt());
            }

            if (resp.has("random_id") && !resp.get("random_id").isJsonNull()) {
                plugin.getConfigManager().setRandomId(resp.get("random_id").getAsInt());
            }

            return serverKey;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка регистрации", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private int detectCore() {
        String serverName = Bukkit.getName();
        if (serverName.equalsIgnoreCase("Purpur")) return 2;
        if (serverName.equalsIgnoreCase("Pufferfish")) return 3;
        return 1;
    }

    private String getServerVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    private String getMotd() {
        try {
            Properties props = new Properties();
            File propsFile = new File("server.properties");
            if (propsFile.exists()) {
                try (FileReader fr = new FileReader(propsFile)) {
                    props.load(fr);
                }
                return props.getProperty("motd", "NexSkin Server");
            }
        } catch (Exception ignored) {}
        return "NexSkin Server";
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
