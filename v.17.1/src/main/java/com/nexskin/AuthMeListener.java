package com.nexskin;

import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AuthMeListener implements Listener {

    private final NexSkin plugin;
    private final SkinApplier applier;

    public AuthMeListener(NexSkin plugin, SkinApplier applier) {
        this.plugin = plugin;
        this.applier = applier;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAuthMeLogin(LoginEvent event) {
        handle(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAuthMeRegister(RegisterEvent event) {
        handle(event.getPlayer());
    }

    private void handle(Player player) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("AuthMe: " + player.getName() + " — применяем скин");
        }
        applier.applyDelayed(player, 20L);
    }
}