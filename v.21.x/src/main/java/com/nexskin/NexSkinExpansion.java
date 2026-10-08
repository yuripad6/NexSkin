package com.nexskin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NexSkinExpansion extends PlaceholderExpansion {

    private final NexSkin plugin;

    public NexSkinExpansion(NexSkin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexskin";
    }

    @Override
    public @NotNull String getAuthor() {
        return "yuripad6";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "has_skin":
                return plugin.getSkinCache().get(player.getName(), 3600) != null
                        ? "true" : "false";

            case "cache_size":
                return String.valueOf(plugin.getSkinCache().size());

            case "online_with_skin": {
                int count = 0;
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (plugin.getSkinCache().get(p.getName(), 3600) != null) count++;
                }
                return String.valueOf(count);
            }

            default:
                return null;
        }
    }
}