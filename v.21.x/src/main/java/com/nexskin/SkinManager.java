package com.nexskin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SkinManager {

    private final NexSkin plugin;

    private static final String MINESKIN_API =
            "https://api.mineskin.org/generate/url";

    public SkinManager(NexSkin plugin) {
        this.plugin = plugin;
    }

    public SkinData getSkinFromSite(String playerName) {
        SkinData cached = plugin.getSkinCache().get(playerName, 3600);
        if (cached != null) return cached;

        String serverKey = plugin.getConfigManager().getServerKey();
        if (serverKey == null || serverKey.isEmpty()) {
            plugin.getLogger().warning("server-key не установлен");
            return null;
        }

        HttpURLConnection conn = null;
        try {
            String url = plugin.getApiUrl() + "get_skin.php?name="
                    + URLEncoder.encode(playerName, "UTF-8")
                    + "&server_key=" + URLEncoder.encode(serverKey, "UTF-8");

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "NexSkin/1.0");

            int code = conn.getResponseCode();
            if (code == 204) return null;  // скина нет
            if (code != 200) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().warning("Сайт вернул код: " + code);
                }
                return null;
            }

            String json = readResponse(conn).trim();
            if (json.isEmpty() || !json.startsWith("{")) return null;

            JsonObject data;
            try {
                data = JsonParser.parseString(json).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                plugin.getLogger().warning("Ошибка парсинга JSON от сайта: " + e.getMessage());
                return null;
            }

            if (data.has("error")) return null;

            if (data.has("value") && data.has("signature")) {
                String value = data.get("value").getAsString();
                String signature = data.get("signature").getAsString();
                if (value.isEmpty() || signature.isEmpty()) return null;

                SkinData result = new SkinData(playerName, value, signature);
                plugin.getSkinCache().put(playerName, value, signature);
                return result;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка получения скина с сайта: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    public SkinData getSkinFromMojang(String playerName) {
        HttpURLConnection conn = null;
        try {
            URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/"
                    + URLEncoder.encode(playerName, "UTF-8"));
            conn = (HttpURLConnection) uuidUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "NexSkin/1.0");

            int code = conn.getResponseCode();
            if (code == 429) {
                plugin.getLogger().warning("Mojang rate limit (429) — пропускаем " + playerName);
                return null;
            }
            if (code != 200) return null;

            String uuidJson = readResponse(conn);
            conn.disconnect();

            String uuid = JsonParser.parseString(uuidJson).getAsJsonObject()
                    .get("id").getAsString();

            URL profileUrl = new URL(
                    "https://sessionserver.mojang.com/session/minecraft/profile/"
                            + uuid + "?unsigned=false");
            conn = (HttpURLConnection) profileUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "NexSkin/1.0");

            if (conn.getResponseCode() != 200) return null;

            String profileJson = readResponse(conn);
            JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
            JsonArray properties = profile.getAsJsonArray("properties");

            for (JsonElement prop : properties) {
                JsonObject property = prop.getAsJsonObject();
                if ("textures".equals(property.get("name").getAsString())) {
                    String value = property.get("value").getAsString();
                    String signature = property.has("signature")
                            ? property.get("signature").getAsString() : "";
                    if (signature.isEmpty()) return null;
                    return new SkinData(profile.get("name").getAsString(), value, signature);
                }
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("Ошибка Mojang API: " + e.getMessage());
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    public SkinData generateFromUrl(String playerName, String skinUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(MINESKIN_API);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "NexSkin/1.0");

            JsonObject body = new JsonObject();
            body.addProperty("url", skinUrl);
            body.addProperty("name", playerName);
            body.addProperty("visibility", 1);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                plugin.getLogger().warning("MineSkin вернул код: " + code);
                return null;
            }

            String json = readResponse(conn);
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();

            if (data.has("data")) {
                JsonObject dataObj = data.getAsJsonObject("data");
                if (dataObj.has("texture")) {
                    JsonObject texture = dataObj.getAsJsonObject("texture");
                    String value = texture.get("value").getAsString();
                    String signature = texture.get("signature").getAsString();
                    if (!value.isEmpty() && !signature.isEmpty()) {
                        return new SkinData(playerName, value, signature);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка MineSkin API: " + e.getMessage());
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
        private final String playerName;
        private final String value;
        private final String signature;

        public SkinData(String playerName, String value, String signature) {
            this.playerName = playerName;
            this.value = value;
            this.signature = signature;
        }

        public String getPlayerName() { return playerName; }
        public String getValue() { return value; }
        public String getSignature() { return signature; }
    }
}