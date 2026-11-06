package com.nexskin.velocity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SkinFetcher {

    private final NexSkinVelocity plugin;
    private final ConfigManager config;

    public SkinFetcher(NexSkinVelocity plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public SkinData fetch(String username) {
        String serverKey = config.getServerKey();
        if (serverKey == null || serverKey.isEmpty()) {
            return null;
        }

        HttpURLConnection conn = null;
        try {
            String url = config.getApiUrl() + "get_skin.php?name="
                    + URLEncoder.encode(username, "UTF-8");

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "NexSkin-Velocity/1.0");

            // HMAC-заголовки
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String signature = HmacUtil.sha256(serverKey + timestamp, serverKey);

            conn.setRequestProperty("X-Server-Key", serverKey);
            conn.setRequestProperty("X-Timestamp", timestamp);
            conn.setRequestProperty("X-Signature", signature);

            int code = conn.getResponseCode();
            if (code == 204) return null;
            if (code == 401) {
                plugin.getLogger().warn("get_skin: HMAC отклонён (401)");
                return null;
            }
            if (code != 200) return null;

            String json = readResponse(conn).trim();
            if (json.isEmpty() || !json.startsWith("{")) return null;

            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            if (data.has("error")) return null;

            if (data.has("value") && data.has("signature")) {
                String value = data.get("value").getAsString();
                String sig = data.get("signature").getAsString();
                if (value.isEmpty() || sig.isEmpty()) return null;

                return new SkinData(value, sig);
            }
        } catch (Exception e) {
            if (config.isDebug()) {
                plugin.getLogger().warn("Ошибка получения скина: " + e.getMessage());
            }
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

    public static class SkinData {
        private final String value;
        private final String signature;

        public SkinData(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }

        public String value() { return value; }
        public String signature() { return signature; }
    }
}