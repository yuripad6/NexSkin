package com.nexskin.velocity;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigManager {

    private static final String API_URL = "https://nex-skin.blockchain-core-group.com/api/";

    private final Properties props = new Properties();
    private final Path configPath;
    private final Path dataPath;
    private final Logger logger;

    private Map<String, Object> data = new HashMap<>();

    public ConfigManager(NexSkinVelocity plugin, Path dataDirectory) {
        this.logger = plugin.getLogger();
        this.configPath = dataDirectory.resolve("config.properties");
        this.dataPath = dataDirectory.resolve("data.yml");

        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            if (!Files.exists(configPath)) {
                try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
                    if (in != null) Files.copy(in, configPath);
                }
            }
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            }

            // Загружаем data.yml
            if (Files.exists(dataPath)) {
                try (InputStream in = Files.newInputStream(dataPath)) {
                    Object loaded = new Yaml().load(in);
                    if (loaded instanceof Map) {
                        data = (Map<String, Object>) loaded;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка загрузки конфига: " + e.getMessage());
        }
    }

    public String getApiUrl() {
        return API_URL;
    }

    public String getServerKey() {
        Object key = data.get("server-key");
        return key != null ? key.toString() : "";
    }

    public int getPlatformRaw() {
        Object val = data.get("platform_raw");
        return val != null ? Integer.parseInt(val.toString()) : 0;
    }

    public int getRandomId() {
        Object val = data.get("random_id");
        return val != null ? Integer.parseInt(val.toString()) : 0;
    }

    public int getCacheTtl() {
        return Integer.parseInt(props.getProperty("cache-ttl", "3600"));
    }

    public boolean isDebug() {
        return Boolean.parseBoolean(props.getProperty("debug", "false"));
    }

    public boolean isMetricsEnabled() {
        return Boolean.parseBoolean(props.getProperty("metrics", "true"));
    }

    public void setServerKey(String key) {
        data.put("server-key", key);
        saveData();
    }

    public void setPlatformRaw(int value) {
        data.put("platform_raw", value);
        saveData();
    }

    public void setRandomId(int value) {
        data.put("random_id", value);
        saveData();
    }

    private void saveData() {
        try (Writer writer = Files.newBufferedWriter(dataPath)) {
            new Yaml().dump(data, writer);
        } catch (IOException e) {
            logger.error("Ошибка сохранения data.yml: " + e.getMessage());
        }
    }
}