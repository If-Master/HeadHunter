package me.kanuunankuulaspluginhead.headHunter;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DynamicSoundManager {
    private static final Map<EntityType, List<Sound>> ENTITY_SOUNDS = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    private static boolean randomSoundsEnabled = true;

    public static void initialize() {
        Bukkit.getLogger().info("[HeadHunter] Scanning for entity sounds dynamically...");
        randomSoundsEnabled = MainScript.config.getBoolean("head-sound-effects.random-sounds", true);

        scanAllEntitySounds();
        loadManualSoundMappings();

        Bukkit.getLogger().info("[HeadHunter] Loaded sounds for " + ENTITY_SOUNDS.size() + " entity types");
    }

    private static void scanAllEntitySounds() {
        int scannedCount = 0;
        int soundsFoundCount = 0;

        for (EntityType type : EntityTextureManager.getKillableEntities()) {
            if (!VersionSafeEntityChecker.isEntityAvailable(type)) {
                continue;
            }

            scannedCount++;
            List<Sound> sounds = findSoundsForEntity(type);

            if (!sounds.isEmpty()) {
                ENTITY_SOUNDS.put(type, sounds);
                soundsFoundCount++;
            }
        }

        Bukkit.getLogger().info("[HeadHunter] Scanned " + scannedCount + " entities, found sounds for " + soundsFoundCount);
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
                        try {
                            sound.getKey();
                            sounds.add(sound);
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
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

    private static void loadManualSoundMappings() {
        addManualSounds(EntityType.PLAYER, Arrays.asList(
                Sound.ENTITY_PLAYER_BREATH, Sound.ENTITY_PLAYER_HURT, Sound.ENTITY_PLAYER_BURP
        ));

        addManualSounds(EntityType.CREEPER, Arrays.asList(
                Sound.ENTITY_CREEPER_PRIMED, Sound.ENTITY_CREEPER_HURT
        ));

        for (EntityType type : EntityTextureManager.getKillableEntities()) {
            Sound legacySound = AnimalData.getAnimalSound(type);
            if (legacySound != null && !ENTITY_SOUNDS.containsKey(type)) {
                ENTITY_SOUNDS.put(type, Collections.singletonList(legacySound));
            }
        }
    }

    private static void addManualSounds(EntityType type, List<Sound> sounds) {
        ENTITY_SOUNDS.merge(type, sounds, (existing, newSounds) -> {
            List<Sound> merged = new ArrayList<>(existing);
            merged.addAll(newSounds);
            return merged.stream().distinct().collect(Collectors.toList());
        });
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