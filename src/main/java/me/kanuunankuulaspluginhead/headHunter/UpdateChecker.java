package me.kanuunankuulaspluginhead.headHunter;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.bukkit.Bukkit.getLogger;

public class UpdateChecker {
    private static String apiUrl = "https://api.github.com/repos/If-Master/HeadHunter/releases/latest";
    private static String assetUrl = "https://api.github.com/repos/If-Master/HeadHunter/releases/assets/";

    public static void checkForUpdates() {
        MainScript.runAsync(() -> {
            try {

                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "HeadHunterToken");

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    getLogger().warning("Failed to check updates. Code: " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String responseBody = response.toString();
                int tagStart = responseBody.indexOf("\"tag_name\":\"") + 12;
                int tagEnd = responseBody.indexOf("\"", tagStart);
                String latest = responseBody.substring(tagStart, tagEnd);
                String current = MainScript.getCurrentVersion();

                if (!current.equals(latest)) {
                    MainScript.setUpdateAvailable(true, latest);
                    getLogger().info("Update available! Current: " + current + ", Latest: " + latest);
                } else {
                    getLogger().info("No updates found.");
                }
            } catch (Exception e) {
                getLogger().warning("Failed to check for updates: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static void updatePluginFromGitHub(CommandSender sender, Plugin plugin) {
        sender.sendMessage("§6Starting plugin update check...");

        MainScript.runAsync(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestProperty("User-Agent", "HeadHunterToken");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                String assetName = "HeadHunter.jar";
                int index = json.indexOf("\"name\":\"" + assetName + "\"");

                if (index == -1) {
                    MainScript.runTask(null, () -> sender.sendMessage("§cRelease asset not found: " + assetName));
                    return;
                }

                int idStart = json.lastIndexOf("\"id\":", index) + 5;
                int idEnd = json.indexOf(",", idStart);
                String assetId = json.substring(idStart, idEnd).trim();

                downloadPluginUpdate(sender, assetId, plugin);
            } catch (Exception e) {
                MainScript.runTask(null, () -> sender.sendMessage("§cFailed to update plugin: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }
    public static void downloadPluginUpdate(CommandSender sender, String assetId, Plugin plugin) {
        MainScript.runAsync(() -> {
            try {
                String downloadUrl = assetUrl + assetId;

                HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/octet-stream");
                conn.setRequestProperty("User-Agent", "HeadHunterToken");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    MainScript.runTask(null, () -> {
                        sender.sendMessage("§cDownload failed. Code: " + responseCode);
                        getLogger().warning("Asset download failed with code: " + responseCode);
                    });
                    return;
                }

                File pluginFile = new File(plugin.getDataFolder().getParentFile(), "HeadHunter.jar");
                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, pluginFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                MainScript.runTask(null, () -> sender.sendMessage("§aPlugin updated. Restart the server to finish update."));

            } catch (Exception e) {
                MainScript.runTask(null, () -> sender.sendMessage("§cFailed to download plugin asset."));
                getLogger().warning("Download error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
