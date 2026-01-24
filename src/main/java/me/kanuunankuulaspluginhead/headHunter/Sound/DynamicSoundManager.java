package me.kanuunankuulaspluginhead.headHunter.Sound;

import me.kanuunankuulaspluginhead.headHunter.MainScript;
import me.kanuunankuulaspluginhead.headHunter.Texture.EntityTextureManager;
import me.kanuunankuulaspluginhead.headHunter.Texture.Enum.EntityHeadVariants;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicSoundManager {
    private static final Map<EntityType, List<Sound>> ENTITY_SOUNDS = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    private static boolean randomSoundsEnabled = true;

    public static void initialize() {
        Bukkit.getLogger().info("[HeadHunter] Initializing sound system...");
        randomSoundsEnabled = MainScript.config.getBoolean("head-sound-effects.random-sounds", true);

        loadSoundsFromVariants();
        scanAdditionalSounds();

        Bukkit.getLogger().info("[HeadHunter] Loaded sounds for " + ENTITY_SOUNDS.size() + " entity types");
    }

    private static void loadSoundsFromVariants() {
        for (EntityHeadVariants variant : EntityHeadVariants.values()) {
            try {
                EntityType type = EntityType.valueOf(variant.getName());
                String soundKey = variant.getSound();

                Sound sound = parseSoundKey(soundKey);
                if (sound != null) {
                    ENTITY_SOUNDS.computeIfAbsent(type, k -> new ArrayList<>()).add(sound);
                }
            } catch (IllegalArgumentException e) {
                // Entity doesn't exist in this version
            }
        }
    }

    private static Sound parseSoundKey(String soundKey) {
        try {
            // Convert "entity.pig.ambient" to "ENTITY_PIG_AMBIENT"
            String enumName = soundKey.toUpperCase().replace('.', '_');
            return Sound.valueOf(enumName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void scanAdditionalSounds() {
        for (EntityType type : EntityTextureManager.getKillableEntities()) {
            if (ENTITY_SOUNDS.containsKey(type)) {
                continue;
            }

            List<Sound> sounds = findSoundsForEntity(type);
            if (!sounds.isEmpty()) {
                ENTITY_SOUNDS.put(type, sounds);
            }
        }
    }

    private static List<Sound> findSoundsForEntity(EntityType type) {
        List<Sound> sounds = new ArrayList<>();
        String entityName = type.name().toUpperCase();

        for (Sound sound : Sound.values()) {
            try {
                String soundName = sound.name();

                if (soundName.startsWith("ENTITY_" + entityName + "_")) {
                    String suffix = soundName.substring(("ENTITY_" + entityName + "_").length());

                    if (isRelevantSoundType(suffix)) {
                        sounds.add(sound);
                    }
                }
            } catch (Exception e) {
                // Skip invalid sounds
            }
        }

        return sounds;
    }

    private static boolean isRelevantSoundType(String suffix) {
        Set<String> relevantTypes = new HashSet<>(Arrays.asList(
                "AMBIENT", "IDLE", "HURT", "DEATH", "STEP", "ATTACK",
                "GROWL", "HOWL", "PURR", "SCREAM", "ROAR", "CELEBRATE",
                "SNEEZE", "SNIFF", "PLAY", "LOOP", "PANT"
        ));

        for (String type : relevantTypes) {
            if (suffix.contains(type)) {
                return true;
            }
        }
        return false;
    }

    public static Sound getSound(EntityType type) {
        if (randomSoundsEnabled) {
            return getRandomSound(type);
        } else {
            return getPrimarySound(type);
        }
    }

    public static Sound getRandomSound(EntityType type) {
        List<Sound> sounds = ENTITY_SOUNDS.get(type);
        if (sounds == null || sounds.isEmpty()) {
            return null;
        }
        return sounds.get(random.nextInt(sounds.size()));
    }

    public static Sound getPrimarySound(EntityType type) {
        List<Sound> sounds = ENTITY_SOUNDS.get(type);
        if (sounds == null || sounds.isEmpty()) {
            return null;
        }
        return sounds.get(0);
    }

    public static List<Sound> getAllSounds(EntityType type) {
        return ENTITY_SOUNDS.getOrDefault(type, Collections.emptyList());
    }

    public static boolean hasSound(EntityType type) {
        return ENTITY_SOUNDS.containsKey(type) && !ENTITY_SOUNDS.get(type).isEmpty();
    }

    public static Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("entity_types_with_sounds", ENTITY_SOUNDS.size());
        info.put("random_sounds_enabled", randomSoundsEnabled);
        info.put("total_sounds", ENTITY_SOUNDS.values().stream().mapToInt(List::size).sum());
        return info;
    }
}