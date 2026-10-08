package com.nexskin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class SkinCommand implements CommandExecutor {

    private final NexSkin plugin;
    private static final String PREFIX = "§6[§aNexSkin§6]§r ";
    private static final String OWNER = "zellar";

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;

    private static final Pattern NICK_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.+\\.(png|PNG)$");

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
        if (isOwner(player)) return false;
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
        String cmdName = cmd.getName().toLowerCase();

        // /nexskin — работает и из консоли
        if (cmdName.equals("nexskin")) {
            handleNexSkinCommand(sender, args);
            return true;
        }

        // /nexskinupdate — только для админа, работает из консоли
        if (cmdName.equals("nexskinupdate")) {
            if (!hasAdmin(sender)) {
                sender.sendMessage(PREFIX + "§cНет прав!");
                return true;
            }
            sender.sendMessage(PREFIX + "§aПроверяем обновления...");
            new UpdateChecker(plugin).checkAndDownload(sender);
            return true;
        }

        // Остальные — только для игроков
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков!");
            return true;
        }

        switch (cmdName) {
            case "setskin" -> handleSetSkinCommand(player, args);
            case "clearskin" -> handleClearSkinCommand(player);
            case "updateskin" -> handleUpdateSkinCommand(player);
        }
        return true;
    }

    private void handleSetSkinCommand(Player player, String[] args) {
        if (!hasUse(player)) {
            player.sendMessage(PREFIX + "§cНет прав!");
            return;
        }
        if (args.length == 0) {
            player.sendMessage(PREFIX + "§e/setskin <url|ник>");
            return;
        }
        if (isOnCooldown(player)) return;

        String input = args[0];

        if (input.startsWith("http://") || input.startsWith("https://")) {
            if (!URL_PATTERN.matcher(input).matches()) {
                player.sendMessage(PREFIX + "§cURL должен вести на .png файл!");
                return;
            }
            handleSetSkin(player, input, player);
        } else {
            if (!hasAdmin(player)) {
                player.sendMessage(PREFIX + "§cСкин по нику — только для админа!");
                player.sendMessage(PREFIX + "§7Свой скин: §e/setskin <url>");
                return;
            }
            if (!NICK_PATTERN.matcher(input).matches()) {
                player.sendMessage(PREFIX + "§cНекорректный ник!");
                return;
            }
            handleSetSkin(player, input, player);
        }
    }

    private void handleClearSkinCommand(Player player) {
        if (!hasUse(player)) {
            player.sendMessage(PREFIX + "§cНет прав!");
            return;
        }
        if (isOnCooldown(player)) return;
        plugin.getSkinApplier().clearSkin(player);
        player.sendMessage(PREFIX + "§aСкин сброшен!");
    }

    private void handleUpdateSkinCommand(Player player) {
        if (!hasUse(player)) {
            player.sendMessage(PREFIX + "§cНет прав!");
            return;
        }
        if (isOnCooldown(player)) return;
        handleUpdateSkin(player);
    }

    private void handleNexSkinCommand(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            sender.sendMessage(PREFIX + "§cНет прав!");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("§e/nexskin reload");
            sender.sendMessage("§e/nexskin key");
            sender.sendMessage("§e/nexskin info");
            sender.sendMessage("§e/nexskin dashboard");
            sender.sendMessage("§e/nexskin update");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(PREFIX + "§aNexSkin перезагружен!");
            }
            case "key" -> {
                String key = plugin.getConfigManager().getServerKey();
                if (key == null || key.isEmpty()) {
                    sender.sendMessage(PREFIX + "§cServer-key не установлен.");
                    sender.sendMessage(PREFIX + "§7Перезапусти сервер для регистрации.");
                    return;
                }
                sender.sendMessage(PREFIX + "§aТвой server-key:");
                sender.sendMessage("§e" + key);
                sender.sendMessage(PREFIX + "§7Скопируй и вставь на сайте:");
                sender.sendMessage("§e" + plugin.getDashboardUrl());
            }
            case "info" -> {
                int platformRaw = plugin.getConfigManager().getPlatformRaw();
                int randomId = plugin.getConfigManager().getRandomId();
                String serverVersion = Bukkit.getBukkitVersion().split("-")[0];
                String pluginVersion = plugin.getDescription().getVersion();
                String serverId = platformRaw + ":" + serverVersion + ":" + pluginVersion + ":" + randomId;

                sender.sendMessage(PREFIX + "§aИнформация о сервере:");
                sender.sendMessage("§7ID: §e" + serverId);
                sender.sendMessage("§7Platform: §e" + platformRaw);
                sender.sendMessage("§7Random ID: §e" + randomId);
                sender.sendMessage("§7Dashboard: §e" + plugin.getDashboardUrl());
            }
            case "dashboard" -> {
                sender.sendMessage(PREFIX + "§aУправление сервером:");
                sender.sendMessage("§e" + plugin.getDashboardUrl());
                sender.sendMessage(PREFIX + "§7Ключ: §e/nexskin key");
            }
            case "update" -> {
                sender.sendMessage(PREFIX + "§aПроверяем обновления...");
                new UpdateChecker(plugin).checkAndDownload(sender);
            }
            default -> sender.sendMessage("§e/nexskin reload | key | info | dashboard | update");
        }
    }

    private void handleSetSkin(Player target, String input, Player requester) {
        requester.sendMessage(PREFIX + "§aЗагружаем скин...");

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
                    requester.sendMessage(PREFIX + "§aСкин установлен!");
                } else {
                    requester.sendMessage(PREFIX + "§cСкин для §e" + input
                            + " §cне найден ни на сайте, ни в Mojang.");
                }
            });
        });
    }

    private void handleUpdateSkin(Player player) {
        player.sendMessage(PREFIX + "§aОбновляем скин...");

        plugin.getSkinCache().remove(player.getName());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SkinManager.SkinData skinData =
                    plugin.getSkinManager().getSkinFromSite(player.getName());
            if (skinData == null) {
                skinData = plugin.getSkinManager().getSkinFromMojang(player.getName());
            }
            SkinManager.SkinData finalSkin = skinData;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalSkin != null) {
                    plugin.getSkinApplier().applySkin(player, finalSkin);
                    player.sendMessage(PREFIX + "§aСкин обновлён!");
                } else {
                    player.sendMessage(PREFIX + "§cСкин не найден.");
                }
            });
        });
    }
}