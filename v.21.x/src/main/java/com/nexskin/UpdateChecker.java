package com.nexskin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.logging.Level;

public class UpdateChecker {

    private final NexSkin plugin;

    public UpdateChecker(NexSkin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ручная проверка (по команде /nexskin update)
     * Скачивает jar и уведомляет
     */
    public void checkAndDownload(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String mcVersion = Bukkit.getBukkitVersion().split("-")[0];
                String apiUrl = plugin.getApiUrl() + "version.php?mc="
                        + URLEncoder.encode(mcVersion, "UTF-8");

                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "NexSkin/1.0");

                if (conn.getResponseCode() == 404) {
                    sendMsg(sender, "§cНет версии для MC " + mcVersion);
                    return;
                }

                if (conn.getResponseCode() != 200) {
                    sendMsg(sender, "§cНе удалось проверить обновление (HTTP " + conn.getResponseCode() + ")");
                    return;
                }

                String json = readResponse(conn);
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();

                String latestVersion = data.get("version").getAsString();
                String currentVersion = plugin.getDescription().getVersion();

                if (latestVersion.equals(currentVersion)) {
                    sendMsg(sender, "§aУ тебя последняя версия: " + currentVersion + " §7(MC " + mcVersion + ")");
                    return;
                }

                sendMsg(sender, "§eНайдена новая версия: §a" + latestVersion
                        + " §7(у тебя: " + currentVersion + ", MC " + mcVersion + ")");
                sendMsg(sender, "§7Скачиваем...");

                String downloadUrl = data.get("url").getAsString();
                File updateDir = new File(Bukkit.getPluginsFolder(), "update");
                updateDir.mkdirs();
                File newJar = new File(updateDir, "NexSkin-" + latestVersion + ".jar");

                downloadFile(downloadUrl, newJar);

                if (data.has("sha256") && !data.get("sha256").getAsString().isEmpty()) {
                    String expectedHash = data.get("sha256").getAsString();
                    String actualHash = calculateSHA256(newJar);
                    if (!expectedHash.equals(actualHash)) {
                        newJar.delete();
                        sendMsg(sender, "§cХеш не совпадает! Обновление отменено.");
                        return;
                    }
                }

                sendMsg(sender, "§aОбновление §e" + latestVersion + " §aскачано!");
                sendMsg(sender, "§7Перезапусти сервер командой §e/restart");
                if (data.has("changelog") && !data.get("changelog").getAsString().isEmpty()) {
                    sendMsg(sender, "§7Что нового: " + data.get("changelog").getAsString());
                }

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка обновления", e);
                sendMsg(sender, "§cОшибка: " + e.getMessage());
            }
        });
    }

    /**
     * Автопроверка + автоскачивание при старте
     * Только уведомляет админа, БЕЗ перезапуска
     */
    public void checkOnly() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String mcVersion = Bukkit.getBukkitVersion().split("-")[0];
                String apiUrl = plugin.getApiUrl() + "version.php?mc="
                        + URLEncoder.encode(mcVersion, "UTF-8");

                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "NexSkin/1.0");

                if (conn.getResponseCode() != 200) return;

                String json = readResponse(conn);
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();

                String latestVersion = data.get("version").getAsString();
                String currentVersion = plugin.getDescription().getVersion();

                if (latestVersion.equals(currentVersion)) {
                    return; // Уже последняя
                }

                String changelog = data.has("changelog")
                        ? data.get("changelog").getAsString() : "";

                File updateDir = new File(Bukkit.getPluginsFolder(), "update");
                updateDir.mkdirs();
                File newJar = new File(updateDir, "NexSkin-" + latestVersion + ".jar");

                // Уже скачано — не качаем повторно
                if (newJar.exists()) {
                    plugin.getLogger().info("Обновление " + latestVersion + " уже скачано, ждёт /restart");
                    notifyAdmins(latestVersion, currentVersion, mcVersion, changelog, true);
                    return;
                }

                // Скачиваем
                plugin.getLogger().info("Найдена новая версия: " + latestVersion + ", скачиваем...");
                String downloadUrl = data.get("url").getAsString();
                downloadFile(downloadUrl, newJar);

                if (data.has("sha256") && !data.get("sha256").getAsString().isEmpty()) {
                    String expectedHash = data.get("sha256").getAsString();
                    String actualHash = calculateSHA256(newJar);
                    if (!expectedHash.equals(actualHash)) {
                        newJar.delete();
                        plugin.getLogger().warning("Хеш не совпадает! Обновление отменено.");
                        return;
                    }
                }

                plugin.getLogger().info("======================================");
                plugin.getLogger().info("Обновление " + latestVersion + " скачано!");
                plugin.getLogger().info("У тебя: " + currentVersion + " (MC " + mcVersion + ")");
                if (!changelog.isEmpty()) {
                    plugin.getLogger().info("Что нового: " + changelog);
                }
                plugin.getLogger().info("Перезапусти сервер командой /restart");
                plugin.getLogger().info("======================================");

                notifyAdmins(latestVersion, currentVersion, mcVersion, changelog, false);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка автообновления", e);
            }
        });
    }

    /**
     * Уведомить админов онлайн
     */
    private void notifyAdmins(String newVersion, String currentVersion,
                              String mcVersion, String changelog, boolean alreadyDownloaded) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("nexskin.admin")) continue;

                if (alreadyDownloaded) {
                    p.sendMessage("§6[§aNexSkin§6]§r §eОбновление §a" + newVersion + " §eуже скачано");
                } else {
                    p.sendMessage("§6[§aNexSkin§6]§r §aОбновление §e" + newVersion + " §aскачано!");
                }
                p.sendMessage("§7У тебя: §e" + currentVersion + " §7(MC " + mcVersion + ")");
                if (!changelog.isEmpty()) {
                    p.sendMessage("§7Что нового: §e" + changelog);
                }
                p.sendMessage("§7Перезапусти сервер: §e/restart");
            }
        });
    }

    private void sendMsg(CommandSender sender, String msg) {
        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                "§6[§aNexSkin§6]§r " + msg));
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private void downloadFile(String url, File destination) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "NexSkin/1.0");

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private String calculateSHA256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) hexString.append(String.format("%02x", b));
        return hexString.toString();
    }
}