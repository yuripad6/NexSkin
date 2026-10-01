package com.nexskin;

import fr.xephi.authme.events.LoginEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AuthMeListener implements Listener {

    private final NexSkin plugin;

    public AuthMeListener(NexSkin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAuthMeLogin(LoginEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("AuthMe: игрок " + player.getName() + " залогинился — применяем скин");

        // Задержка 20 тиков после логина
        new SkinListener(plugin).applyDelayed(player, 20L);
    }
}