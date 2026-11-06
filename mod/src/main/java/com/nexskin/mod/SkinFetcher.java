package com.nexskin.mod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SkinFetcher {
    private static final String API = "https://nex-skin.blockchain-core-group.com/api/get_skin_public.php?name=";
    private static final Map<String, SkinData> CACHE = new HashMap<>();
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 минут
    private static final Map<String, Long> CACHE_TIME = new HashMap<>();

    public static SkinData fetch(String username) {
        // Кэш
        if (CACHE.containsKey(username)) {
            long time = CACHE_TIME.getOrDefault(username, 0L);
            if (System.currentTimeMillis() - time < CACHE_TTL) {
                return CACHE.get(username);
            }
        }

        try {
            String url = API + URLEncoder.encode(username, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "NexSkin-Mod/1.0");

            if (conn.getResponseCode() != 200) return null;

            String json = readResponse(conn);
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();

            SkinData skin = new SkinData(
                    data.get("value").getAsString(),
                    data.get("signature").getAsString()
            );

            CACHE.put(username, skin);
            CACHE_TIME.put(username, System.currentTimeMillis());
            return skin;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    public static class SkinData {
        public final String value;
        public final String signature;
        public SkinData(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }
    }
}