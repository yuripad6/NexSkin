package com.nexskin;

import org.bukkit.plugin.java.JavaPlugin;

public final class NexSkin extends JavaPlugin {

    private static NexSkin instance;
    private SkinManager skinManager;
    private SkinApplier skinApplier;
    private SkinCache skinCache;

    @Override
    public void onEnable() {
        instance = this;

        skinCache = new SkinCache(this);
        skinManager = new SkinManager(this);
        skinApplier = new SkinApplier(this);

        // Регистрация сервера (асинхронно) — сразу
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            new ServerRegistrar(this).register();
        });

        // Обновление онлайна каждые 5 минут
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            new ServerRegistrar(this).register();
        }, 20L * 60 * 5, 20L * 60 * 5);  // 5 минут

        

        SkinCommand command = new SkinCommand(this);
        getCommand("setskin").setExecutor(command);
        getCommand("clearskin").setExecutor(command);
        getCommand("updateskin").setExecutor(command);
        getCommand("nexskin").setExecutor(command);

        getServer().getPluginManager().registerEvents(new SkinListener(this), this);

        getLogger().info("NexSkin v0.0.1-beta включён!");
    }

    @Override
    public void onDisable() {
        if (skinCache != null) {
            skinCache.close();
        }
        getLogger().info("NexSkin выключен.");
    }

    public static NexSkin getInstance() { return instance; }
    public SkinManager getSkinManager() { return skinManager; }
    public SkinApplier getSkinApplier() { return skinApplier; }
    public SkinCache getSkinCache() { return skinCache; }
}