package me.kanuunankuulaspluginhead.headHunter.Texture;

import me.kanuunankuulaspluginhead.headHunter.Texture.Enum.EntityHeadVariants;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.entity.*;
import org.bukkit.profile.PlayerProfile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTextureManager {
    private static final Map<EntityType, List<EntityHeadVariants>> ENTITY_VARIANTS = new ConcurrentHashMap<>();
    private static final Set<EntityType> KILLABLE_ENTITIES = EnumSet.noneOf(EntityType.class);

    private static final Set<String> EXCLUDED_KEYWORDS = new HashSet<>(Arrays.asList(
            "ARMOR_STAND", "ITEM_FRAME", "GLOW_ITEM_FRAME", "PAINTING", "LEASH_KNOT",
            "MARKER", "INTERACTION", "DISPLAY", "BOAT", "CHEST_BOAT", "MINECART",
            "TNT", "FALLING_BLOCK", "PRIMED_TNT", "ARROW", "SPECTRAL_ARROW",
            "TRIDENT", "FIREWORK", "EXPERIENCE_ORB", "AREA_EFFECT_CLOUD",
            "EGG", "ENDER_PEARL", "SNOWBALL", "FISHING_HOOK", "LIGHTNING"
    ));

    public static void initialize() {
        scanAllEntities();
        mapEntityVariants();

        int totalVariants = ENTITY_VARIANTS.values().stream().mapToInt(List::size).sum();
        Bukkit.getLogger().info("[HeadHunter] Mapped " + ENTITY_VARIANTS.size() +
                " entity types with " + totalVariants + " total variants");
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
            return entityClass != null && org.bukkit.entity.LivingEntity.class.isAssignableFrom(entityClass);
        } catch (Exception e) {
            return false;
        }
    }

    private static void mapEntityVariants() {
        ENTITY_VARIANTS.clear();

        for (EntityHeadVariants variant : EntityHeadVariants.values()) {
            try {
                String baseName = variant.getName();
                EntityType type = EntityType.valueOf(baseName);

                if (isKillableEntity(type)) {
                    ENTITY_VARIANTS.computeIfAbsent(type, k -> new ArrayList<>()).add(variant);
                }
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public static PlayerProfile getOrCreateProfile(EntityType type, String profileName, Entity entity) {
        List<EntityHeadVariants> variants = ENTITY_VARIANTS.get(type);

        if (variants == null || variants.isEmpty()) {
            return null;
        }

        if (variants.size() == 1) {
            return variants.get(0).getSkullProfile();
        }

        EntityHeadVariants variant = detectVariant(type, entity, variants);

        try {
            return variant.getSkullProfile();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HeadHunter] Failed to create profile for " + type + ": " + e.getMessage());
            return null;
        }
    }

    private static EntityHeadVariants detectVariant(EntityType type, Entity entity, List<EntityHeadVariants> variants) {
        if (entity == null) {
            return variants.get(0);
        }

        try {
            EntityHeadVariants detected = null;

            switch (type) {
                case WOLF:
                    detected = detectWolfVariant((Wolf) entity, variants);
                    break;
                case CAT:
                    detected = detectCatVariant((Cat) entity, variants);
                    break;
                case SHEEP:
                    detected = detectSheepVariant((Sheep) entity, variants);
                    break;
                case AXOLOTL:
                    detected = detectAxolotlVariant((Axolotl) entity, variants);
                    break;
                case FOX:
                    detected = detectFoxVariant((Fox) entity, variants);
                    break;
                case LLAMA:
                    detected = detectLlamaVariant((Llama) entity, variants);
                    break;
                case TRADER_LLAMA:
                    detected = detectTraderLlamaVariant((TraderLlama) entity, variants);
                    break;
                case HORSE:
                    detected = detectHorseVariant((Horse) entity, variants);
                    break;
                case RABBIT:
                    detected = detectRabbitVariant((Rabbit) entity, variants);
                    break;
                case PARROT:
                    detected = detectParrotVariant((Parrot) entity, variants);
                    break;
                case TROPICAL_FISH:
                    detected = detectTropicalFishVariant((TropicalFish) entity, variants);
                    break;
                case MOOSHROOM:
                    detected = detectMooshroomVariant((MushroomCow) entity, variants);
                    break;
                case PANDA:
                    detected = detectPandaVariant((Panda) entity, variants);
                    break;
                case VILLAGER:
                    detected = detectVillagerVariant((Villager) entity, variants);
                    break;
                case ZOMBIE_VILLAGER:
                    detected = detectZombieVillagerVariant((ZombieVillager) entity, variants);
                    break;
                case FROG:
                    detected = detectFrogVariant((Frog) entity, variants);
                    break;
                case BEE:
                    detected = detectBeeVariant((Bee) entity, variants);
                    break;
                case VEX:
                    detected = detectVexVariant((Vex) entity, variants);
                    break;
                case STRIDER:
                    detected = detectStriderVariant((Strider) entity, variants);
                    break;
                case COW:
                case PIG:
                case CHICKEN:
                    detected = detectBiomeVariant(entity, variants);
                    break;
                default:
                    detected = variants.get(0);
            }

            if (detected != null) {
                Bukkit.getLogger().fine("[HeadHunter] Detected variant " + detected.name() +
                        " for " + type + " at biome " +
                        entity.getLocation().getBlock().getBiome().name());
            }

            return detected != null ? detected : variants.get(0);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[HeadHunter] Error detecting variant for " + type + ": " + e.getMessage());
            return variants.get(0);
        }
    }

    private static EntityHeadVariants detectWolfVariant(Wolf wolf, List<EntityHeadVariants> variants) {
        boolean angry = wolf.isAngry();

        try {
            Wolf.Variant wolfVariant = wolf.getVariant();

            String variantKey = wolfVariant.getKey().getKey().toUpperCase();
            String variantName = "WOLF_" + variantKey + (angry ? "_ANGRY" : "");


            return variants.stream()
                    .filter(v -> v.name().equals(variantName))
                    .findFirst()
                    .orElse(variants.get(0));
        } catch (Exception e) {
            String variantName = angry ? "WOLF_PALE_ANGRY" : "WOLF_PALE";
            return variants.stream()
                    .filter(v -> v.name().equals(variantName))
                    .findFirst()
                    .orElse(variants.get(0));
        }
    }
    private static EntityHeadVariants detectCatVariant(Cat cat, List<EntityHeadVariants> variants) {
        Cat.Type catType = cat.getCatType();
        String variantName = "CAT_" + catType.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectSheepVariant(Sheep sheep, List<EntityHeadVariants> variants) {
        DyeColor color = sheep.getColor();
        String variantName = "SHEEP_" + color.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectAxolotlVariant(Axolotl axolotl, List<EntityHeadVariants> variants) {
        Axolotl.Variant axolotlVariant = axolotl.getVariant();
        String variantName = "AXOLOTL_" + axolotlVariant.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectFoxVariant(Fox fox, List<EntityHeadVariants> variants) {
        Fox.Type foxType = fox.getFoxType();
        String variantName = foxType == Fox.Type.SNOW ? "FOX_WHITE" : "FOX";

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectLlamaVariant(Llama llama, List<EntityHeadVariants> variants) {
        Llama.Color llamaColor = llama.getColor();
        String variantName = "LLAMA_" + llamaColor.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectTraderLlamaVariant(TraderLlama llama, List<EntityHeadVariants> variants) {
        try {
            Llama.Color llamaColor = llama.getColor();
            String variantName = "TRADER_LLAMA_" + llamaColor.name();

            return variants.stream()
                    .filter(v -> v.name().equals(variantName))
                    .findFirst()
                    .orElse(variants.get(0));
        } catch (Exception e) {
            return variants.get(0);
        }
    }

    private static EntityHeadVariants detectHorseVariant(Horse horse, List<EntityHeadVariants> variants) {
        Horse.Color horseColor = horse.getColor();
        String variantName = "HORSE_" + horseColor.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectRabbitVariant(Rabbit rabbit, List<EntityHeadVariants> variants) {
        Rabbit.Type rabbitType = rabbit.getRabbitType();
        String variantName = "RABBIT_" + rabbitType.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectParrotVariant(Parrot parrot, List<EntityHeadVariants> variants) {
        Parrot.Variant parrotVariant = parrot.getVariant();
        String variantName = "PARROT_" + parrotVariant.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectTropicalFishVariant(TropicalFish fish, List<EntityHeadVariants> variants) {
        DyeColor bodyColor = fish.getBodyColor();
        String variantName = "TROPICAL_FISH_" + bodyColor.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectMooshroomVariant(MushroomCow mooshroom, List<EntityHeadVariants> variants) {
        MushroomCow.Variant variant = mooshroom.getVariant();
        String variantName = "MOOSHROOM_COW_" + variant.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectPandaVariant(Panda panda, List<EntityHeadVariants> variants) {
        Panda.Gene mainGene = panda.getMainGene();
        if (mainGene == Panda.Gene.BROWN) {
            return variants.stream()
                    .filter(v -> v.name().equals("PANDA_BROWN"))
                    .findFirst()
                    .orElse(variants.get(0));
        }
        return variants.get(0);
    }

    private static EntityHeadVariants detectVillagerVariant(Villager villager, List<EntityHeadVariants> variants) {
        Villager.Profession profession = villager.getProfession();
        String variantName = "VILLAGER_" + profession.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectZombieVillagerVariant(ZombieVillager zombieVillager, List<EntityHeadVariants> variants) {
        Villager.Profession profession = zombieVillager.getVillagerProfession();
        String variantName = "ZOMBIE_VILLAGER_" + profession.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectFrogVariant(Frog frog, List<EntityHeadVariants> variants) {
        Frog.Variant frogVariant = frog.getVariant();
        String variantName = "FROG_" + frogVariant.name();

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectBeeVariant(Bee bee, List<EntityHeadVariants> variants) {
        boolean angry = bee.hasStung() || bee.getAnger() > 0;
        String variantName = angry ? "BEE_AWARE" : "BEE";

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectVexVariant(Vex vex, List<EntityHeadVariants> variants) {
        boolean charging = vex.isCharging();
        String variantName = charging ? "VEX_CHARGE" : "VEX";

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectStriderVariant(Strider strider, List<EntityHeadVariants> variants) {
        boolean shivering = strider.isShivering();
        String variantName = shivering ? "STRIDER_SHIVERING" : "STRIDER";

        return variants.stream()
                .filter(v -> v.name().equals(variantName))
                .findFirst()
                .orElse(variants.get(0));
    }

    private static EntityHeadVariants detectBiomeVariant(Entity entity, List<EntityHeadVariants> variants) {
        try {
            String biomeKey = entity.getLocation().getBlock().getBiome().getKey().getKey().toUpperCase();

            Bukkit.getLogger().info("[HeadHunter] Detecting biome variant for " + entity.getType() + " in biome: " + biomeKey);

            String variantSuffix;
            if (biomeKey.contains("COLD") || biomeKey.contains("FROZEN") || biomeKey.contains("ICE") ||
                    biomeKey.contains("SNOW") || biomeKey.contains("TAIGA")) {
                variantSuffix = "_COLD";
            } else if (biomeKey.contains("DESERT") || biomeKey.contains("SAVANNA") ||
                    biomeKey.contains("BADLANDS") || biomeKey.contains("JUNGLE")) {
                variantSuffix = "_WARM";
            } else {
                variantSuffix = "_TEMPERATE";
            }

            String variantName = entity.getType().name() + variantSuffix;

            Bukkit.getLogger().info("[HeadHunter] Biome " + biomeKey + " -> variant " + variantName);

            return variants.stream()
                    .filter(v -> v.name().equals(variantName))
                    .findFirst()
                    .orElse(variants.get(0));

        } catch (Exception e) {
            Bukkit.getLogger().warning("[HeadHunter] Error detecting biome variant: " + e.getMessage());
            return variants.get(0);
        }
    }
    public static Set<EntityType> getKillableEntities() {
        return Collections.unmodifiableSet(KILLABLE_ENTITIES);
    }

    public static boolean hasTexture(EntityType type) {
        return ENTITY_VARIANTS.containsKey(type);
    }

    public static List<EntityHeadVariants> getVariants(EntityType type) {
        return ENTITY_VARIANTS.getOrDefault(type, Collections.emptyList());
    }

    public static void clearCache() {
    }

    public static Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("killable_entities", KILLABLE_ENTITIES.size());
        info.put("entity_types_with_variants", ENTITY_VARIANTS.size());
        int totalVariants = ENTITY_VARIANTS.values().stream().mapToInt(List::size).sum();
        info.put("total_variants", totalVariants);
        info.put("entities_with_texture", ENTITY_VARIANTS.size());
        return info;
    }
}