package me.kanuunankuulaspluginhead.headHunter;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import static me.kanuunankuulaspluginhead.headHunter.MainScript.*;
import static org.bukkit.Bukkit.getLogger;

public class SoundScript implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.getBoolean("head-sound-effects.enabled", true)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.NOTE_BLOCK) return;

        Block noteBlock = event.getClickedBlock();
        Block headBlock = noteBlock.getRelative(BlockFace.UP);

        if (headBlock.getType() != Material.PLAYER_HEAD && headBlock.getType() != Material.PLAYER_WALL_HEAD) {
            return;
        }

        if (!(headBlock.getState() instanceof Skull skull)) return;

        if (!skull.getPersistentDataContainer().has(headTypeKey, PersistentDataType.STRING)) {
            return;
        }

        String headTypeString = skull.getPersistentDataContainer().get(headTypeKey, PersistentDataType.STRING);

        // Check if we can actually interact (not cancelled by protection plugins)
        if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
            return;
        }

        // Cancel the event to prevent note block sound
        event.setCancelled(true);

        Location soundLocation = noteBlock.getLocation().add(0.5, 0.5, 0.5);
        float volume = (float) config.getDouble("head-sound-effects.volume", 1.0);
        float pitch = (float) config.getDouble("head-sound-effects.pitch", 1.0);

        if ("PLAYER".equals(headTypeString)) {
            noteBlock.getWorld().playSound(soundLocation, Sound.ENTITY_PLAYER_BREATH, volume, pitch);

            if (config.getBoolean("head-sound-effects.messages", false)) {
                String headOwner = skull.getPersistentDataContainer().get(headOwnerKey, PersistentDataType.STRING);
                if (headOwner != null) {
                    event.getPlayer().sendMessage("§7*" + headOwner + " sounds*");
                }
            }
        } else {
            try {
                EntityType entityType = EntityType.valueOf(headTypeString);
                Sound animalSound = AnimalData.getAnimalSound(entityType);

                if (animalSound != null) {
                    noteBlock.getWorld().playSound(soundLocation, animalSound, volume, pitch);

                    if (config.getBoolean("head-sound-effects.messages", false)) {
                        String mobName = getDisplayName(entityType);
                        event.getPlayer().sendMessage("§7*" + mobName + " sounds*");
                    }
                } else {
                    getLogger().info("No sound found for entity type: " + entityType);
                }
            } catch (IllegalArgumentException e) {
            }
        }

        if (config.getBoolean("head-sound-effects.particles", true)) {
            noteBlock.getWorld().spawnParticle(
                    org.bukkit.Particle.NOTE,
                    noteBlock.getLocation().add(0.5, 1.5, 0.5),
                    5,
                    0.5, 0.5, 0.5,
                    0.1
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public static void onNoteBlockPlay(org.bukkit.event.block.NotePlayEvent event) {
        if (!config.getBoolean("head-sound-effects.enabled", true)) return;

        // Check if the event was cancelled by protection plugins
        if (event.isCancelled()) return;

        Block noteBlock = event.getBlock();
        Block headBlock = noteBlock.getRelative(BlockFace.UP);

        if (headBlock.getType() != Material.PLAYER_HEAD && headBlock.getType() != Material.PLAYER_WALL_HEAD) {
            return;
        }

        if (!(headBlock.getState() instanceof Skull skull)) return;

        if (!skull.getPersistentDataContainer().has(headTypeKey, PersistentDataType.STRING)) {
            return;
        }

        String headTypeString = skull.getPersistentDataContainer().get(headTypeKey, PersistentDataType.STRING);

        // Cancel the original note block sound
        event.setCancelled(true);

        Location soundLocation = noteBlock.getLocation().add(0.5, 0.5, 0.5);
        float volume = (float) config.getDouble("head-sound-effects.volume", 1.0);
        float pitch = (float) config.getDouble("head-sound-effects.pitch", 1.0);

        if ("PLAYER".equals(headTypeString)) {
            noteBlock.getWorld().playSound(soundLocation, Sound.ENTITY_PLAYER_BREATH, volume, pitch);
        } else {
            try {
                EntityType entityType = EntityType.valueOf(headTypeString);
                Sound animalSound = AnimalData.getAnimalSound(entityType);

                if (animalSound != null) {
                    noteBlock.getWorld().playSound(soundLocation, animalSound, volume, pitch);
                }
            } catch (IllegalArgumentException e) {
            }
        }

        if (config.getBoolean("head-sound-effects.particles", true)) {
            noteBlock.getWorld().spawnParticle(
                    org.bukkit.Particle.NOTE,
                    noteBlock.getLocation().add(0.5, 1.5, 0.5),
                    5,
                    0.5, 0.5, 0.5,
                    0.1
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public static void onHeadPlacedOnNoteblock(BlockPlaceEvent event) {
        if (event.getItemInHand().getType() != Material.PLAYER_HEAD) return;
        if (!event.getItemInHand().hasItemMeta()) return;

        // Check if the block placement was cancelled by protection plugins
        if (event.isCancelled()) return;

        Block placedBlock = event.getBlockPlaced();
        Block blockBelow = placedBlock.getRelative(BlockFace.DOWN);

        if (blockBelow.getType() != Material.NOTE_BLOCK) return;

        SkullMeta meta = (SkullMeta) event.getItemInHand().getItemMeta();
        if (!meta.getPersistentDataContainer().has(headTypeKey, PersistentDataType.STRING)) {
            return;
        }

        String headTypeString = meta.getPersistentDataContainer().get(headTypeKey, PersistentDataType.STRING);
        if ("PLAYER".equals(headTypeString)) return;

        runLater(placedBlock.getLocation(), () -> {
            try {
                EntityType entityType = EntityType.valueOf(headTypeString);
                Sound animalSound = AnimalData.getAnimalSound(entityType);

                if (animalSound != null && config.getBoolean("head-sound-effects.play-on-place", true)) {
                    Location soundLocation = placedBlock.getLocation().add(0.5, 0.5, 0.5);
                    placedBlock.getWorld().playSound(soundLocation, animalSound, 0.7f, 1.0f);

                    if (config.getBoolean("head-sound-effects.particles", true)) {
                        placedBlock.getWorld().spawnParticle(
                                org.bukkit.Particle.HEART,
                                soundLocation,
                                3,
                                0.3, 0.3, 0.3,
                                0.1
                        );
                    }
                }
            } catch (IllegalArgumentException e) {
            }
        }, 5L);
    }
}