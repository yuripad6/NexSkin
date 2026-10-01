package com.nexskin;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class SkinCache {

    private final NexSkin plugin;
    private final File dbFile;
    private final String url;
    private Connection connection;

    public SkinCache(NexSkin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "cache.db");
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        init();
    }

    private void init() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS skin_cache (" +
                        "nickname TEXT PRIMARY KEY, " +
                        "value TEXT NOT NULL, " +
                        "signature TEXT NOT NULL, " +
                        "updated_at INTEGER NOT NULL)");
            }

            plugin.getLogger().info("SQLite кэш инициализирован");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка SQLite: " + e.getMessage(), e);
        }
    }

    public SkinManager.SkinData get(String nickname, long ttlSeconds) {
        if (connection == null) return null;

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT value, signature, updated_at FROM skin_cache WHERE nickname = ?")) {
            ps.setString(1, nickname.toLowerCase());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                long updatedAt = rs.getLong("updated_at");
                long now = System.currentTimeMillis() / 1000;

                if (now - updatedAt < ttlSeconds) {
                    return new SkinManager.SkinData(
                            nickname,
                            rs.getString("value"),
                            rs.getString("signature")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка чтения кэша: " + e.getMessage());
        }
        return null;
    }

    public void put(String nickname, String value, String signature) {
        if (connection == null) return;

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO skin_cache (nickname, value, signature, updated_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, nickname.toLowerCase());
            ps.setString(2, value);
            ps.setString(3, signature);
            ps.setLong(4, System.currentTimeMillis() / 1000);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка записи кэша: " + e.getMessage());
        }
    }

    public void remove(String nickname) {
        if (connection == null) return;

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM skin_cache WHERE nickname = ?")) {
            ps.setString(1, nickname.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка удаления из кэша: " + e.getMessage());
        }
    }

    public void clearAll() {
        if (connection == null) return;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM skin_cache");
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка очистки кэша: " + e.getMessage());
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Ошибка закрытия SQLite: " + e.getMessage());
            }
        }
    }
}