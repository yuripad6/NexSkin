package com.nexskin;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class NexSkin extends JavaPlugin {

    private static NexSkin instance;
    private SkinManager skinManager;
    private SkinApplier skinApplier;
    private SkinCache skinCache;
    private ConfigManager configManager;

    private static final String API_BASE =
            "https://nex-skin.blockchain-core-group.com/api/";
    private static final String DASHBOARD_URL =
            "https://nex-skin.blockchain-core-group.com/server/";

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        skinCache = new SkinCache(this);
        skinManager = new SkinManager(this);
        skinApplier = new SkinApplier(this);

        configManager = new ConfigManager(this);

        String existingKey = configManager.getServerKey();
        File dataFile = configManager.getDataFile();
        boolean hasDataFile = dataFile.exists() && dataFile.length() > 0;

        getLogger().info("data.yml: " + dataFile.getAbsolutePath());
        getLogger().info("data.yml существует: " + hasDataFile);

        if (existingKey == null || existingKey.isEmpty()) {
            if (hasDataFile) {
                getLogger().severe("========================================");
                getLogger().severe("data.yml существует, но server-key не читается!");
                getLogger().severe("Возможно, SECRET изменился между версиями.");
                getLogger().severe("НЕ регистрируюсь заново!");
                getLogger().severe("Варианты:");
                getLogger().severe("1. Удалить data.yml для перерегистрации");
                getLogger().severe("2. Восстановить SECRET");
                getLogger().severe("========================================");
                return;
            }
            getLogger().info("Первый запуск — регистрируемся на сайте...");
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                String newKey = new ServerRegistrar(this).registerAndGetKey();
                if (newKey != null) {
                    configManager.setServerKey(newKey);
                    getLogger().info("server-key сохранён в data.yml");
                } else {
                    getLogger().severe("Не удалось зарегистрироваться на сайте!");
                }
            });
        } else {
            getLogger().info("server-key загружен из data.yml: "
                    + existingKey.substring(0, Math.min(8, existingKey.length())) + "...");
        }

        getServer().getScheduler().runTaskTimerAsynchronously(this, () ->
                        new ServerRegistrar(this).register(),
                20L * 60 * 5, 20L * 60 * 5);

        SkinCommand command = new SkinCommand(this);
        getCommand("setskin").setExecutor(command);
        getCommand("clearskin").setExecutor(command);
        getCommand("updateskin").setExecutor(command);
        getCommand("nexskin").setExecutor(command);
        getCommand("nexskinupdate").setExecutor(command);

        if (Bukkit.getPluginManager().getPlugin("AuthMe") != null) {
            getServer().getPluginManager().registerEvents(
                    new AuthMeListener(this, skinApplier), this);
            getLogger().info("AuthMe найден — скин применяется после логина");
        } else {
            getServer().getPluginManager().registerEvents(new SkinListener(this), this);
            getLogger().info("AuthMe не найден — скин применяется при входе");
        }

        initBStats();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexSkinExpansion(this).register();
            getLogger().info("PlaceholderAPI найден — плейсхолдеры зарегистрированы");
        }

        // Автопроверка обновлений через 1 минуту после старта
        if (getConfig().getBoolean("auto-update-check", true)) {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                new UpdateChecker(this).checkOnly();
            }, 20L * 60);
        }

        getLogger().info("NexSkin v" + getDescription().getVersion() + " включён!");
    }

    @Override
    public void onDisable() {
        if (skinCache != null) skinCache.close();
        getLogger().info("NexSkin выключен.");
    }

    private void initBStats() {
        if (!getConfig().getBoolean("metrics", true)) {
            getLogger().info("bStats отключён в config.yml");
            return;
        }

        int pluginId = 34442;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new SimplePie("uses_authme", () ->
                Bukkit.getPluginManager().getPlugin("AuthMe") != null ? "Yes" : "No"));

        metrics.addCustomChart(new SimplePie("java_version", () ->
                System.getProperty("java.version")));

        metrics.addCustomChart(new SimplePie("server_version", () ->
                Bukkit.getBukkitVersion()));

        metrics.addCustomChart(new SingleLineChart("cached_skins", () ->
                skinCache.size()));

        metrics.addCustomChart(new SimplePie("uses_papi", () ->
                Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null ? "Yes" : "No"));
    }

    public String getApiUrl() {
        return API_BASE;
    }

    public String getDashboardUrl() {
        return DASHBOARD_URL;
    }

    public static NexSkin getInstance() { return instance; }
    public SkinManager getSkinManager() { return skinManager; }
    public SkinApplier getSkinApplier() { return skinApplier; }
    public SkinCache getSkinCache() { return skinCache; }
    public ConfigManager getConfigManager() { return configManager; }
}