package me.kanuunankuulaspluginhead.headHunter.Util;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;

import java.util.*;

public class VersionSafeEntityChecker {

    private static final Set<String> AVAILABLE_ENTITIES = new HashSet<>();
    private static String minecraftVersion = "unknown";

    public static void initialize() {
        detectMinecraftVersion();
        scanAvailableEntities();

        Bukkit.getLogger().info("[HeadHunter] Running on Minecraft " + minecraftVersion);
        Bukkit.getLogger().info("[HeadHunter] Detected " + AVAILABLE_ENTITIES.size() + " entity types");
    }

    public static boolean entityExists(String entityName) {
        return AVAILABLE_ENTITIES.contains(entityName.toUpperCase());
    }

    public static EntityType getEntityType(String name) {
        if (!entityExists(name)) {
            return null;
        }

        try {
            return EntityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isEntityAvailable(EntityType type) {
        if (type == null) return false;
        return AVAILABLE_ENTITIES.contains(type.name());
    }

    public static Set<String> getAvailableEntities() {
        return Collections.unmodifiableSet(AVAILABLE_ENTITIES);
    }

    private static void detectMinecraftVersion() {
        try {
            String version = Bukkit.getVersion();
            if (version.contains("MC: ")) {
                int start = version.indexOf("MC: ") + 4;
                int end = version.indexOf(")", start);
                if (end > start) {
                    minecraftVersion = version.substring(start, end);
                }
            }
        } catch (Exception e) {
            minecraftVersion = "unknown";
        }
    }

    private static void scanAvailableEntities() {
        AVAILABLE_ENTITIES.clear();

        for (EntityType type : EntityType.values()) {
            try {
                if (type.getEntityClass() != null) {
                    AVAILABLE_ENTITIES.add(type.name());
                }
            } catch (Exception e) {
            }
        }
    }

    public static String getMinecraftVersion() {
        return minecraftVersion;
    }

    public static boolean isVersion1_21OrHigher() {
        try {
            String[] parts = minecraftVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                return major > 1 || (major == 1 && minor >= 21);
            }
        } catch (Exception e) {
        }
        return false;
    }
    public static boolean isVersion1_21_10OrHigher() {
        try {
            String[] parts = minecraftVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int version = Integer.parseInt(parts[2]);
                return major > 1 || (major == 1 && minor >= 21 && version >= 10);
            }
        } catch (Exception e) {
        }
        return false;
    }
    public static boolean isVersion1_21_11OrHigher() {
        try {
            String[] parts = minecraftVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int version = Integer.parseInt(parts[2]);
                return major > 1 || (major == 1 && minor >= 21 && version >= 11);
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean isVersion1_21_9OrHigher() {
        try {
            String[] parts = minecraftVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int version = Integer.parseInt(parts[2]);
                return major > 1 || (major == 1 && minor >= 21 && version >= 9);
            }
        } catch (Exception e) {
        }
        return false;
    }
    public static boolean isVersion1_21_6OrHigher() {
        try {
            String[] parts = minecraftVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int version = Integer.parseInt(parts[2]);
                return major > 1 || (major == 1 && minor >= 21 && version >= 6);
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean isVersion1_20OrHigher() {
        try {
            String[] parts = minecraftVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                return major > 1 || (major == 1 && minor >= 20);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}