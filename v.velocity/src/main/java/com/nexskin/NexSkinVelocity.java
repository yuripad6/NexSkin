package com.nexskin.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Plugin(id = "nexskin", name = "NexSkin", version = "0.1.0-beta",
        description = "Скины без модов для пираток и лицензий",
        authors = {"yuripad6"})
public class NexSkinVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    private ConfigManager configManager;
    private SkinFetcher skinFetcher;
    private SkinCache skinCache;
    private ServerRegistrar serverRegistrar;

    @Inject
    public NexSkinVelocity(ProxyServer server, Logger logger,
                           @DataDirectory Path dataDirectory,
                           Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        configManager = new ConfigManager(this, dataDirectory);
        skinCache = new SkinCache(configManager.getCacheTtl());
        skinFetcher = new SkinFetcher(this, configManager);
        serverRegistrar = new ServerRegistrar(this, configManager);

        if (configManager.getServerKey() == null || configManager.getServerKey().isEmpty()) {
            logger.info("Первый запуск — регистрируемся на сайте...");
            server.getScheduler().buildTask(this, () -> {
                String newKey = serverRegistrar.registerAndGetKey();
                if (newKey != null) {
                    configManager.setServerKey(newKey);
                    logger.info("Server-key сохранён в config.properties");
                } else {
                    logger.error("Не удалось зарегистрироваться на сайте!");
                }
            }).schedule();
        } else {
            logger.info("Server-key загружен: " + configManager.getServerKey().substring(0, 8) + "...");

            server.getScheduler().buildTask(this, () -> serverRegistrar.register())
                    .repeat(java.time.Duration.ofMinutes(5))
                    .schedule();
        }

        if (configManager.isMetricsEnabled()) {
            try {
                metricsFactory.make(this, 34442);
                logger.info("bStats инициализирован");
            } catch (Exception e) {
                logger.warn("Не удалось инициализировать bStats: " + e.getMessage());
            }
        }

        logger.info("NexSkin Velocity включён!");
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        String username = event.getUsername();

        SkinFetcher.SkinData cached = skinCache.get(username);
        if (cached != null) {
            applySkin(event, cached, username);
            return;
        }

        server.getScheduler().buildTask(this, () -> {
            SkinFetcher.SkinData skin = skinFetcher.fetch(username);
            if (skin != null) {
                skinCache.put(username, skin);
            }
        }).schedule();
    }

    private void applySkin(GameProfileRequestEvent event, SkinFetcher.SkinData skin, String username) {
        try {
            GameProfile original = event.getGameProfile();

            List<GameProfile.Property> props = new ArrayList<>();
            for (GameProfile.Property p : original.getProperties()) {
                if (!p.getName().equals("textures")) {
                    props.add(p);
                }
            }

            props.add(new GameProfile.Property("textures", skin.value(), skin.signature()));

            GameProfile newProfile = new GameProfile(
                    original.getId(),
                    original.getName(),
                    props
            );
            event.setGameProfile(newProfile);

            if (configManager.isDebug()) {
                logger.info("Применён скин для " + username);
            }
        } catch (Exception e) {
            logger.warn("Не удалось применить скин для " + username + ": " + e.getMessage());
        }
    }

    public ProxyServer getServer() { return server; }
    public Logger getLogger() { return logger; }
    public ConfigManager getConfigManager() { return configManager; }
    public SkinCache getSkinCache() { return skinCache; }
}