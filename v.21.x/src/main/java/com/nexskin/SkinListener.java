package com.nexskin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SkinListener implements Listener {

    private final NexSkin plugin;
    private final boolean authMeEnabled;

    public SkinListener(NexSkin plugin) {
        this.plugin = plugin;
        this.authMeEnabled = Bukkit.getPluginManager().getPlugin("AuthMe") != null;

        if (authMeEnabled) {
            plugin.getLogger().info("AuthMe найден — скин будет применяться после логина");
            // Регистрируем обработчик LoginEvent
            Bukkit.getPluginManager().registerEvents(new AuthMeListener(plugin), plugin);
        } else {
            plugin.getLogger().info("AuthMe не найден — скин применяется при входе");
        }
    }

    // Без AuthMe — применяем при входе
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (authMeEnabled) return; // Если AuthMe есть — пропускаем, ждём LoginEvent

        Player player = event.getPlayer();
        applyDelayed(player, 20L);
    }

    // Общий метод для применения скина
    void applyDelayed(Player player, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                SkinManager.SkinData skinData = plugin.getSkinManager().getSkinFromSite(player.getName());
                if (skinData == null) {
                    skinData = plugin.getSkinManager().getSkinFromMojang(player.getName());
                }
                SkinManager.SkinData finalSkin = skinData;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalSkin != null && player.isOnline()) {
                        plugin.getSkinApplier().applySkin(player, finalSkin);
                        plugin.getLogger().info("Авто-скин применён для " + player.getName());
                    }
                });
            });
        }, delayTicks);
    }
}