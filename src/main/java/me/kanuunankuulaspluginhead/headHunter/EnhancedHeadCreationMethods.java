package me.kanuunankuulaspluginhead.headHunter;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;

import java.util.*;

public class EnhancedHeadCreationMethods {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null || entity instanceof Player) {
            return;
        }

        if (!MainScript.config.getBoolean("mob-head-drops-enabled", true)) {
            return;
        }

        EntityType type = entity.getType();

        if (!EntityTextureManager.isKillableEntity(type)) {
            return;
        }

        if (type == EntityType.MANNEQUIN) {
            handleMannequinDeath(entity, killer);
            return;
        }

        double baseChance = MainScript.config.getDouble("mob-head-drop-chances." + type.name().toLowerCase(), 0);

        if (baseChance <= 0) {
            baseChance = getDefaultDropChance(type);
        }

        double lootingBonus = 0;
        if (MainScript.config.getBoolean("looting-increases-drop-chance", true)) {
            int lootingLevel = killer.getInventory().getItemInMainHand().getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LOOTING);
            lootingBonus = lootingLevel * MainScript.config.getDouble("looting-bonus-per-level", 5.0);
        }

        double finalChance = baseChance + lootingBonus;

        if (new Random().nextDouble() * 100 < finalChance) {
            ItemStack head = createDynamicMobHead(entity, killer);
            if (head != null) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), head);
            }
        }
    }

    private static double getDefaultDropChance(EntityType type) {
        try {
            Class<?> entityClass = type.getEntityClass();
            if (entityClass == null) return 0;

            if (org.bukkit.entity.Boss.class.isAssignableFrom(entityClass)) {
                return MainScript.config.getDouble("default-boss-drop-chance", 100.0);
            }

            if (org.bukkit.entity.Monster.class.isAssignableFrom(entityClass)) {
                return MainScript.config.getDouble("default-hostile-drop-chance", 5.0);
            }

            return MainScript.config.getDouble("default-passive-drop-chance", 2.0);
        } catch (Exception e) {
            return 0;
        }
    }

    public static ItemStack createDynamicMobHead(LivingEntity entity, Player killer) {
        EntityType type = entity.getType();

        if (!EntityTextureManager.isKillableEntity(type)) {
            Bukkit.getLogger().warning("[HeadHunter] Attempted to create head for invalid entity: " + type);
            return null;
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta == null) {
            return null;
        }

        String displayName = MainScript.getDisplayName(type);
        String profileName = formatProfileName(type);
        String colorCode = MainScript.config.getString("entity-colors." + type.name().toLowerCase(),
                getDefaultColorCode(type));

        meta.setDisplayName(colorCode + displayName + " Head");

        List<String> lore = buildHeadLore(entity, killer, displayName);
        meta.setLore(lore);

        PlayerProfile profile = EntityTextureManager.getOrCreateProfile(type, profileName);
        if (profile != null) {
            meta.setOwnerProfile(profile);
        } else {
            Bukkit.getLogger().info("[HeadHunter] No texture available for " + type + ", using default");
        }

        String loreData = String.join("|", lore);
        meta.getPersistentDataContainer().set(MainScript.headOwnerKey, PersistentDataType.STRING, displayName);
        meta.getPersistentDataContainer().set(MainScript.killerKey, PersistentDataType.STRING, killer.getName());
        meta.getPersistentDataContainer().set(MainScript.headTypeKey, PersistentDataType.STRING, type.name());
        meta.getPersistentDataContainer().set(MainScript.loreKey, PersistentDataType.STRING, loreData);

        head.setItemMeta(meta);
        return head;
    }

    private static List<String> buildHeadLore(LivingEntity entity, Player killer, String displayName) {
        List<String> lore = new ArrayList<>();

        String loreFormat = MainScript.config.getString("mob-head-lore-format", "§7Killed by: §c%killer%");
        String loreText = loreFormat
                .replace("%killer%", killer.getName())
                .replace("%mob%", displayName)
                .replace("%world%", entity.getWorld().getName());

        lore.add(loreText);

        if (MainScript.config.getBoolean("show-kill-location", false)) {
            Location loc = entity.getLocation();
            lore.add(String.format("§7Location: %d, %d, %d",
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        }

        if (MainScript.config.getBoolean("show-kill-time", false)) {
            lore.add("§7Killed: " + new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm")
                    .format(new Date()));
        }

        if (entity.getCustomName() != null && MainScript.config.getBoolean("show-custom-name", true)) {
            lore.add("§7Named: " + entity.getCustomName());
        }

        return lore;
    }

    private static String formatProfileName(EntityType type) {
        String name = type.name().toLowerCase().replace("_", "");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private static String getDefaultColorCode(EntityType type) {
        try {
            Class<?> entityClass = type.getEntityClass();
            if (entityClass == null) return "§f";

            if (org.bukkit.entity.Boss.class.isAssignableFrom(entityClass)) {
                return "§4";
            }
            if (org.bukkit.entity.Monster.class.isAssignableFrom(entityClass)) {
                return "§c";
            }
            if (org.bukkit.entity.WaterMob.class.isAssignableFrom(entityClass)) {
                return "§b";
            }

            return "§a";
        } catch (Exception e) {
            return "§f";
        }
    }

    private static void handleMannequinDeath(LivingEntity mannequin, Player killer) {
        EntityType type = mannequin.getType();
        double chance = MainScript.config.getDouble("mob-head-drop-chances." + type.name().toLowerCase(), 0);

        if (new Random().nextDouble() * 100 >= chance) {
            return;
        }

        String profileName = mannequin.getCustomName();
        if (profileName != null) {
            profileName = ChatColor.stripColor(profileName);
        }

        if (profileName == null || !MainScript.config.getBoolean("allow_mannequin_skin", true)) {
            ItemStack head = createDynamicMobHead(mannequin, killer);
            if (head != null) {
                mannequin.getWorld().dropItemNaturally(mannequin.getLocation(), head);
            }
            return;
        }

        Location dropLocation = mannequin.getLocation();
        String finalProfileName = profileName;

        MainScript.runAsync(() -> {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                String colorCode = MainScript.config.getString("entity-colors.mannequin", "§9");
                meta.setDisplayName(colorCode + "Mannequin Head");

                try {
                    org.bukkit.profile.PlayerProfile profile = Bukkit.createPlayerProfile(finalProfileName);
                    profile.update().thenAccept(updatedProfile -> {
                        MainScript.runTask(dropLocation, () -> {
                            if (updatedProfile.getTextures().getSkin() != null) {
                                meta.setOwnerProfile(updatedProfile);
                            }

                            List<String> lore = new ArrayList<>();
                            lore.add("§7Mannequin of: " + finalProfileName);
                            lore.add("§7Killed by: §c" + killer.getName());
                            meta.setLore(lore);

                            String loreData = String.join("|", lore);
                            meta.getPersistentDataContainer().set(MainScript.headOwnerKey, PersistentDataType.STRING, "Mannequin");
                            meta.getPersistentDataContainer().set(MainScript.killerKey, PersistentDataType.STRING, killer.getName());
                            meta.getPersistentDataContainer().set(MainScript.headTypeKey, PersistentDataType.STRING, EntityType.MANNEQUIN.name());
                            meta.getPersistentDataContainer().set(MainScript.loreKey, PersistentDataType.STRING, loreData);
                            head.setItemMeta(meta);

                            dropLocation.getWorld().dropItemNaturally(dropLocation, head);
                        });
                    });
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[HeadHunter] Failed to fetch mannequin skin: " + e.getMessage());
                    ItemStack defaultHead = createDynamicMobHead(mannequin, killer);
                    MainScript.runTask(dropLocation, () -> {
                        if (defaultHead != null) {
                            dropLocation.getWorld().dropItemNaturally(dropLocation, defaultHead);
                        }
                    });
                }
            }
        });
    }
}