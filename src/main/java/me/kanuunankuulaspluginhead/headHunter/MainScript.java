package me.kanuunankuulaspluginhead.headHunter;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.inventory.AnvilInventory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static me.kanuunankuulaspluginhead.headHunter.PlayerSkinData.createAndGivePlayerHeadWithSkin;

public class MainScript extends JavaPlugin implements Listener, TabCompleter {
    // Version Data
    public String latestVersion = getDescription().getVersion();
    public String currentversion = getDescription().getVersion();
    public static String getCurrentVersion() {
        return instance != null ? instance.currentversion : "Unknown";
    }

    // Folia, files & Times
    private static boolean isFolia = false;

    private File notificationsFile;
    public File soundScript;
    static MainScript instance;
    public static FileConfiguration config;

    private static final long CACHE_EXPIRY = 300000;


    // Booleans
    private boolean isUpdateAvailable = false;
    private boolean playerHeadDropsEnabled;
    private boolean mobHeadDropsEnabled;
    private volatile boolean flushScheduled = false;
    private boolean allowMannequinSkin;


    // Others
    private final Random random = new Random();
    private ValidationMode validationMode;

    // Head data
    public static NamespacedKey killerKey;
    public static NamespacedKey killTimeKey;
    public static NamespacedKey loreKey;
    public static NamespacedKey headOwnerKey;
    public static NamespacedKey headTypeKey;

    // Data Maps
    private static final Map<EntityType, String> DISPLAY_NAME_CACHE = new EnumMap<>(EntityType.class);
    private static final Map<EntityType, String> PROFILE_NAME_CACHE = new EnumMap<>(EntityType.class);
    private static final Map<EntityType, String> COLOR_CODE_CACHE = new EnumMap<>(EntityType.class);
    private static final Map<String, EntityType> ENTITY_TYPE_LOOKUP = new HashMap<>();
    private static final Map<String, Boolean> PLAYER_EXISTENCE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> CACHE_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Map<String, UUID> UUID_CACHE = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> queuedHeadsMemory = new ConcurrentHashMap<>();
    private final Map<String, UUID> mobHeadUUIDs = new HashMap<>();
    private Map<UUID, Boolean> playerNotificationPreferences = new HashMap<>();
    public static final Map<EntityType, String> mobTextures = new HashMap<>();
    public static final Map<String, PlayerProfile> SKIN_CACHE = new ConcurrentHashMap<>();
    public static final Map<String, Long> SKIN_CACHE_TIMESTAMPS = new ConcurrentHashMap<>();
    public static final long SKIN_CACHE_EXPIRY = 3600000;

    public enum ValidationMode {
        SERVER_ONLY,
        MOJANG_API,
        DISABLED
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack firstItem = inventory.getItem(0);

        if (firstItem != null && firstItem.getType() == Material.PLAYER_HEAD && firstItem.hasItemMeta()) {
            SkullMeta meta = (SkullMeta) firstItem.getItemMeta();
            if (meta.getPersistentDataContainer().has(headOwnerKey, PersistentDataType.STRING)) {
                event.setResult(null);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
                if (block.getState() instanceof Skull skull) {
                    return skull.getPersistentDataContainer().has(headOwnerKey, PersistentDataType.STRING);
                }
            }
            return false;
        });
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
                if (block.getState() instanceof Skull skull) {
                    return skull.getPersistentDataContainer().has(headOwnerKey, PersistentDataType.STRING);
                }
            }
            return false;
        });
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
                if (block.getState() instanceof Skull skull) {
                    if (skull.getPersistentDataContainer().has(headOwnerKey, PersistentDataType.STRING)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
                if (block.getState() instanceof Skull skull) {
                    if (skull.getPersistentDataContainer().has(headOwnerKey, PersistentDataType.STRING)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && playerHeadDropsEnabled && !killer.getUniqueId().equals(victim.getUniqueId())) {
            if (killer.getName().equals(victim.getName())) { return; }

            ItemStack head = createPlayerHead(victim, killer);
            victim.getWorld().dropItemNaturally(victim.getLocation(), head);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null || entity instanceof Player) {
            return;
        }

        if (!mobHeadDropsEnabled) {
            return;
        }

        EntityType type = entity.getType();

        if (!EntityTextureManager.isKillableEntity(type)) {
            return;
        }

        if (type.name().equals("MANNEQUIN")) {
            handleMannequinDeath(entity, killer);
            return;
        }

        double baseChance = config.getDouble("mob-head-drop-chances." + type.name().toLowerCase(), -1);

        if (baseChance < 0) {
            baseChance = getDefaultDropChance(type);
        }

        double lootingBonus = 0;
        if (config.getBoolean("looting-increases-drop-chance", true)) {
            int lootingLevel = killer.getInventory().getItemInMainHand()
                    .getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LOOTING);
            lootingBonus = lootingLevel * config.getDouble("looting-bonus-per-level", 5.0);
        }

        double finalChance = baseChance + lootingBonus;

        if (random.nextDouble() * 100 < finalChance) {
            ItemStack head = createMobHead(entity, killer);
            if (head != null) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), head);
            }
        }
    }
    private double getDefaultDropChance(EntityType type) {
        try {
            Class<?> entityClass = type.getEntityClass();
            if (entityClass == null) return 0;

            if (org.bukkit.entity.Boss.class.isAssignableFrom(entityClass)) {
                return config.getDouble("default-boss-drop-chance", 100.0);
            }

            if (org.bukkit.entity.Monster.class.isAssignableFrom(entityClass)) {
                return config.getDouble("default-hostile-drop-chance", 5.0);
            }

            return config.getDouble("default-passive-drop-chance", 2.0);
        } catch (Exception e) {
            return 0;
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.PLAYER_HEAD && item.hasItemMeta()) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta.getPersistentDataContainer().has(loreKey, PersistentDataType.STRING)) {
                Block block = event.getBlockPlaced();
                if (block.getState() instanceof Skull skull) {
                    skull.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING,
                            meta.getPersistentDataContainer().get(loreKey, PersistentDataType.STRING));
                    skull.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING,
                            meta.getPersistentDataContainer().get(headOwnerKey, PersistentDataType.STRING));
                    skull.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING,
                            meta.getPersistentDataContainer().get(killerKey, PersistentDataType.STRING));
                    skull.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING,
                            meta.getPersistentDataContainer().get(headTypeKey, PersistentDataType.STRING));
                    skull.update();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeadBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if ((block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD)
                && block.getState() instanceof Skull skull) {

            if (!skull.getPersistentDataContainer().has(headOwnerKey, PersistentDataType.STRING)) {
                return;
            }

            event.setDropItems(false);

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();

            if (meta != null) {
                if (skull.getOwnerProfile() != null) {
                    meta.setOwnerProfile(skull.getOwnerProfile());
                }

                String headOwner = skull.getPersistentDataContainer().get(headOwnerKey, PersistentDataType.STRING);
                String killer = skull.getPersistentDataContainer().get(killerKey, PersistentDataType.STRING);
                String headType = skull.getPersistentDataContainer().get(headTypeKey, PersistentDataType.STRING);
                String loreData = skull.getPersistentDataContainer().get(loreKey, PersistentDataType.STRING);

                if ("PLAYER".equals(headType)) {
                    meta.setDisplayName("§b" + headOwner + "'s Head");
                } else if (headType != null) {
                    try {
                        EntityType entityType = EntityType.valueOf(headType);
                        String colorCode = getMobColorCode(entityType);
                        meta.setDisplayName(colorCode + headOwner + " Head");
                    } catch (IllegalArgumentException e) {
                        meta.setDisplayName("§b" + headOwner + " Head");
                    }
                }

                if (loreData != null && !loreData.isEmpty()) {
                    List<String> lore = new ArrayList<>(Arrays.asList(loreData.split("\\|")));
                    meta.setLore(lore);
                }

                meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, headOwner);
                if (killer != null) {
                    meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, killer);
                }
                if (headType != null) {
                    meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, headType);
                }
                if (loreData != null) {
                    meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);
                }

                for (NamespacedKey key : skull.getPersistentDataContainer().getKeys()) {
                    if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        String value = skull.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        if (value != null) {
                            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
                        }
                    }
                }

                item.setItemMeta(meta);
            }

            event.getPlayer().sendMessage("§eThis head belonged to: " +
                    skull.getPersistentDataContainer().get(headOwnerKey, PersistentDataType.STRING));

            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        File file = new File(getDataFolder(), "queued_heads.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> base64List = config.getStringList(uuid.toString());

        if (base64List == null || base64List.isEmpty()) return;

        runTask(player.getLocation(), () -> {
            for (String base64 : base64List) {
                ItemStack item = ItemSerializer.deserialize(base64);
                if (item != null) {
                    player.getInventory().addItem(item);
                }
            }

            player.sendMessage("§aYou received " + base64List.size() + " head(s) while you were offline.");
            config.set(uuid.toString(), null);

            try {
                config.save(file);
            } catch (IOException e) {
                getLogger().warning("Failed to save queued_heads.yml: " + e.getMessage());
            }


        });

        if (!player.hasPermission("hh.update") || !isUpdateAvailable) return;
        runLater(player.getLocation(), () -> {
            if (isNotificationsEnabled(player.getUniqueId())) {
                if (compareVersions(latestVersion, currentversion) > 0) {
                    player.sendMessage("§6HeadHunter Plugin §eupdate available! Latest version: §a" + latestVersion + " §7Current version: " + currentversion);
                    player.sendMessage("§7Use §e/hh update §7to download the latest version.");
                    player.sendMessage("§7To disable these notifications, use §e/hh ignore");
                }
            }
        }, 40L);
    }

    @Override
    public void onEnable() {
        instance = this;
        VersionSafeEntityChecker.initialize();
        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║      HeadHunter Version Detection     ║");
        getLogger().info("╠════════════════════════════════════════╣");
        getLogger().info("║ Minecraft: " + VersionSafeEntityChecker.getMinecraftVersion());
        getLogger().info("║ Available Entities: " + VersionSafeEntityChecker.getAvailableEntities().size());
        getLogger().info("║ If you just updated the plugin");
        getLogger().info("║ Please make sure to check if there is");
        getLogger().info("║ any new updates to the config File");
        getLogger().info("║ https://sites.google.com/view/ifmasters-plugins/mc-plugins/configs/headhunter ");
        getLogger().info("╚════════════════════════════════════════╝");

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info("Folia detected - using region-aware scheduling");
        } catch (ClassNotFoundException e) {
            isFolia = false;
            getLogger().info("Running on Paper/Bukkit - using traditional scheduling");
        }

        getServer().getPluginManager().registerEvents(new SoundScript(), this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                Player player = event.getPlayer();
                File file = new File(getDataFolder(), "queued_heads.yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                List<String> list = config.getStringList(player.getUniqueId().toString());
                if (list == null || list.isEmpty()) return;

                for (String base64 : list) {
                    ItemStack item = ItemSerializer.deserialize(base64);
                    player.getInventory().addItem(item);
                }

                config.set(player.getUniqueId().toString(), null);
                try {
                    config.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                player.sendMessage("§aYou received " + list.size() + " head(s) while you were offline.");
            }
        }, this);

        saveDefaultConfig();
        config = getConfig();
        headOwnerKey = new NamespacedKey(this, "head_owner");
        killerKey = new NamespacedKey(this, "killer");
        killTimeKey = new NamespacedKey(this, "kill_time");
        headTypeKey = new NamespacedKey(this, "head_type");
        loreKey = new NamespacedKey(this, "lore");
        getServer().getPluginManager().registerEvents(this, this);

        AnimalData.initializeMobTextures();

        EntityTextureManager.initialize();
        DynamicSoundManager.initialize();

        Map<String, Object> entityDebug = EntityTextureManager.getDebugInfo();
        getLogger().info("[HeadHunter] Entity System:");
        getLogger().info("  - Killable Entities: " + entityDebug.get("killable_entities"));
        getLogger().info("  - Predefined Textures: " + entityDebug.get("predefined_textures"));
        getLogger().info("  - Generated Textures: " + entityDebug.get("generated_textures"));
        getLogger().info("  - Entities with Texture: " + entityDebug.get("entities_with_texture"));

        Map<String, Object> soundDebug = DynamicSoundManager.getDebugInfo();
        getLogger().info("[HeadHunter] Sound System:");
        getLogger().info("  - Entity Types with Sounds: " + soundDebug.get("entity_types_with_sounds"));
        getLogger().info("  - Random Sounds: " + soundDebug.get("random_sounds_enabled"));
        getLogger().info("  - Total Sounds: " + soundDebug.get("total_sounds"));

        initializeMobUUIDs();

        notificationsFile = new File(getDataFolder(), "notifications.txt");
        if (!notificationsFile.exists()) {
            try {
                getDataFolder().mkdirs();
                notificationsFile.createNewFile();
            } catch (IOException e) {
                getLogger().warning("Failed to create notifications file: " + e.getMessage());
            }
        }
        loadNotificationPreferences();
        cacheConfigValues();
        getCommand("hh").setExecutor(this);
        getCommand("hh").setTabCompleter(this);
        loadValidationSettings();
        UpdateChecker.checkForUpdates();
        initializeCaches();

        getLogger().info("[HeadHunter] enabled");
    }

    @Override
    public void onDisable() {
        flushQueuedHeadsToDisk();

        ItemSerializer.clearCache();
        UUID_CACHE.clear();
        queuedHeadsMemory.clear();
        PLAYER_EXISTENCE_CACHE.clear();
        CACHE_TIMESTAMPS.clear();
        SKIN_CACHE.clear();
        SKIN_CACHE_TIMESTAMPS.clear();

        getLogger().info("[HeadHunter] disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("hh")) return false;

        if (args.length == 0) {
            sender.sendMessage("§6=== HeadHunter Commands ===");
            sender.sendMessage("§e/hh spawn <entity/player> [player/Entity Name] - Spawn a head");
            sender.sendMessage("§e/hh reload - Reload configuration");
            sender.sendMessage("§e/hh update - Download latest update");
            sender.sendMessage("§e/hh ignore - Toggle update notifications");
            sender.sendMessage("§e/hh uv     - Checks for any updates");
            if (sender.hasPermission("headhunter.admin")) {
                sender.sendMessage("§e/hh debug [entities|sounds] - System diagnostics");
            }

            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("headhunter.reload")) {
                    sender.sendMessage("§cYou don't have permission to reload the plugin!");
                    return true;
                }
                reloadConfig();
                config = getConfig();
                cacheConfigValues();
                loadValidationSettings();
                sender.sendMessage("§aHeadHunter configuration reloaded!");
                break;

            case "spawn":
                if (!sender.hasPermission("headhunter.spawn")) {
                    sender.sendMessage("§cYou don't have permission to spawn heads!");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /hh spawn <mob/player> [name]");
                    return true;
                }
                spawnHead((Player) sender, args);
                break;
            case "give":
                if (!sender.hasPermission("headhunter.purchased")) {
                    sender.sendMessage("§cYou don't have permission to spawn heads!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /hh give <mob> [name]");
                    return true;
                }
                if (sender instanceof Player) {
                    purchaseHead((Player) sender, args);
                } else {
                    purchaseHeadConsole(sender, args);
                }
                break;

            case "update":
                if (!sender.hasPermission("hh.update")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (isUpdateAvailable) {
                    sender.sendMessage("§aLatest version: " + latestVersion);
                    UpdateChecker.updatePluginFromGitHub(sender, instance);
                } else {
                    sender.sendMessage("§aNo updates available. You're running the latest version!");
                }
                break;

            case "ignore":
                if (!sender.hasPermission("hh.update")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players!");
                    return true;
                }
                Player player = (Player) sender;
                boolean currentSetting = isNotificationsEnabled(player.getUniqueId());
                setNotificationsEnabled(player.getUniqueId(), !currentSetting);
                sender.sendMessage("§aUpdate notifications " + (!currentSetting ? "enabled" : "disabled") + "!");
                break;
            case "uv":
                if (!sender.hasPermission("hh.update")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players!");
                    return true;
                }
                sender.sendMessage("§aChecking for updates");

                UpdateChecker.checkForUpdates();
                if (isUpdateAvailable) {
                    sender.sendMessage("§aLatest version: " + latestVersion);
                    sender.sendMessage("§aTo update please use /hh update");

                } else {
                    sender.sendMessage("§aNo updates available. You're running the latest version!");
                }
                break;
            case "debug":
                if (!sender.hasPermission("headhunter.admin")) {
                    sender.sendMessage("§cYou don't have permission to use debug commands!");
                    return true;
                }

                sender.sendMessage("§6=== HeadHunter Debug Info ===");

                Map<String, Object> entityInfo = EntityTextureManager.getDebugInfo();
                sender.sendMessage("§eEntity System:");
                sender.sendMessage("  §7Killable Entities: §a" + entityInfo.get("killable_entities"));
                sender.sendMessage("  §7Predefined Textures: §a" + entityInfo.get("predefined_textures"));
                sender.sendMessage("  §7Generated Textures: §a" + entityInfo.get("generated_textures"));
                sender.sendMessage("  §7Entities with Texture: §a" + entityInfo.get("entities_with_texture"));
                sender.sendMessage("  §7Cached Profiles: §a" + entityInfo.get("cached_profiles"));

                Map<String, Object> soundInfo = DynamicSoundManager.getDebugInfo();
                sender.sendMessage("§eSound System:");
                sender.sendMessage("  §7Entities with Sounds: §a" + soundInfo.get("entity_types_with_sounds"));
                sender.sendMessage("  §7Random Sounds Enabled: §a" + soundInfo.get("random_sounds_enabled"));
                sender.sendMessage("  §7Total Sound Variants: §a" + soundInfo.get("total_sounds"));

                sender.sendMessage("§eMemory:");
                sender.sendMessage("  §7Item Serialization Cache: §a" + ItemSerializer.getCacheSize());
                sender.sendMessage("  §7Skin Cache: §a" + SKIN_CACHE.size());

                if (args.length > 1 && args[1].equalsIgnoreCase("entities")) {
                    sender.sendMessage("§6=== All Killable Entities ===");
                    Set<EntityType> killable = EntityTextureManager.getKillableEntities();
                    int count = 0;
                    StringBuilder line = new StringBuilder("§7");

                    for (EntityType type : killable) {
                        String status = EntityTextureManager.hasTexture(type) ? "§a✓" : "§c✗";
                        String soundStatus = DynamicSoundManager.hasSound(type) ? "§a♪" : "§7-";
                        line.append(status).append(soundStatus).append(" §f").append(type.name()).append(" §8| ");
                        count++;

                        if (count % 3 == 0) {
                            sender.sendMessage(line.toString());
                            line = new StringBuilder("§7");
                        }
                    }

                    if (line.length() > 2) {
                        sender.sendMessage(line.toString());
                    }

                    sender.sendMessage("§7Legend: §a✓§7=Has Texture, §a♪§7=Has Sound, §c✗§7=No Texture, §7-§7=No Sound");
                }

                if (args.length > 1 && args[1].equalsIgnoreCase("sounds")) {
                    if (!sender.hasPermission("headhunter.admin")) {
                        sender.sendMessage("§cYou don't have permission to use debug commands!");
                        return true;
                    }

                    if (args.length > 2) {
                        try {
                            EntityType type = EntityType.valueOf(args[2].toUpperCase());
                            sender.sendMessage("§6=== Sounds for " + type.name() + " ===");

                            java.util.List<Sound> sounds = DynamicSoundManager.getAllSounds(type);
                            if (sounds.isEmpty()) {
                                sender.sendMessage("§cNo sounds found for this entity!");
                            } else {
                                for (Sound sound : sounds) {
                                    sender.sendMessage("§7- §e" + sound.getClass().getName());
                                }
                                sender.sendMessage("§7Total: §a" + sounds.size() + " §7sound variants");
                            }
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("§cInvalid entity type!");
                        }
                    } else {
                        sender.sendMessage("§cUsage: /hh debug sounds <entity_type>");
                    }
                }

                sender.sendMessage("§7Use §e/hh debug entities §7to see all killable entities");
                sender.sendMessage("§7Use §e/hh debug sounds <entity> §7to see entity sounds");
                break;

            default:
                sender.sendMessage("§cUnknown subcommand! Use /hh for help.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("spawn");
            completions.add("reload");
            completions.add("update");
            completions.add("uv");
            completions.add("ignore");
            completions.add("give");
            completions.add("debug");

        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            completions.add("entity");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("entity")) {
            for (EntityType type : EntityTextureManager.getKillableEntities()) {
                if (EntityTextureManager.hasTexture(type)) {
                    completions.add(type.name().toLowerCase());
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("entity")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }

        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            completions.add("player");
            completions.add("entity");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("spawn") && args[1].equalsIgnoreCase("player")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("spawn") && args[1].equalsIgnoreCase("entity")) {
            for (EntityType type : EntityTextureManager.getKillableEntities()) {
                if (EntityTextureManager.hasTexture(type)) {
                    completions.add(type.name().toLowerCase());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            completions.add("entities");
            completions.add("sounds");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("sounds")) {
            for (EntityType type : EntityTextureManager.getKillableEntities()) {
                completions.add(type.name().toLowerCase());
            }
        }



        return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
    }

    private void loadValidationSettings() {
        String mode = config.getString("player-validation.mode", "SERVER_ONLY").toUpperCase();
        try {
            validationMode = ValidationMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid validation mode: " + mode + ". Using SERVER_ONLY");
            validationMode = ValidationMode.SERVER_ONLY;
        }
        getLogger().info("Player validation mode: " + validationMode);
    }
    private void cacheConfigValues() {
        playerHeadDropsEnabled = config.getBoolean("player-head-drops-enabled", true);
        mobHeadDropsEnabled = config.getBoolean("mob-head-drops-enabled", true);
        allowMannequinSkin = config.getBoolean("allow_mannequin_skin", true);

    }

    private void validatePlayerMojangAPI(String playerName, Consumer<Boolean> callback) {
        String lowerName = playerName.toLowerCase();
        Long timestamp = CACHE_TIMESTAMPS.get(lowerName);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRY) {
            callback.accept(PLAYER_EXISTENCE_CACHE.get(lowerName));
            return;
        }

        runAsync(() -> {
            boolean exists = false;
            try {
                String apiUrl = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                connection.setRequestProperty("User-Agent", "HeadHunter-Plugin");

                int responseCode = connection.getResponseCode();
                exists = (responseCode == 200);

                if (responseCode != 200 && responseCode != 404) {
                    getLogger().warning("Unexpected response from Mojang API for player '" + playerName + "': " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                getLogger().warning("Couldn't find profile with name: " + playerName);
                exists = false;
            }

            PLAYER_EXISTENCE_CACHE.put(lowerName, Boolean.valueOf(exists));
            CACHE_TIMESTAMPS.put(lowerName, Long.valueOf(System.currentTimeMillis()));

            boolean finalExists = exists;
            runTask(null, () -> callback.accept(Boolean.valueOf(finalExists)));
        });
    }
    private void validatePlayer(String playerName, Consumer<Boolean> callback) {
        if (isLikelyBedrockPlayer(playerName)) {
            callback.accept(Boolean.valueOf(hasPlayerEverJoined(playerName)));
            return;
        }

        switch (validationMode) {
            case SERVER_ONLY:
                callback.accept(Boolean.valueOf(hasPlayerEverJoined(playerName)));
                break;
            case MOJANG_API:
                validatePlayerMojangAPI(playerName, callback);
                break;
            case DISABLED:
                callback.accept(Boolean.valueOf(true));
                break;
        }
    }

    private boolean hasPlayerEverJoined(String playerName) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            return offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline();
        } catch (Exception e) {
            getLogger().warning("Couldn't find profile with name: " + playerName);
            return false;
        }
    }
    private boolean isLikelyBedrockPlayer(String playerName) {
        String prefix = config.getString("geyser.prefix", ".");
        if (playerName.startsWith(prefix)) {
            return true;
        }


        if (playerName.contains(" ") || playerName.length() > 16) {
            return true;
        }

        return false;
    }

    // initializers
    private void initializeCaches() {
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && mobTextures.containsKey(type)) {
                String name = type.name().replace("_", " ").toLowerCase();
                DISPLAY_NAME_CACHE.put(type, name.substring(0, 1).toUpperCase() + name.substring(1));

                String profileName = type.name().toLowerCase().replace("_", "");
                PROFILE_NAME_CACHE.put(type, profileName.substring(0, 1).toUpperCase() + profileName.substring(1));
                ENTITY_TYPE_LOOKUP.put(type.name().toLowerCase(), type);

                String colorCode = switch (type) {
                    case SPIDER, CAVE_SPIDER, DROWNED, VEX, WITCH, BREEZE, BOGGED, EVOKER, SHULKER, SILVERFISH, STRAY -> "§c";
                    case ILLUSIONER, CREAKING, GIANT, MANNEQUIN -> "§9";
                    case BLAZE, MAGMA_CUBE, WARDEN, ELDER_GUARDIAN, WITHER -> "§4";
                    case ENDERMAN, GUARDIAN, ZOMBIFIED_PIGLIN, ENDERMITE, PHANTOM, PILLAGER -> "§5";
                    case PIG, COW, SHEEP, CHICKEN, ALLAY, ARMADILLO, AXOLOTL, WOLF, COD, BEE, FOX, GOAT , CAT, BAT, CAMEL, DONKEY, FROG, GLOW_SQUID, HORSE,
                         MOOSHROOM, MULE, OCELOT, PARROT, PUFFERFISH, RABBIT, SALMON, SKELETON_HORSE, SNIFFER, SQUID, STRIDER, TADPOLE, TROPICAL_FISH, TURTLE,
                         WANDERING_TRADER, LLAMA, TRADER_LLAMA, COPPER_GOLEM, HAPPY_GHAST  -> "§a";
                    case NAUTILUS, ZOMBIE_NAUTILUS, PARCHED, CAMEL_HUSK -> "§3";
                    case VILLAGER, IRON_GOLEM, SNOW_GOLEM, ZOMBIE_VILLAGER, ZOMBIE_HORSE, PANDA, DOLPHIN, POLAR_BEAR -> "§b";
                    case HOGLIN, ZOGLIN, PIGLIN_BRUTE, RAVAGER, HUSK, VINDICATOR  -> "§6";
                    case SLIME -> "§2";
                    default -> "§f";
                };
                COLOR_CODE_CACHE.put(type, colorCode);
            }
        }

    }
    private void initializeMobUUIDs() {
        for (EntityType type : mobTextures.keySet()) {
            String name = type.name().replace("_", " ").toLowerCase();
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            mobHeadUUIDs.put(type.name(), UUID.nameUUIDFromBytes(("mobhead_" + type.name()).getBytes()));
        }
    }

    // Getters
    public static String getDisplayName(EntityType entityType) {
        return DISPLAY_NAME_CACHE.getOrDefault(entityType, entityType.name());
    }
    private String getProfileName(EntityType entityType) {
        return PROFILE_NAME_CACHE.getOrDefault(entityType, entityType.name());
    }
    private String getMobColorCode(EntityType entityType) {
        return COLOR_CODE_CACHE.getOrDefault(entityType, "§f");
    }
    private UUID getCachedUUID(String key) {
        return UUID_CACHE.computeIfAbsent(key, k -> UUID.nameUUIDFromBytes(k.getBytes()));
    }

    // Runnables
    public static void runAsync(Runnable task) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(instance, (t) -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(instance, task);
        }
    }
    public static void runLater(Location location, Runnable task, long delay) {
        if (isFolia) {
            if (location != null) {
                Bukkit.getRegionScheduler().runDelayed(instance, location, (t) -> task.run(), delay);
            } else {
                Bukkit.getGlobalRegionScheduler().runDelayed(instance, (t) -> task.run(), delay);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(instance, task, delay);
        }
    }
    public static void runTask(Location location, Runnable task) {
        if (isFolia) {
            if (location != null) {
                Bukkit.getRegionScheduler().run(instance, location, (t) -> task.run());
            } else {
                Bukkit.getGlobalRegionScheduler().run(instance, (t) -> task.run());
            }
        } else {
            Bukkit.getScheduler().runTask(instance, task);
        }
    }

    // Heads stuff
    private void handleMannequinDeath(LivingEntity mannequin, Player killer) {
        EntityType type = mannequin.getType();
        double chance = config.getDouble("mob-head-drop-chances." + type.name().toLowerCase(), 0);

        if (random.nextDouble() * 100 >= chance) {
            return;
        }
        if (!VersionSafeEntityChecker.isEntityAvailable(type)) {
            getLogger().warning("Mannequin entity not available in this version!");
            return;
        }

        String profileName = mannequin.getCustomName();
        if (profileName != null) {
            profileName = ChatColor.stripColor(profileName);
        }

        if (profileName == null || !allowMannequinSkin) {
            ItemStack head = createMobHead(mannequin, killer);
            if (head != null) {
                mannequin.getWorld().dropItemNaturally(mannequin.getLocation(), head);
            }
            return;
        }

        Location dropLocation = mannequin.getLocation();
        String finalProfileName = profileName;

        runAsync(() -> {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                String colorCode = config.getString("entity-colors.mannequin", getMobColorCode(EntityType.MANNEQUIN));
                meta.setDisplayName(colorCode + "Mannequin Head");

                try {
                    PlayerProfile profile = Bukkit.createPlayerProfile(finalProfileName);
                    profile.update().thenAccept(updatedProfile -> {
                        runTask(dropLocation, () -> {
                            if (updatedProfile.getTextures().getSkin() != null) {
                                meta.setOwnerProfile(updatedProfile);
                            }

                            List<String> lore = new ArrayList<>();
                            lore.add("§7Mannequin of: " + finalProfileName);
                            lore.add("§7Killed by: §c" + killer.getName());
                            meta.setLore(lore);

                            String loreData = String.join("|", lore);
                            meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, "Mannequin");
                            meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, killer.getName());
                            meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, EntityType.MANNEQUIN.name());
                            meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);
                            head.setItemMeta(meta);

                            dropLocation.getWorld().dropItemNaturally(dropLocation, head);
                        });
                    });
                } catch (Exception e) {
                    getLogger().warning("[HeadHunter] Failed to fetch mannequin skin: " + e.getMessage());
                    ItemStack defaultHead = createMobHead(mannequin, killer);
                    runTask(dropLocation, () -> {
                        if (defaultHead != null) {
                            dropLocation.getWorld().dropItemNaturally(dropLocation, defaultHead);
                        }
                    });
                }
            }
        });
    }

    public static void createAndGivePlayerHead(Player player, String playerName) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            if (targetPlayer != null) {
                meta.setOwningPlayer(targetPlayer);
            } else {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
            }
            meta.setDisplayName("§b" + playerName + "'s Head");

            List<String> lore = new ArrayList<>();
            lore.add("§7Spawned by: §e" + player.getName());
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, playerName);
            meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, "SPAWNED");
            meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, "PLAYER");

            String loreData = String.join("|", lore);
            meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);

            head.setItemMeta(meta);
        }

        player.getInventory().addItem(head);
        player.sendMessage("§aSpawned " + playerName + "'s head!");
    }
    private ItemStack createPlayerHead(Player victim, Player killer) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(victim);
            meta.setDisplayName("§b" + victim.getName() + "'s Head");


            List<String> lore = new ArrayList<>();
            lore.add("§7Killed by: §c" + killer.getName());
            meta.setLore(lore);

            String loreData = String.join("|", lore);

            meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, victim.getName());
            meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, killer.getName());
            meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, "PLAYER");
            meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);
            head.setItemMeta(meta);
        }
        return head;
    }
    private ItemStack createMobHead(LivingEntity entity, Player killer) {
        EntityType entityType = entity.getType();

        if (!EntityTextureManager.isKillableEntity(entityType)) {
            getLogger().warning("[HeadHunter] Attempted to create head for invalid entity: " + entityType);
            return null;
        }

        Material vanillaHeadMaterial = getVanillaHeadMaterial(entityType);

        if (vanillaHeadMaterial != null) {
            ItemStack head = new ItemStack(vanillaHeadMaterial);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                String displayName = getDisplayName(entityType);
                String colorCode = config.getString("entity-colors." + entityType.name().toLowerCase(),
                        getMobColorCode(entityType));

                meta.setDisplayName(colorCode + displayName + " Head");

                List<String> lore = buildHeadLore(entity, killer, displayName);
                meta.setLore(lore);

                String loreData = String.join("|", lore);
                meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, displayName);
                meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, killer.getName());
                meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, entityType.name());
                meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);

                head.setItemMeta(meta);
            }

            return head;
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta == null) {
            return null;
        }

        String displayName = getDisplayName(entityType);
        String profileName = getProfileName(entityType);
        String colorCode = config.getString("entity-colors." + entityType.name().toLowerCase(),
                getMobColorCode(entityType));

        meta.setDisplayName(colorCode + displayName + " Head");

        List<String> lore = buildHeadLore(entity, killer, displayName);
        meta.setLore(lore);

        PlayerProfile profile = EntityTextureManager.getOrCreateProfile(entityType, profileName);
        if (profile != null) {
            meta.setOwnerProfile(profile);
        } else {
            getLogger().fine("[HeadHunter] No texture for " + entityType + ", using default");
        }

        String loreData = String.join("|", lore);
        meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, displayName);
        meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, killer.getName());
        meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, entityType.name());
        meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);

        head.setItemMeta(meta);
        return head;
    }

    private List<String> buildHeadLore(LivingEntity entity, Player killer, String displayName) {
        List<String> lore = new ArrayList<>();

        String loreFormat = config.getString("mob-head-lore-format", "§7Killed by: §c%killer%");
        String loreText = loreFormat
                .replace("%killer%", killer.getName())
                .replace("%mob%", displayName)
                .replace("%world%", entity.getWorld().getName());

        lore.add(loreText);

        if (config.getBoolean("show-kill-location", false)) {
            Location loc = entity.getLocation();
            lore.add(String.format("§7Location: %d, %d, %d",
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        }

        if (config.getBoolean("show-kill-time", false)) {
            lore.add("§7Killed: " + new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm")
                    .format(new java.util.Date()));
        }

        if (entity.getCustomName() != null && config.getBoolean("show-custom-name", true)) {
            lore.add("§7Named: " + entity.getCustomName());
        }

        return lore;
    }


    private ItemStack createSpawnedMobHead(EntityType entityType, String spawner, String targetName) {
        Material vanillaHeadMaterial = getVanillaHeadMaterial(entityType);

        if (vanillaHeadMaterial != null) {
            ItemStack head = new ItemStack(vanillaHeadMaterial);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                String displayName = getDisplayName(entityType);
                String colorCode = config.getString("entity-colors." + entityType.name().toLowerCase(),
                        getMobColorCode(entityType));

                meta.setDisplayName(colorCode + displayName + " Head");

                List<String> lore = new ArrayList<>();
                if (!spawner.equals("Server")) {
                    lore.add("§7Spawned by: §e" + spawner);
                } else {
                    lore.add("§7Was given to: §e" + targetName);
                }
                meta.setLore(lore);

                String loreData = String.join("|", lore);
                meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, displayName);
                meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, "SPAWNED");
                meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, entityType.name());
                meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);

                head.setItemMeta(meta);
            }

            return head;
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            String displayName = getDisplayName(entityType);
            String profileName = getProfileName(entityType);

            String colorCode = config.getString("entity-colors." + entityType.name().toLowerCase(),
                    getMobColorCode(entityType));
            meta.setDisplayName(colorCode + displayName + " Head");

            List<String> lore = new ArrayList<>();
            if (!spawner.equals("Server")) {
                lore.add("§7Spawned by: §e" + spawner);
            } else {
                lore.add("§7Was given to: §e" + targetName);
            }

            try {
                if (entityType == EntityType.valueOf("MANNEQUIN") && !allowMannequinSkin) {
                    lore.add("§7Default Steve skin");
                }
            } catch (IllegalArgumentException e) {
            }
            meta.setLore(lore);

            String loreData = String.join("|", lore);

            boolean isMannequin = false;
            try {
                isMannequin = entityType == EntityType.valueOf("MANNEQUIN");
            } catch (IllegalArgumentException e) {
            }

            if (!isMannequin || allowMannequinSkin) {
                PlayerProfile profile = EntityTextureManager.getOrCreateProfile(entityType, profileName);
                if (profile != null) {
                    meta.setOwnerProfile(profile);
                }
            }

            meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, displayName);
            meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, "SPAWNED");
            meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, entityType.name());
            meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);
            head.setItemMeta(meta);
        }
        return head;
    }

    private void purchaseHeadConsole(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /hh give entity <mob> <playerName>");
            return;
        }

        if (!args[1].equalsIgnoreCase("entity")) {
            sender.sendMessage("§cOnly 'entity' type supported from console.");
            return;
        }

        String mobName = args[2].toLowerCase();
        String targetName = args[3];

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer not found: " + targetName);
            return;
        }

        EntityType entityType = null;
        try {
            entityType = EntityType.valueOf(mobName.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (EntityType type2 : EntityTextureManager.getKillableEntities()) {
                if (type2.name().equalsIgnoreCase(mobName)) {
                    entityType = type2;
                    break;
                }
            }
        }

        if (entityType == null || !EntityTextureManager.isKillableEntity(entityType)) {
            sender.sendMessage("§cInvalid or unsupported mob type: " + mobName);
            return;
        }

        ItemStack head = createSpawnedMobHead(entityType, "Server", targetName);

        if (targetPlayer.isOnline()) {
            Player onlinePlayer = targetPlayer.getPlayer();
            runTask(onlinePlayer.getLocation(), () -> {
                onlinePlayer.getInventory().addItem(head);
                onlinePlayer.sendMessage("§aYou have received a " + mobName + " head from the server!");
            });
            sender.sendMessage("§aGiven head to online player " + targetName);
        } else {
            queueHeadForPlayer(targetPlayer.getUniqueId(), head);
            sender.sendMessage("§aQueued head for offline player " + targetName);
        }
    }
    private void purchaseHead(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /hh give entity <mob> <player>");
            return;
        }

        String type = args[1].toLowerCase();

        if (type.equals("entity")) {
            String mobName = args[2].toLowerCase();
            String playerName = args[3];

            EntityType entityType = ENTITY_TYPE_LOOKUP.get(mobName.toLowerCase());

            if (entityType == null || !mobTextures.containsKey(entityType)) {
                sender.sendMessage("§cInvalid or unsupported mob type.");
                return;
            }

            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(playerName);
            Player targetPlayer = offlineTarget.getPlayer();

            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage("§cThe target player is not online.");
                return;
            }

            ItemStack head = createSpawnedMobHead(entityType, playerName, playerName);

            if (targetPlayer == null || !offlineTarget.isOnline()) {
                queueHeadForPlayer(offlineTarget.getUniqueId(), head);
                sender.sendMessage("§e" + playerName + " is offline. Head will be delivered when they log in.");
                return;
            }

            targetPlayer.getInventory().addItem(head);


            String mobDisplayName = Character.toUpperCase(mobName.charAt(0)) + mobName.substring(1).replace("_", " ");
            sender.sendMessage("§aGave " + mobDisplayName + " head to " + targetPlayer.getName() + "!");
        }
    }

    private void spawnHead(Player player, String[] args) {
        String type = args[1].toLowerCase();

        if (type.equals("player")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /hh spawn player <playername>");
                return;
            }
            String playerName = args[2];

            if (validationMode == ValidationMode.DISABLED) {
                createAndGivePlayerHeadWithSkin(player, playerName);
                return;
            }
            if (validationMode == ValidationMode.MOJANG_API) {
                player.sendMessage("§7Validating player...");
            }

            validatePlayer(playerName, exists -> {
                if (!exists) {
                    String message = switch (validationMode) {
                        case SERVER_ONLY -> "§cPlayer '" + playerName + "' has never joined this server!";
                        case MOJANG_API -> "§cPlayer '" + playerName + "' does not exist on Mojang servers!";
                        default -> "";
                    };
                    player.sendMessage(message);
                    player.sendMessage("§7Tip: Check the spelling or use a different validation mode in config.");
                    return;
                }

                createAndGivePlayerHeadWithSkin(player, playerName);
            });

        } else if (type.equals("entity")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /hh spawn entity <mob_type>");
                return;
            }

            String mobName = args[2];
            if (!VersionSafeEntityChecker.entityExists(mobName)) {
                player.sendMessage("§cEntity '" + mobName + "' does not exist in Minecraft " +
                        VersionSafeEntityChecker.getMinecraftVersion());
                player.sendMessage("§7This entity may be from a newer or older version.");
                return;
            }

            EntityType entityType = null;

            try {
                entityType = EntityType.valueOf(mobName.toUpperCase());
            } catch (IllegalArgumentException e) {
                for (EntityType type2 : EntityTextureManager.getKillableEntities()) {
                    if (type2.name().equalsIgnoreCase(mobName)) {
                        entityType = type2;
                        break;
                    }
                }
            }

            if (entityType == null || !EntityTextureManager.isKillableEntity(entityType)) {
                player.sendMessage("§cUnknown or invalid mob type: " + mobName);
                player.sendMessage("§7Use tab completion to see available entities");
                return;
            }

            if (!EntityTextureManager.hasTexture(entityType)) {
                player.sendMessage("§eWarning: No texture available for " + entityType.name());
                player.sendMessage("§7Spawning with default/generated texture...");
            }

            ItemStack head = createSpawnedMobHead(entityType, player.getName(), player.getName());
            player.getInventory().addItem(head);

            String displayName = getDisplayName(entityType);
            player.sendMessage("§aSpawned " + displayName + " head!");
        }
    }

    // Head queues
    private void queueHeadForPlayer(UUID playerUUID, ItemStack headItem) {
        String serializedItem = ItemSerializer.serialize(headItem);
        if (serializedItem == null) return;

        queuedHeadsMemory.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(serializedItem);

        if (!flushScheduled) {
            flushScheduled = true;
            runLater(null, this::flushQueuedHeadsToDisk, 600L);
        }
    }
    private Material getMaterialSafely(String materialName) {
        try {
            Material material = Material.valueOf(materialName);
            if (material.isItem()) {
                return material;
            }
        } catch (IllegalArgumentException e) {
            getLogger().fine("[HeadHunter] Material " + materialName + " not available in this version");
        }
        return null;
    }

    private Material getVanillaHeadMaterial(EntityType entityType) {
        if (!VersionSafeEntityChecker.isEntityAvailable(entityType)) {
            return null;
        }

        try {
            switch (entityType) {
                case ZOMBIE:
                    return getMaterialSafely("ZOMBIE_HEAD");
                case CREEPER:
                    return getMaterialSafely("CREEPER_HEAD");
                case SKELETON:
                    return getMaterialSafely("SKELETON_SKULL");
                case WITHER_SKELETON:
                    return getMaterialSafely("WITHER_SKELETON_SKULL");
                case PIGLIN:
                    return getMaterialSafely("PIGLIN_HEAD");
                case ENDER_DRAGON:
                    return getMaterialSafely("DRAGON_HEAD");
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    private void flushQueuedHeadsToDisk() {
        if (queuedHeadsMemory.isEmpty()) {
            flushScheduled = false;
            return;
        }

        runAsync(() -> {
            File file = new File(getDataFolder(), "queued_heads.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            synchronized (queuedHeadsMemory) {
                for (Map.Entry<UUID, List<String>> entry : queuedHeadsMemory.entrySet()) {
                    List<String> existing = config.getStringList(entry.getKey().toString());
                    existing.addAll(entry.getValue());
                    config.set(entry.getKey().toString(), existing);
                }
                queuedHeadsMemory.clear();
            }

            try {
                config.save(file);
            } catch (IOException e) {
                getLogger().warning("Failed to flush queued heads: " + e.getMessage());
            }

            flushScheduled = false;
        });
    }

    // Update Info things & Notifications
    public static void setUpdateAvailable(boolean available, String version) {
        if (instance != null) {
            instance.isUpdateAvailable = available;
            if (available) {
                instance.latestVersion = version;
            }
        }
    }

    private void loadNotificationPreferences() {
        try (BufferedReader reader = new BufferedReader(new FileReader(notificationsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" - Notifications: ");
                if (parts.length == 2) {
                    UUID uuid = UUID.fromString(parts[0].replace("Userid: ", "").trim());
                    boolean enabled = parts[1].trim().equalsIgnoreCase("Enabled");
                    playerNotificationPreferences.put(uuid, Boolean.valueOf(enabled));
                }
            }
        } catch (IOException e) {
            getLogger().warning("Failed to load notification preferences: " + e.getMessage());
        }
    }
    private void saveNotificationPreferences() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(notificationsFile))) {
            for (Map.Entry<UUID, Boolean> entry : playerNotificationPreferences.entrySet()) {
                writer.println("Userid: " + entry.getKey() + " - Notifications: " + (entry.getValue() ? "Enabled" : "Disabled"));
            }
        } catch (IOException e) {
            getLogger().warning("Failed to save notification preferences: " + e.getMessage());
        }
    }
    public boolean isNotificationsEnabled(UUID uuid) {
        return playerNotificationPreferences.getOrDefault(uuid, Boolean.valueOf(true));
    }
    public void setNotificationsEnabled(UUID uuid, boolean enabled) {
        playerNotificationPreferences.put(uuid, Boolean.valueOf(enabled));
        saveNotificationPreferences();
    }

    private int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        int length = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2 = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }
        return 0;
    }


    private static String formatMobName(EntityType entityType) {
        String name = entityType.name();
        StringBuilder sb = new StringBuilder(name.length() + 2);

        boolean capitalize = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                sb.append(' ');
                capitalize = true;
            } else if (capitalize) {
                sb.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }

        return sb.toString();
    }

    public static void runTaskForPlayer(Player player, Runnable task) {
        if (isFolia) {
            Bukkit.getRegionScheduler().run(instance, player.getLocation(), (t) -> task.run());
        } else {
            Bukkit.getScheduler().runTask(instance, task);
        }
    }

}
