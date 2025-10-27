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

public class SoundScript implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
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

        if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
            return;
        }

        event.setCancelled(true);

        Location soundLocation = noteBlock.getLocation().add(0.5, 0.5, 0.5);
        float volume = (float) config.getDouble("head-sound-effects.volume", 1.0);
        float pitch = (float) config.getDouble("head-sound-effects.pitch", 1.0);

        Sound sound = null;
        String displayName = null;

        if ("PLAYER".equals(headTypeString)) {
            sound = DynamicSoundManager.getSound(EntityType.PLAYER);
            if (sound == null) {
                sound = Sound.ENTITY_PLAYER_BREATH;
            }
            displayName = skull.getPersistentDataContainer().get(headOwnerKey, PersistentDataType.STRING);
        } else {
            try {
                EntityType entityType = EntityType.valueOf(headTypeString);
                sound = DynamicSoundManager.getSound(entityType);
                displayName = getDisplayName(entityType);
            } catch (IllegalArgumentException e) {
            }
        }

        if (sound != null) {
            noteBlock.getWorld().playSound(soundLocation, sound, volume, pitch);

            if (config.getBoolean("head-sound-effects.messages", false) && displayName != null) {
                event.getPlayer().sendMessage("§7*" + displayName + " sounds*");
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
    public void onNoteBlockPlay(org.bukkit.event.block.NotePlayEvent event) {
        if (!config.getBoolean("head-sound-effects.enabled", true)) return;
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
        event.setCancelled(true);

        Location soundLocation = noteBlock.getLocation().add(0.5, 0.5, 0.5);
        float volume = (float) config.getDouble("head-sound-effects.volume", 1.0);
        float pitch = (float) config.getDouble("head-sound-effects.pitch", 1.0);

        Sound sound = null;

        if ("PLAYER".equals(headTypeString)) {
            sound = DynamicSoundManager.getSound(EntityType.PLAYER);
            if (sound == null) {
                sound = Sound.ENTITY_PLAYER_BREATH;
            }
        } else {
            try {
                EntityType entityType = EntityType.valueOf(headTypeString);
                sound = DynamicSoundManager.getSound(entityType);
            } catch (IllegalArgumentException e) {
            }
        }

        if (sound != null) {
            noteBlock.getWorld().playSound(soundLocation, sound, volume, pitch);
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
    public void onHeadPlacedOnNoteblock(BlockPlaceEvent event) {
        if (event.getItemInHand().getType() != Material.PLAYER_HEAD) return;
        if (!event.getItemInHand().hasItemMeta()) return;
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
                Sound sound = DynamicSoundManager.getSound(entityType);

                if (sound != null && config.getBoolean("head-sound-effects.play-on-place", true)) {
                    Location soundLocation = placedBlock.getLocation().add(0.5, 0.5, 0.5);
                    placedBlock.getWorld().playSound(soundLocation, sound, 0.7f, 1.0f);

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
