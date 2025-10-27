package me.kanuunankuulaspluginhead.headHunter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTextureManager {
    private static final Map<EntityType, PlayerProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Set<EntityType> KILLABLE_ENTITIES = EnumSet.noneOf(EntityType.class);
    private static final Map<EntityType, String> AUTO_GENERATED_TEXTURES = new ConcurrentHashMap<>();

    private static final Set<String> EXCLUDED_KEYWORDS = new HashSet<>(Arrays.asList(
            "ARMOR_STAND", "ITEM_FRAME", "GLOW_ITEM_FRAME", "PAINTING", "LEASH_KNOT",
            "MARKER", "INTERACTION", "DISPLAY", "BOAT", "CHEST_BOAT", "MINECART",
            "TNT", "FALLING_BLOCK", "PRIMED_TNT", "ARROW", "SPECTRAL_ARROW",
            "TRIDENT", "FIREWORK", "EXPERIENCE_ORB", "AREA_EFFECT_CLOUD",
            "EGG", "ENDER_PEARL", "SNOWBALL", "FISHING_HOOK", "LIGHTNING"
    ));

    public static void initialize() {
        scanAllEntities();
        generateMissingTextures();
    }

    private static void scanAllEntities() {
        KILLABLE_ENTITIES.clear();

        for (EntityType type : EntityType.values()) {
            if (isKillableEntity(type)) {
                KILLABLE_ENTITIES.add(type);
            }
        }
    }

    public static boolean isKillableEntity(EntityType type) {
        if (type == null || !type.isAlive()) {
            return false;
        }

        String typeName = type.name();
        for (String keyword : EXCLUDED_KEYWORDS) {
            if (typeName.contains(keyword)) {
                return false;
            }
        }

        try {
            Class<?> entityClass = type.getEntityClass();
            if (entityClass == null) return false;

            boolean isLiving = LivingEntity.class.isAssignableFrom(entityClass);

            try {
                boolean isMonster = Entity.class.isAssignableFrom(entityClass);
                if (isMonster) return true;
            } catch (NoClassDefFoundError e) {
            }

            return isLiving;
        } catch (Exception e) {
            return false;
        }
    }

    private static void generateMissingTextures() {
        AUTO_GENERATED_TEXTURES.clear();

        for (EntityType type : KILLABLE_ENTITIES) {
            if (!MainScript.mobTextures.containsKey(type)) {
                String texture = attemptTextureGeneration(type);
                if (texture != null) {
                    AUTO_GENERATED_TEXTURES.put(type, texture);
                }
            }
        }
    }

    private static String attemptTextureGeneration(EntityType type) {
        String entityName = type.name().toLowerCase();

        Map<String, String> knownPatterns = new HashMap<>();
        knownPatterns.put("_skeleton", "http://textures.minecraft.net/texture/301268e9c492da1f0d88271cb492a4b302395f515a7bbf77f4a20b95fc02eb2");
        knownPatterns.put("_zombie", "http://textures.minecraft.net/texture/56fc854bb84cf4b7697297973e02b79bc10698460b51a639c60e5e417734e11");
        knownPatterns.put("piglin", "http://textures.minecraft.net/texture/7eabaecc5fae5a8a49c8863ff4831aaa284198f1a2398890c765e0a8de18da8c");
        knownPatterns.put("villager", "http://textures.minecraft.net/texture/350ba6b7690363737a46c85a2795058ff166eb20a7b5c9fa3fb2b391199822d2");

        for (Map.Entry<String, String> pattern : knownPatterns.entrySet()) {
            if (entityName.contains(pattern.getKey())) {
                return pattern.getValue();
            }
        }

        String baseTexture = "http://textures.minecraft.net/texture/";
        String hash = generateConsistentHash(entityName);
        return baseTexture + hash;
    }

    private static String generateConsistentHash(String input) {
        UUID uuid = UUID.nameUUIDFromBytes(input.getBytes());
        return uuid.toString().replace("-", "");
    }

    public static PlayerProfile getOrCreateProfile(EntityType type, String profileName) {
        if (PROFILE_CACHE.containsKey(type)) {
            return PROFILE_CACHE.get(type);
        }

        String textureUrl = getTextureUrl(type);
        if (textureUrl == null) {
            return null;
        }

        try {
            UUID profileUuid = UUID.nameUUIDFromBytes(("mobhead_" + type.name()).getBytes());
            PlayerProfile profile = Bukkit.createPlayerProfile(profileUuid, profileName);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);

            PROFILE_CACHE.put(type, profile);
            return profile;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HeadHunter] Failed to create profile for " + type + ": " + e.getMessage());
            return null;
        }
    }

    private static String getTextureUrl(EntityType type) {
        if (MainScript.mobTextures.containsKey(type)) {
            return MainScript.mobTextures.get(type);
        }
        return AUTO_GENERATED_TEXTURES.get(type);
    }

    public static Set<EntityType> getKillableEntities() {
        return Collections.unmodifiableSet(KILLABLE_ENTITIES);
    }

    public static boolean hasTexture(EntityType type) {
        return MainScript.mobTextures.containsKey(type) || AUTO_GENERATED_TEXTURES.containsKey(type);
    }

    public static void clearCache() {
        PROFILE_CACHE.clear();
    }

    public static Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("killable_entities", KILLABLE_ENTITIES.size());
        info.put("predefined_textures", MainScript.mobTextures.size());
        info.put("generated_textures", AUTO_GENERATED_TEXTURES.size());
        info.put("cached_profiles", PROFILE_CACHE.size());
        info.put("entities_with_texture", (int) KILLABLE_ENTITIES.stream()
                .filter(EntityTextureManager::hasTexture).count());
        return info;
    }
}