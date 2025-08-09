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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class MainScript extends JavaPlugin implements Listener, TabCompleter {
    private static boolean isFolia = false;
    public File soundScript;
    public String latestVersion = getDescription().getVersion();
    public String currentversion = getDescription().getVersion();
    public static String getCurrentVersion() {
        return instance != null ? instance.currentversion : "Unknown";
    }


    private boolean isUpdateAvailable = false;
    private static MainScript instance;

    public static FileConfiguration config;
    private final Random random = new Random();
    public static NamespacedKey headOwnerKey;
    private NamespacedKey killerKey;
    private NamespacedKey killTimeKey;
    public static NamespacedKey headTypeKey;
    private NamespacedKey loreKey;

    public static final Map<EntityType, String> mobTextures = new HashMap<>();
    private final Map<String, UUID> mobHeadUUIDs = new HashMap<>();

    private File notificationsFile;
    private Map<UUID, Boolean> playerNotificationPreferences = new HashMap<>();


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

    @Override
    public void onEnable() {
        instance = this;
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

        getCommand("hh").setExecutor(this);
        getCommand("hh").setTabCompleter(this);

        UpdateChecker.checkForUpdates();

        getLogger().info("[HeadHunter] enabled");
    }

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






    private void initializeMobUUIDs() {
        for (EntityType type : mobTextures.keySet()) {
            String name = type.name().replace("_", " ").toLowerCase();
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            mobHeadUUIDs.put(type.name(), UUID.nameUUIDFromBytes(("mobhead_" + type.name()).getBytes()));
        }
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            completions.add("entity");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("entity")) {
            for (EntityType type : EntityType.values()) {
                if (type.isAlive() && mobTextures.containsKey(type)) {
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
            for (EntityType type : EntityType.values()) {
                if (type.isAlive() && mobTextures.containsKey(type)) {
                    completions.add(type.name().toLowerCase());
                }
            }
        }


        return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
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
        for (EntityType et : EntityType.values()) {
            if (et.name().toLowerCase().equals(mobName)) {
                entityType = et;
                break;
            }
        }

        if (entityType == null) {
            sender.sendMessage("§cUnknown mob type: " + mobName);
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


    private void spawnHead(Player player, String[] args) {
        String type = args[1].toLowerCase();

        if (type.equals("player")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /hh spawn player <playername>");
                return;
            }
            String playerName = args[2];
            Player targetPlayer = Bukkit.getPlayer(playerName);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                if (targetPlayer != null) {
                    meta.setOwningPlayer(targetPlayer);
                } else {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
                }
                meta.setDisplayName("§f" + playerName + "'s Head");

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

        } else if (type.equals("entity")) {
            EntityType entityType = null;
            String MobName2 = args[2];
            for (EntityType et : EntityType.values()) {
                if (et.name().toLowerCase().equals(MobName2)) {
                    entityType = et;
                    break;
                }
            }

            if (entityType == null || !mobTextures.containsKey(entityType)) {
                player.sendMessage("§cUnknown mob type: " + MobName2);
                return;
            }

            ItemStack head = createSpawnedMobHead(entityType, player.getName(), player.getName());
            player.getInventory().addItem(head);

            String mobName = entityType.name().replace("_", " ").toLowerCase();
            mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1);
            player.sendMessage("§aSpawned " + mobName + " head!");
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

            EntityType entityType = null;
            for (EntityType et : EntityType.values()) {
                if (et.name().toLowerCase().equals(mobName)) {
                    entityType = et;
                    break;
                }
            }

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
    private void queueHeadForPlayer(UUID playerUUID, ItemStack headItem) {
        File file = new File(getDataFolder(), "queued_heads.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String serializedItem = ItemSerializer.serialize(headItem);

        List<String> list = config.getStringList(playerUUID.toString());
        list.add(serializedItem);
        config.set(playerUUID.toString(), list);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && config.getBoolean("player-head-drops-enabled", true) && !killer.getUniqueId().equals(victim.getUniqueId())) {
            if (killer.getName().equals(victim.getName())) { return; }

            ItemStack head = createPlayerHead(victim, killer);
            victim.getWorld().dropItemNaturally(victim.getLocation(), head);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer != null && !(entity instanceof Player)) {
            if (!config.getBoolean("mob-head-drops-enabled", true)) return;

            String type = entity.getType().name().toLowerCase();
            double chance = config.getDouble("mob-head-drop-chances." + type, 0);
            if (random.nextDouble() * 100 < chance) {
                ItemStack head = createMobHead(entity, killer);
                entity.getWorld().dropItemNaturally(entity.getLocation(), head);
            }
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
                        meta.setDisplayName("§f" + headOwner + " Head");
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
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            EntityType entityType = entity.getType();
            String displayName = getDisplayName(entityType);
            String profileName = getProfileName(entityType);

            String colorCode = getMobColorCode(entityType);
            meta.setDisplayName(colorCode + displayName + " Head");

            List<String> lore = new ArrayList<>();
            lore.add("§7Killed by: §c" + killer.getName());
            meta.setLore(lore);

            String loreData = String.join("|", lore);

            if (mobTextures.containsKey(entityType)) {
                try {
                    UUID headUUID = UUID.nameUUIDFromBytes((entityType.name() + "_" + killer.getName()).getBytes());
                    PlayerProfile profile = Bukkit.createPlayerProfile(headUUID, profileName);
                    PlayerTextures textures = profile.getTextures();
                    textures.setSkin(new URL(mobTextures.get(entityType)));
                    profile.setTextures(textures);
                    meta.setOwnerProfile(profile);
                } catch (MalformedURLException e) {
                    getLogger().warning("Invalid texture URL for " + entityType + ": " + e.getMessage());
                }
            }

            meta.getPersistentDataContainer().set(headOwnerKey, PersistentDataType.STRING, displayName);
            meta.getPersistentDataContainer().set(killerKey, PersistentDataType.STRING, killer.getName());
            meta.getPersistentDataContainer().set(headTypeKey, PersistentDataType.STRING, entityType.name());
            meta.getPersistentDataContainer().set(loreKey, PersistentDataType.STRING, loreData);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createSpawnedMobHead(EntityType entityType, String spawner, String targetName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            String displayName = getDisplayName(entityType);
            String profileName = getProfileName(entityType);

            String colorCode = getMobColorCode(entityType);
            meta.setDisplayName(colorCode + displayName + " Head");

            List<String> lore = new ArrayList<>();
            if (!spawner.equals("Server")) {
                lore.add("§7Spawned by: §e" + spawner);
            } else {
                lore.add("§7Was given to: §e" + targetName);

            }
            meta.setLore(lore);

            String loreData = String.join("|", lore);

            if (mobTextures.containsKey(entityType)) {
                try {
                    UUID headUUID = UUID.nameUUIDFromBytes(("spawned_" + entityType.name() + "_" + spawner).getBytes());
                    PlayerProfile profile = Bukkit.createPlayerProfile(headUUID, profileName);
                    PlayerTextures textures = profile.getTextures();
                    textures.setSkin(new URL(mobTextures.get(entityType)));
                    profile.setTextures(textures);
                    meta.setOwnerProfile(profile);
                } catch (MalformedURLException e) {
                    getLogger().warning("Invalid texture URL for " + entityType + ": " + e.getMessage());
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

    public static String getDisplayName(EntityType entityType) {
        String name = entityType.name().replace("_", " ").toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String getProfileName(EntityType entityType) {
        String name = entityType.name().toLowerCase().replace("_", "");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static void setUpdateAvailable(boolean available, String version) {
        if (instance != null) {
            instance.isUpdateAvailable = available;
            if (available) {
                instance.latestVersion = version;
            }
        }
    }

    public boolean isUpdateAvailable() {
        return isUpdateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    private void loadNotificationPreferences() {
        try (BufferedReader reader = new BufferedReader(new FileReader(notificationsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" - Notifications: ");
                if (parts.length == 2) {
                    UUID uuid = UUID.fromString(parts[0].replace("Userid: ", "").trim());
                    boolean enabled = parts[1].trim().equalsIgnoreCase("Enabled");
                    playerNotificationPreferences.put(uuid, enabled);
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
        return playerNotificationPreferences.getOrDefault(uuid, true);
    }

    public void setNotificationsEnabled(UUID uuid, boolean enabled) {
        playerNotificationPreferences.put(uuid, enabled);
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

    private String getMobColorCode(EntityType entityType) {
        return switch (entityType) {
            case SPIDER, CAVE_SPIDER, DROWNED, VEX, WITCH, BREEZE, BOGGED, EVOKER, SHULKER, SILVERFISH, STRAY -> "§c";
            case ILLUSIONER, CREAKING, GIANT -> "§9";
            case BLAZE, MAGMA_CUBE, WARDEN, ELDER_GUARDIAN, WITHER -> "§4";
            case ENDERMAN, GUARDIAN, ZOMBIFIED_PIGLIN, ENDERMITE, PHANTOM, PILLAGER -> "§5";
            case PIG, COW, SHEEP, CHICKEN, ALLAY, ARMADILLO, AXOLOTL, WOLF, COD, BEE, FOX, GOAT , CAT, BAT, CAMEL, DONKEY, FROG, GLOW_SQUID, HORSE, MOOSHROOM, MULE, OCELOT, PARROT, PUFFERFISH, RABBIT, SALMON, SKELETON_HORSE, SNIFFER, SQUID, STRIDER, TADPOLE, TROPICAL_FISH, TURTLE, WANDERING_TRADER, LLAMA, TRADER_LLAMA -> "§a";
            case VILLAGER, IRON_GOLEM, SNOW_GOLEM, ZOMBIE_VILLAGER, ZOMBIE_HORSE, PANDA, DOLPHIN, POLAR_BEAR -> "§b";
            case HOGLIN, ZOGLIN, PIGLIN_BRUTE, RAVAGER, HUSK, VINDICATOR  -> "§6";
            case SLIME -> "§2";
            default -> "§f";
        };
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
}


