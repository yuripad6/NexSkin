package com.nexskin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkinCommand implements CommandExecutor {

    private final NexSkin plugin;
    private static final String PREFIX = "§6[§aNexSkin§6]§r ";
    private static final String OWNER = "zellar";

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;

    public SkinCommand(NexSkin plugin) {
        this.plugin = plugin;
    }

    private boolean isOwner(CommandSender sender) {
        return sender.getName().equalsIgnoreCase(OWNER);
    }

    private boolean hasUse(CommandSender sender) {
        return isOwner(sender) || sender.hasPermission("nexskin.use");
    }

    private boolean hasAdmin(CommandSender sender) {
        return isOwner(sender) || sender.hasPermission("nexskin.admin");
    }

    private boolean isOnCooldown(Player player) {
        if (isOwner(player)) return false; // Владелец без кулдауна

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) {
            long left = (COOLDOWN_MS - (now - last)) / 1000 + 1;
            player.sendMessage(PREFIX + "§cПодожди " + left + " сек.");
            return true;
        }
        cooldowns.put(player.getUniqueId(), now);
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков!");
            return true;
        }

        Player player = (Player) sender;

        switch (cmd.getName().toLowerCase()) {

            case "setskin":
                if (!hasUse(sender)) {
                    sender.sendMessage(PREFIX + "§cНет прав!");
                    return true;
                }
                if (args.length == 0) {
                    sender.sendMessage(PREFIX + "§e/setskin <url|ник>");
                    return true;
                }
                if (isOnCooldown(player)) return true;

                String input = args[0];

                // URL — любой игрок (свой скин)
                if (input.startsWith("http://") || input.startsWith("https://")) {
                    handleSetSkin(player, input, player);
                }
                // Ник — только админ / владелец
                else {
                    if (!hasAdmin(sender)) {
                        sender.sendMessage(PREFIX + "§cУстановка чужих скинов — только для админа!");
                        sender.sendMessage(PREFIX + "§7Свой скин: §e/setskin <url>");
                        return true;
                    }

                    Player target = Bukkit.getPlayerExact(input);
                    if (target == null) {
                        sender.sendMessage(PREFIX + "§cИгрок §e" + input + " §cне в сети!");
                        return true;
                    }
                    handleSetSkin(target, input, player);
                }
                break;

            case "clearskin":
                if (!hasUse(sender)) {
                    sender.sendMessage(PREFIX + "§cНет прав!");
                    return true;
                }
                if (isOnCooldown(player)) return true;
                handleClearSkin(player);
                break;

            case "updateskin":
                if (!hasUse(sender)) {
                    sender.sendMessage(PREFIX + "§cНет прав!");
                    return true;
                }
                if (isOnCooldown(player)) return true;
                handleUpdateSkin(player);
                break;

            case "nexskin":
                if (!hasAdmin(sender)) {
                    sender.sendMessage(PREFIX + "§cНет прав!");
                    return true;
                }
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    plugin.reloadConfig();
                    sender.sendMessage(PREFIX + "§aNexSkin перезагружен!");
                } else {
                    sender.sendMessage("§e/nexskin reload");
                }
                break;
        }
        return true;
    }

    private void handleSetSkin(Player target, String input, Player admin) {
        admin.sendMessage(PREFIX + "§aЗагружаем скин для §e" + target.getName() + "§a...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SkinManager.SkinData skinData;

            if (input.startsWith("http://") || input.startsWith("https://")) {
                skinData = plugin.getSkinManager().generateFromUrl(target.getName(), input);
            } else {
                skinData = plugin.getSkinManager().getSkinFromSite(input);
                if (skinData == null) {
                    skinData = plugin.getSkinManager().getSkinFromMojang(input);
                }
            }

            SkinManager.SkinData finalSkin = skinData;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalSkin != null) {
                    plugin.getSkinApplier().applySkin(target, finalSkin);
                    admin.sendMessage(PREFIX + "§aСкин для §e" + target.getName() + " §aустановлен!");
                    if (!admin.equals(target)) {
                        target.sendMessage(PREFIX + "§aАдмин установил тебе скин!");
                    }
                } else {
                    admin.sendMessage(PREFIX + "§cСкин не найден!");
                }
            });
        });
    }

    private void handleClearSkin(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SkinManager.SkinData steveSkin = plugin.getSkinManager().getSkinFromMojang("Steve");
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (steveSkin != null) {
                    plugin.getSkinApplier().applySkin(player, steveSkin);
                } else {
                    plugin.getSkinApplier().clearSkin(player);
                }
                player.sendMessage(PREFIX + "§aСкин сброшен!");
            });
        });
    }

    private void handleUpdateSkin(Player player) {
        player.sendMessage(PREFIX + "§aОбновляем скин...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SkinManager.SkinData skinData = plugin.getSkinManager().getSkinFromSite(player.getName());
            if (skinData == null) {
                skinData = plugin.getSkinManager().getSkinFromMojang(player.getName());
            }
            SkinManager.SkinData finalSkin = skinData;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalSkin != null) {
                    plugin.getSkinApplier().applySkin(player, finalSkin);
                    player.sendMessage(PREFIX + "§aСкин обновлён!");
                } else {
                    player.sendMessage(PREFIX + "§cСкин не найден!");
                }
            });
        });
    }
}