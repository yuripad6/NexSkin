package com.nexskin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class SkinApplier {

    private final NexSkin plugin;

    public SkinApplier(NexSkin plugin) {
        this.plugin = plugin;
    }

    public void applySkin(Player player, SkinManager.SkinData skinData) {
        if (!player.isOnline()) return;
        if (skinData == null || skinData.getValue() == null || skinData.getSignature() == null) return;

        try {
            PlayerProfile profile = player.getPlayerProfile();
            profile.removeProperty("textures");
            profile.setProperty(new ProfileProperty("textures",
                    skinData.getValue(), skinData.getSignature()));
            player.setPlayerProfile(profile);

            playEffects(player, EffectType.SUCCESS);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Скин установлен для " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Не удалось применить скин для " + player.getName(), e);
        }
    }

    public void applyDelayed(Player player, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                SkinManager.SkinData skinData =
                        plugin.getSkinManager().getSkinFromSite(player.getName());
                if (skinData == null) {
                    skinData = plugin.getSkinManager().getSkinFromMojang(player.getName());
                }
                SkinManager.SkinData finalSkin = skinData;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalSkin != null && player.isOnline()) {
                        applySkin(player, finalSkin);
                    }
                });
            });
        }, delayTicks);
    }

    public void clearSkin(Player player) {
        if (!player.isOnline()) return;

        try {
            PlayerProfile profile = player.getPlayerProfile();
            profile.removeProperty("textures");

            // Тянем Стива с сайта
            SkinManager.SkinData steve = plugin.getSkinManager().getSkinFromSite("Steve");
            if (steve != null && steve.getValue() != null && steve.getSignature() != null) {
                profile.setProperty(new ProfileProperty("textures",
                        steve.getValue(), steve.getSignature()));
            }

            player.setPlayerProfile(profile);

            playEffects(player, EffectType.RESET);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Скин сброшен для " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Не удалось сбросить скин для " + player.getName(), e);
        }
    }

    /**
     * Проигрывает партиклы и звук при смене скина.
     */
    private void playEffects(Player player, EffectType type) {
        boolean particlesEnabled = plugin.getConfig().getBoolean("particles", true);
        boolean soundsEnabled = plugin.getConfig().getBoolean("sounds", true);

        if (!particlesEnabled && !soundsEnabled) return;

        switch (type) {
            case SUCCESS -> {
                if (particlesEnabled) {
                    // Зелёные искры + белые частицы
                    player.getWorld().spawnParticle(
                            Particle.HAPPY_VILLAGER,
                            player.getLocation().add(0, 1, 0),
                            20, 0.5, 0.5, 0.5, 0
                    );
                    player.getWorld().spawnParticle(
                            Particle.END_ROD,
                            player.getLocation().add(0, 1.5, 0),
                            10, 0.3, 0.3, 0.3, 0.05
                    );
                }
                if (soundsEnabled) {
                    player.playSound(
                            player.getLocation(),
                            Sound.ENTITY_PLAYER_LEVELUP,
                            0.7f, 1.5f
                    );
                }
            }
            case RESET -> {
                if (particlesEnabled) {
                    // Серый дым — сброс
                    player.getWorld().spawnParticle(
                            Particle.SMOKE,
                            player.getLocation().add(0, 1, 0),
                            15, 0.4, 0.5, 0.4, 0.02
                    );
                }
                if (soundsEnabled) {
                    player.playSound(
                            player.getLocation(),
                            Sound.BLOCK_ANVIL_LAND,
                            0.4f, 1.8f
                    );
                }
            }
        }
    }

    private enum EffectType {
        SUCCESS,
        RESET
    }
}