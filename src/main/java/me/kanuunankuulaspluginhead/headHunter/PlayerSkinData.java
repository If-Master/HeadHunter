package me.kanuunankuulaspluginhead.headHunter;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static me.kanuunankuulaspluginhead.headHunter.MainScript.*;

public class PlayerSkinData {

    private static boolean geyserAvailable = false;
    private static Object geyserApi = null;
    private static final Map<String, Boolean> registeredSkulls = new ConcurrentHashMap<>();

    static {
        try {
            Class<?> geyserClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            geyserApi = geyserClass.getMethod("api").invoke(null);
            geyserAvailable = true;
            Bukkit.getLogger().info("[HeadHunter] Geyser API detected - enabling Bedrock skull registration");
        } catch (Exception e) {
            geyserAvailable = false;
            Bukkit.getLogger().info("[HeadHunter] Geyser not found - Java-only mode");
        }
    }

    public static void createAndGivePlayerHeadWithSkin(Player player, String playerName) {
        boolean isBedrockPlayer = isBedrockPlayer(player);

        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer != null) {
            if (isBedrockPlayer) {
                createBedrockCompatibleHead(player, playerName, targetPlayer.getPlayerProfile());
            } else {
                createAndGivePlayerHead(player, playerName);
            }
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            if (isBedrockPlayer) {
                fetchPlayerProfileAndCreateHead(player, playerName, isBedrockPlayer);
            } else {
                createAndGivePlayerHead(player, playerName);
            }
            return;
        }

        player.sendMessage("§7Fetching player skin...");
        fetchPlayerSkinAsync(playerName, (success, profile) -> {
            runTask(player.getLocation(), () -> {
                if (success && profile != null) {
                    if (isBedrockPlayer) {
                        createBedrockCompatibleHead(player, playerName, profile);
                    } else {
                        createAndGivePlayerHeadWithProfile(player, playerName, profile);
                    }
                } else {
                    player.sendMessage("§cCouldn't fetch skin for " + playerName + ". Using default skin.");
                    createAndGivePlayerHead(player, playerName);
                }
            });
        });
    }

    private static void fetchPlayerProfileAndCreateHead(Player player, String playerName, boolean isBedrockPlayer) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(offlinePlayer.getUniqueId(), playerName);
            if (profile.getTextures().getSkin() != null) {
                if (isBedrockPlayer) {
                    createBedrockCompatibleHead(player, playerName, profile);
                } else {
                    createAndGivePlayerHeadWithProfile(player, playerName, profile);
                }
                return;
            }
        } catch (Exception e) {
        }

        player.sendMessage("§7Fetching player skin...");
        fetchPlayerSkinAsync(playerName, (success, profile) -> {
            runTask(player.getLocation(), () -> {
                if (success && profile != null) {
                    if (isBedrockPlayer) {
                        createBedrockCompatibleHead(player, playerName, profile);
                    } else {
                        createAndGivePlayerHeadWithProfile(player, playerName, profile);
                    }
                } else {
                    player.sendMessage("§cCouldn't fetch skin for " + playerName + ". Using default skin.");
                    createAndGivePlayerHead(player, playerName);
                }
            });
        });
    }

    private static void createBedrockCompatibleHead(Player player, String playerName, PlayerProfile profile) {
        if (!geyserAvailable) {
            createAndGivePlayerHeadWithProfile(player, playerName, profile);
            return;
        }

        String skinUrl = getSkinUrlFromProfile(profile);
        if (skinUrl != null) {
            registerSkullWithGeyser(playerName, skinUrl, () -> {
                ItemStack head = createRegisteredSkullItem(player, playerName, profile);
                player.getInventory().addItem(head);
                player.sendMessage("§aSpawned " + playerName + "'s head (Bedrock compatible)!");
            });
        } else {
            createAndGivePlayerHeadWithProfile(player, playerName, profile);
        }
    }

    private static void registerSkullWithGeyser(String playerName, String skinUrl, Runnable callback) {
        String skullId = "headhunter_" + playerName.toLowerCase();

        if (registeredSkulls.containsKey(skullId)) {
            callback.run();
            return;
        }

        try {
            Class<?> geyserApiClass = geyserApi.getClass();
            Object skullManager = geyserApiClass.getMethod("skullManager").invoke(geyserApi);

            if (skullManager != null) {
                Class<?> skullManagerClass = skullManager.getClass();

                Object skullData = createSkullData(skullId, skinUrl, playerName);

                if (skullData != null) {
                    skullManagerClass.getMethod("registerSkull", Object.class).invoke(skullManager, skullData);
                    registeredSkulls.put(skullId, Boolean.valueOf(true));

                    Bukkit.getLogger().info("[HeadHunter] Registered skull for Bedrock: " + playerName);
                }
            }

            callback.run();

        } catch (Exception e) {
            Bukkit.getLogger().info("[HeadHunter] Failed to register skull with Geyser: " + e.getMessage()+ "! Geyser doesn't really support this yet meaning there's nothing I can do about it sorry :| ");
            callback.run();
        }
    }

    private static Object createSkullData(String skullId, String skinUrl, String playerName) {
        try {
            Class<?> skullDataClass = Class.forName("org.geysermc.geyser.api.skull.SkullData");

            Object builder = skullDataClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();

            builder = builderClass.getMethod("textureUrl", String.class).invoke(builder, skinUrl);
            builder = builderClass.getMethod("skullUuid", UUID.class).invoke(builder,
                    UUID.nameUUIDFromBytes(skullId.getBytes()));

            return builderClass.getMethod("build").invoke(builder);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[HeadHunter] Could not create skull data: " + e.getMessage());
            return null;
        }
    }

    private static ItemStack createRegisteredSkullItem(Player player, String playerName, PlayerProfile profile) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwnerProfile(profile);

            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(MainScript.instance, "geyser_skull_id"),
                    PersistentDataType.STRING,
                    "headhunter_" + playerName.toLowerCase()
            );

            setHeadMetadata(meta, playerName, player.getName(), "PLAYER");
            head.setItemMeta(meta);
        }
        return head;
    }

    private static String getSkinUrlFromProfile(PlayerProfile profile) {
        try {
            PlayerTextures textures = profile.getTextures();
            if (textures.getSkin() != null) {
                return textures.getSkin().toString();
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static boolean isBedrockPlayer(Player player) {
        if (!geyserAvailable) return false;

        try {
            Class<?> geyserApiClass = geyserApi.getClass();
            Object connectionManager = geyserApiClass.getMethod("connectionManager").invoke(geyserApi);
            Class<?> connectionManagerClass = connectionManager.getClass();
            Object connection = connectionManagerClass.getMethod("getConnectionByUuid", UUID.class)
                    .invoke(connectionManager, player.getUniqueId());
            return connection != null;
        } catch (Exception e) {
            return player.getUniqueId().toString().startsWith("00000000-0000-0000-0009");
        }
    }

    private static void fetchPlayerSkinAsync(String playerName, BiConsumer<Boolean, PlayerProfile> callback) {
        String lowerName = playerName.toLowerCase();

        Long timestamp = SKIN_CACHE_TIMESTAMPS.get(lowerName);
        if (timestamp != null && System.currentTimeMillis() - timestamp < SKIN_CACHE_EXPIRY) {
            PlayerProfile cachedProfile = SKIN_CACHE.get(lowerName);
            if (cachedProfile != null) {
                callback.accept(Boolean.valueOf(true), cachedProfile);
                return;
            }
        }

        runAsync(() -> {
            try {
                Bukkit.getLogger().info("Starting skin fetch for: " + playerName);

                String uuid = getUUIDFromMojang(playerName);
                if (uuid == null) {
                    Bukkit.getLogger().warning("Failed to get UUID for: " + playerName);
                    callback.accept(Boolean.valueOf(false), null);
                    return;
                }

                PlayerProfile profile = getProfileFromMojang(uuid, playerName);

                if (profile != null) {
                    SKIN_CACHE.put(lowerName, profile);
                    SKIN_CACHE_TIMESTAMPS.put(lowerName, Long.valueOf(System.currentTimeMillis()));
                } else {
                    Bukkit.getLogger().warning("Failed to get profile for: " + playerName);
                }

                callback.accept(Boolean.valueOf(profile != null), profile);

            } catch (Exception e) {
                e.printStackTrace();
                callback.accept(Boolean.valueOf(false), null);
            }
        });
    }

    private static String getUUIDFromMojang(String playerName) {
        try {
            String apiUrl = "https://api.mojang.com/users/profiles/minecraft/" + playerName;

            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "HeadHunter-Plugin");

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                Bukkit.getLogger().warning("Failed to get UUID for " + playerName + ". Response code: " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseStr = response.toString();

            if (responseStr.contains("\"id\"")) {
                int idIndex = responseStr.indexOf("\"id\"");
                int colonIndex = responseStr.indexOf(":", idIndex);
                int start = responseStr.indexOf("\"", colonIndex) + 1;
                int end = responseStr.indexOf("\"", start);

                if (start > 0 && end > start) {
                    String uuid = responseStr.substring(start, end);
                    return uuid;
                }
            }

            Bukkit.getLogger().warning("No UUID found in response for " + playerName);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static PlayerProfile getProfileFromMojang(String uuid, String playerName) {
        try {
            String apiUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false";

            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "HeadHunter-Plugin");

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                Bukkit.getLogger().warning("Failed to get profile for " + playerName + ". Response code: " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseStr = response.toString();

            String textureData = extractTextureData(responseStr);
            if (textureData != null) {
                return createProfileFromTextureData(uuid, playerName, textureData);
            } else {
                Bukkit.getLogger().warning("No texture data found in profile response for " + playerName);
            }

            return null;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Exception getting profile for " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String extractTextureData(String profileJson) {
        try {
            if (profileJson.contains("\"textures\"") && profileJson.contains("\"value\"")) {
                int texturesIndex = profileJson.indexOf("\"textures\"");
                if (texturesIndex == -1) return null;

                int valueIndex = profileJson.indexOf("\"value\"", texturesIndex);
                if (valueIndex == -1) return null;

                int colonIndex = profileJson.indexOf(":", valueIndex);
                if (colonIndex == -1) return null;

                int start = profileJson.indexOf("\"", colonIndex) + 1;
                if (start == 0) return null;

                int end = profileJson.indexOf("\"", start);
                if (end == -1) return null;

                return profileJson.substring(start, end);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static PlayerProfile createProfileFromTextureData(String uuid, String playerName, String base64TextureData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64TextureData);
            String textureJson = new String(decoded);

            String skinUrl = extractSkinUrl(textureJson);
            if (skinUrl == null) {
                Bukkit.getLogger().warning("No skin URL found in texture data for " + playerName);
                return null;
            }

            UUID playerUUID = UUID.fromString(uuid.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
            ));

            PlayerProfile profile = Bukkit.createPlayerProfile(playerUUID, playerName);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(skinUrl));

            String capeUrl = extractCapeUrl(textureJson);
            if (capeUrl != null) {
                try {
                    textures.setCape(new URL(capeUrl));
                } catch (Exception e) {
                }
            }

            profile.setTextures(textures);
            return profile;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to create profile from texture data for " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String extractSkinUrl(String textureJson) {
        try {
            if (textureJson.contains("\"SKIN\"") && textureJson.contains("\"url\"")) {
                int skinIndex = textureJson.indexOf("\"SKIN\"");
                if (skinIndex == -1) return null;

                int urlIndex = textureJson.indexOf("\"url\"", skinIndex);
                if (urlIndex == -1) return null;

                int colonIndex = textureJson.indexOf(":", urlIndex);
                if (colonIndex == -1) return null;

                int start = textureJson.indexOf("\"", colonIndex) + 1;
                if (start == 0) return null;

                int end = textureJson.indexOf("\"", start);
                if (end == -1) return null;

                return textureJson.substring(start, end);
            }
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Exception extracting skin URL: " + e.getMessage());
            return null;
        }
    }

    private static String extractCapeUrl(String textureJson) {
        try {
            if (textureJson.contains("\"CAPE\"") && textureJson.contains("\"url\"")) {
                int capeIndex = textureJson.indexOf("\"CAPE\"");
                if (capeIndex == -1) return null;

                int urlIndex = textureJson.indexOf("\"url\"", capeIndex);
                if (urlIndex == -1) return null;

                int colonIndex = textureJson.indexOf(":", urlIndex);
                if (colonIndex == -1) return null;

                int start = textureJson.indexOf("\"", colonIndex) + 1;
                if (start == 0) return null;

                int end = textureJson.indexOf("\"", start);
                if (end == -1) return null;

                return textureJson.substring(start, end);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void setHeadMetadata(SkullMeta meta, String headOwner, String spawner, String headType) {
        meta.setDisplayName("§b" + headOwner + "'s Head");

        List<String> lore = new ArrayList<>();
        lore.add("§7Spawned by: §e" + spawner);
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, headOwner);
        meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, "SPAWNED");
        meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, headType);

        String loreData = String.join("|", lore);
        meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);
    }

    private static void createAndGivePlayerHeadWithProfile(Player player, String playerName, PlayerProfile profile) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwnerProfile(profile);
            setHeadMetadata(meta, playerName, player.getName(), "PLAYER");
            head.setItemMeta(meta);
        }

        player.getInventory().addItem(head);
        player.sendMessage("§aSpawned " + playerName + "'s head!");
    }
}
