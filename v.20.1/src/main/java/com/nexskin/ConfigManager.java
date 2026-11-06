package com.nexskin;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.logging.Level;

public class ConfigManager {

    private final NexSkin plugin;
    private final File dataFile;
    private YamlConfiguration data;

    private static final String SECRET = computeSecret();

    public ConfigManager(NexSkin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось создать data.yml", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить data.yml", e);
        }
    }


    public String getServerKey() {
        String encrypted = data.getString("k", "");
        if (encrypted.isEmpty()) return "";
        return decrypt(encrypted);
    }

    public void setServerKey(String key) {
        data.set("k", encrypt(key));
        save();
    }

    public boolean hasServerKey() {
        String encrypted = data.getString("k", "");
        return !encrypted.isEmpty();
    }

    public File getDataFile() {
        return dataFile;
    }

    public int getPlatformRaw() {
        return data.getInt("p", 0);
    }

    public void setPlatformRaw(int platformRaw) {
        data.set("p", platformRaw);
        save();
    }


    public int getRandomId() {
        return data.getInt("r", 0);
    }

    public void setRandomId(int randomId) {
        data.set("r", randomId);
        save();
    }


    private static String computeSecret() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest("nexskin-secret-key-v1".getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "nexskin-fallback-secret";
        }
    }

    private String encrypt(String plain) {
        if (plain == null) return "";
        try {
            byte[] dataBytes = plain.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = SECRET.getBytes(StandardCharsets.UTF_8);
            byte[] result = new byte[dataBytes.length];
            for (int i = 0; i < dataBytes.length; i++) {
                result[i] = (byte) (dataBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            return "";
        }
    }

    private String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return "";
        try {
            byte[] dataBytes = Base64.getDecoder().decode(encrypted);
            byte[] keyBytes = SECRET.getBytes(StandardCharsets.UTF_8);
            byte[] result = new byte[dataBytes.length];
            for (int i = 0; i < dataBytes.length; i++) {
                result[i] = (byte) (dataBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}