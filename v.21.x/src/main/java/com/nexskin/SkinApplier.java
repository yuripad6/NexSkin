package com.nexskin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SkinApplier {

    private final NexSkin plugin;

    public SkinApplier(NexSkin plugin) {
        this.plugin = plugin;
    }

    public void applySkin(Player player, SkinManager.SkinData skinData) {
        if (!player.isOnline()) return;
        if (skinData == null || skinData.getValue() == null || skinData.getSignature() == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                PlayerProfile profile = player.getPlayerProfile();
                profile.removeProperty("textures");
                profile.setProperty(new ProfileProperty("textures",
                        skinData.getValue(), skinData.getSignature()));
                player.setPlayerProfile(profile);

                player.hidePlayer(plugin, player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.showPlayer(plugin, player);
                }, 5L);

                plugin.getLogger().info("[NexSkin] Скин установлен для " + player.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("[NexSkin] Ошибка установки скина: " + e.getMessage());
            }
        }, 10L);
    }

    public void clearSkin(Player player) {
        if (!player.isOnline()) return;
        try {
            PlayerProfile profile = player.getPlayerProfile();
            profile.removeProperty("textures");
            player.setPlayerProfile(profile);
            plugin.getLogger().info("[NexSkin] Скин сброшен для " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("[NexSkin] Ошибка сброса скина: " + e.getMessage());
        }
    }
}
