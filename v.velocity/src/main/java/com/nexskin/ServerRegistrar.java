package com.nexskin.velocity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ServerRegistrar {

    private final NexSkinVelocity plugin;
    private final ConfigManager config;

    public ServerRegistrar(NexSkinVelocity plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public String registerAndGetKey() {
        return register();
    }

    public String register() {
        HttpURLConnection conn = null;
        try {
            int core = 4;  // Velocity = 4
            String serverVersion = "Velocity";
            String pluginVersion = "0.1.0-beta";
            String name = "Velocity Proxy";
            int online = plugin.getServer().getPlayerCount();

            String existingKey = config.getServerKey();
            boolean hasKey = existingKey != null && !existingKey.isEmpty();

            // Логируем только при первой регистрации (без ключа)
            if (!hasKey) {
                plugin.getLogger().info("Регистрация: core=" + core
                        + ", server=" + serverVersion
                        + ", plugin=" + pluginVersion + ", key=нет");
            }

            String apiUrl = config.getApiUrl() + "register_server.php";
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "NexSkin-Velocity/1.0");

            // HMAC-заголовки (только если есть server_key)
            if (hasKey) {
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                String signature = HmacUtil.sha256(existingKey + timestamp, existingKey);

                conn.setRequestProperty("X-Server-Key", existingKey);
                conn.setRequestProperty("X-Timestamp", timestamp);
                conn.setRequestProperty("X-Signature", signature);
            }

            // Тело запроса — без server_key (он в заголовках)
            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append("core=").append(core)
                    .append("&server_version=").append(URLEncoder.encode(serverVersion, "UTF-8"))
                    .append("&plugin_version=").append(URLEncoder.encode(pluginVersion, "UTF-8"))
                    .append("&name=").append(URLEncoder.encode(name, "UTF-8"))
                    .append("&online=").append(online);

            String data = dataBuilder.toString();

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                plugin.getLogger().warn("Регистрация: HTTP " + code);
                return null;
            }

            String json = readResponse(conn);
            JsonObject resp = JsonParser.parseString(json).getAsJsonObject();

            if (resp.has("blocked") && !resp.get("blocked").isJsonNull()
                    && resp.get("blocked").getAsBoolean()) {
                plugin.getLogger().error("Сервер заблокирован: " +
                        (resp.has("reason") && !resp.get("reason").isJsonNull()
                                ? resp.get("reason").getAsString() : "?"));
                return null;
            }

            String serverKey = null;
            if (resp.has("server_key") && !resp.get("server_key").isJsonNull()) {
                serverKey = resp.get("server_key").getAsString();

                if (!hasKey) {
                    config.setServerKey(serverKey);
                    plugin.getLogger().info("server-key сохранён в data.yml");
                }
            } else if (!hasKey) {
                plugin.getLogger().warn("Сайт вернул null server_key");
            }

            if (resp.has("platform_raw") && !resp.get("platform_raw").isJsonNull()) {
                config.setPlatformRaw(resp.get("platform_raw").getAsInt());
            }

            if (resp.has("random_id") && !resp.get("random_id").isJsonNull()) {
                config.setRandomId(resp.get("random_id").getAsInt());
            }

            return serverKey;

        } catch (Exception e) {
            plugin.getLogger().warn("Ошибка регистрации: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
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