package me.kanuunankuulaspluginhead.headHunter.Texture;

import me.kanuunankuulaspluginhead.headHunter.MainScript;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

import static me.kanuunankuulaspluginhead.headHunter.MainScript.*;

public class PlayerSkinData {

    private static final Map<String, PlayerProfile> PROFILE_CACHE = new HashMap<>();
    private static final Map<String, Long> CACHE_TIMESTAMPS = new HashMap<>();
    private static final long CACHE_EXPIRY = 3600000;

    public static void createAndGivePlayerHeadWithSkin(Player player, String playerName) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            givePlayerHead(player, playerName, targetPlayer.getPlayerProfile());
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            loadOfflinePlayerProfile(playerName, offlinePlayer.getUniqueId(), profile -> {
                if (profile != null) {
                    givePlayerHead(player, playerName, profile);
                } else {
                    player.sendMessage("§7Fetching skin from Mojang...");
                    fetchFromMojangAPI(player, playerName);
                }
            });
            return;
        }

        player.sendMessage("§7Fetching player skin from Mojang...");
        fetchFromMojangAPI(player, playerName);
    }

    private static void loadOfflinePlayerProfile(String playerName, UUID uuid, Consumer<PlayerProfile> callback) {
        runAsync(() -> {
            try {
                MainScript.getInstance().getLogger().info("[HeadHunter] Attempting to load offline profile for: " + playerName + " (UUID: " + uuid + ")");

                PlayerProfile profile = Bukkit.createPlayerProfile(uuid, playerName);

                MainScript.getInstance().getLogger().info("[HeadHunter] Created profile, now updating from Mojang...");

                profile.update().whenComplete((updatedProfile, error) -> {
                    runTask(null, () -> {
                        if (error != null) {
                            MainScript.getInstance().getLogger().warning(
                                    "[HeadHunter] Failed to update profile for " + playerName + ": " + error.getMessage()
                            );
                            error.printStackTrace();
                            callback.accept(null);
                            return;
                        }

                        if (updatedProfile != null) {
                            PlayerTextures textures = updatedProfile.getTextures();
                            MainScript.getInstance().getLogger().info("[HeadHunter] Profile updated. Has skin: " + (textures.getSkin() != null));

                            if (textures.getSkin() != null) {
                                MainScript.getInstance().getLogger().info("[HeadHunter] Skin URL: " + textures.getSkin());
                                PROFILE_CACHE.put(playerName.toLowerCase(), updatedProfile);
                                CACHE_TIMESTAMPS.put(playerName.toLowerCase(), System.currentTimeMillis());
                                callback.accept(updatedProfile);
                            } else {
                                MainScript.getInstance().getLogger().warning("[HeadHunter] Profile has no skin texture");
                                callback.accept(null);
                            }
                        } else {
                            MainScript.getInstance().getLogger().warning("[HeadHunter] Updated profile is null");
                            callback.accept(null);
                        }
                    });
                });
            } catch (Exception e) {
                MainScript.getInstance().getLogger().warning(
                        "[HeadHunter] Error loading offline player profile for " + playerName + ": " + e.getMessage()
                );
                e.printStackTrace();
                runTask(null, () -> callback.accept(null));
            }
        });
    }

    private static void fetchFromMojangAPI(Player player, String playerName) {
        String lowerName = playerName.toLowerCase();

        Long timestamp = CACHE_TIMESTAMPS.get(lowerName);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY) {
            PlayerProfile cachedProfile = PROFILE_CACHE.get(lowerName);
            if (cachedProfile != null) {
                MainScript.getInstance().getLogger().info("[HeadHunter] Using cached profile for: " + playerName);
                givePlayerHead(player, playerName, cachedProfile);
                return;
            }
        }

        MainScript.getInstance().getLogger().info("[HeadHunter] Fetching from Mojang API for: " + playerName);

        runAsync(() -> {
            try {
                String uuid = fetchUUIDFromMojang(playerName);
                if (uuid == null) {
                    MainScript.getInstance().getLogger().warning("[HeadHunter] Could not fetch UUID for: " + playerName);
                    runTask(player.getLocation(), () -> {
                        player.sendMessage("§cPlayer not found on Mojang servers!");
                    });
                    return;
                }

                MainScript.getInstance().getLogger().info("[HeadHunter] Found UUID: " + uuid + " for " + playerName);

                PlayerProfile profile = fetchProfileFromMojang(uuid, playerName);
                if (profile != null) {
                    MainScript.getInstance().getLogger().info("[HeadHunter] Successfully created profile from Mojang data");
                    PROFILE_CACHE.put(lowerName, profile);
                    CACHE_TIMESTAMPS.put(lowerName, System.currentTimeMillis());

                    runTask(player.getLocation(), () -> {
                        givePlayerHead(player, playerName, profile);
                    });
                } else {
                    MainScript.getInstance().getLogger().warning("[HeadHunter] Failed to create profile from Mojang data");
                    runTask(player.getLocation(), () -> {
                        player.sendMessage("§cFailed to fetch skin data!");
                    });
                }
            } catch (Exception e) {
                MainScript.getInstance().getLogger().warning(
                        "[HeadHunter] Error fetching from Mojang API for " + playerName + ": " + e.getMessage()
                );
                e.printStackTrace();
                runTask(player.getLocation(), () -> {
                    player.sendMessage("§cError fetching player skin: " + e.getMessage());
                });
            }
        });
    }

    private static String fetchUUIDFromMojang(String playerName) {
        try {
            String apiUrl = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            MainScript.getInstance().getLogger().info("[HeadHunter] Fetching UUID from: " + apiUrl);

            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "HeadHunter-Plugin");

            int responseCode = connection.getResponseCode();
            MainScript.getInstance().getLogger().info("[HeadHunter] UUID API Response Code: " + responseCode);

            if (responseCode != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();
            MainScript.getInstance().getLogger().info("[HeadHunter] UUID API Response: " + json);

            int idStart = json.indexOf("\"id\"");
            if (idStart == -1) {
                MainScript.getInstance().getLogger().warning("[HeadHunter] Could not find 'id' field in response");
                return null;
            }

            int colonIndex = json.indexOf(":", idStart);
            if (colonIndex == -1) {
                return null;
            }

            int valueStart = json.indexOf("\"", colonIndex) + 1;
            int valueEnd = json.indexOf("\"", valueStart);

            if (valueStart < 1 || valueEnd <= valueStart) {
                MainScript.getInstance().getLogger().warning("[HeadHunter] Could not parse UUID from response");
                return null;
            }

            String uuid = json.substring(valueStart, valueEnd);
            MainScript.getInstance().getLogger().info("[HeadHunter] Parsed UUID: " + uuid);

            return uuid;
        } catch (Exception e) {
            MainScript.getInstance().getLogger().warning(
                    "[HeadHunter] Failed to fetch UUID from Mojang for " + playerName + ": " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    private static PlayerProfile fetchProfileFromMojang(String uuid, String playerName) {
        try {
            String apiUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
            MainScript.getInstance().getLogger().info("[HeadHunter] Fetching profile from: " + apiUrl);

            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "HeadHunter-Plugin");

            int responseCode = connection.getResponseCode();
            MainScript.getInstance().getLogger().info("[HeadHunter] Profile API Response Code: " + responseCode);

            if (responseCode != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();
            MainScript.getInstance().getLogger().info("[HeadHunter] Profile API Response length: " + json.length());

            String textureData = extractTextureData(json);

            if (textureData != null) {
                MainScript.getInstance().getLogger().info("[HeadHunter] Extracted texture data (length: " + textureData.length() + ")");
                return createProfileFromTexture(uuid, playerName, textureData);
            } else {
                MainScript.getInstance().getLogger().warning("[HeadHunter] No texture data found in response");
            }
            return null;
        } catch (Exception e) {
            MainScript.getInstance().getLogger().warning(
                    "[HeadHunter] Failed to fetch profile from Mojang for " + playerName + ": " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    private static String extractTextureData(String json) {
        try {
            int valueIndex = json.indexOf("\"value\"");
            if (valueIndex == -1) {
                MainScript.getInstance().getLogger().warning("[HeadHunter] Could not find 'value' field in texture response");
                return null;
            }

            int colonIndex = json.indexOf(":", valueIndex);
            if (colonIndex == -1) {
                return null;
            }

            int valueStart = json.indexOf("\"", colonIndex) + 1;
            int valueEnd = json.indexOf("\"", valueStart);

            if (valueStart < 1 || valueEnd <= valueStart) {
                MainScript.getInstance().getLogger().warning("[HeadHunter] Could not parse texture value from response");
                return null;
            }

            String textureData = json.substring(valueStart, valueEnd);
            MainScript.getInstance().getLogger().info("[HeadHunter] Extracted texture data length: " + textureData.length());

            return textureData;
        } catch (Exception e) {
            MainScript.getInstance().getLogger().warning("[HeadHunter] Error extracting texture data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static PlayerProfile createProfileFromTexture(String uuid, String playerName, String base64Texture) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Texture);
            String textureJson = new String(decoded);

            MainScript.getInstance().getLogger().info("[HeadHunter] Decoded texture JSON: " + textureJson);

            int urlIndex = textureJson.indexOf("\"url\"");
            if (urlIndex == -1) {
                MainScript.getInstance().getLogger().warning("[HeadHunter] Could not find 'url' field in texture data");
                return null;
            }

            int colonIndex = textureJson.indexOf(":", urlIndex);
            if (colonIndex == -1) {
                MainScript.getInstance().getLogger().warning("[HeadHunter] Could not find ':' after 'url' field");
                return null;
            }

            int urlStart = textureJson.indexOf("\"", colonIndex) + 1;
            int urlEnd = textureJson.indexOf("\"", urlStart);

            if (urlStart < 1 || urlEnd <= urlStart) {
                MainScript.getInstance().getLogger().warning("[HeadHunter] Could not parse skin URL from texture data");
                return null;
            }

            String skinUrl = textureJson.substring(urlStart, urlEnd);
            MainScript.getInstance().getLogger().info("[HeadHunter] Extracted skin URL: " + skinUrl);

            UUID playerUUID = UUID.fromString(
                    uuid.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")
            );

            MainScript.getInstance().getLogger().info("[HeadHunter] Creating profile with UUID: " + playerUUID);

            PlayerProfile profile = Bukkit.createPlayerProfile(playerUUID, playerName);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(skinUrl));
            profile.setTextures(textures);

            MainScript.getInstance().getLogger().info("[HeadHunter] Successfully created profile with skin");

            return profile;
        } catch (Exception e) {
            MainScript.getInstance().getLogger().warning(
                    "[HeadHunter] Failed to create profile from texture for " + playerName + ": " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    private static void givePlayerHead(Player player, String playerName, PlayerProfile profile) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwnerProfile(profile);
            meta.setDisplayName("§b" + playerName + "'s Head");

            List<String> lore = new ArrayList<>();
            lore.add("§7Spawned by: §e" + player.getName());
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, playerName);
            meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, "SPAWNED");
            meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, "PLAYER");
            meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, String.join("|", lore));

            head.setItemMeta(meta);
        }

        player.getInventory().addItem(head);
        player.sendMessage("§aSpawned " + playerName + "'s head!");
    }
}