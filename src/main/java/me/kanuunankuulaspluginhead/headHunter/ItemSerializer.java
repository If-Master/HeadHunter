package me.kanuunankuulaspluginhead.headHunter;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class ItemSerializer {
    private static final ConcurrentHashMap<ItemStack, String> serializationCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

    private static final ThreadLocal<ByteArrayOutputStream> OUTPUT_BUFFER =
            ThreadLocal.withInitial(() -> new ByteArrayOutputStream(512));


    public static String serialize(ItemStack item) {
        if (item == null) return null;

        String cached = serializationCache.get(item);
        if (cached != null) {
            return cached;
        }

        ByteArrayOutputStream out = OUTPUT_BUFFER.get();
        out.reset();

        try (BukkitObjectOutputStream oos = new BukkitObjectOutputStream(out)) {
            oos.writeObject(item);

            String serialized = Base64.getEncoder().encodeToString(out.toByteArray());

            if (serializationCache.size() < MAX_CACHE_SIZE) {
                serializationCache.put(item.clone(), serialized);
            }

            return serialized;
        } catch (IOException e) {
            if (MainScript.instance != null) {
                MainScript.instance.getLogger().warning("Failed to serialize item: " + e.getMessage());
            }
            return null;
        } catch (Exception e) {
            if (MainScript.instance != null) {
                MainScript.instance.getLogger().severe("Unexpected error during item serialization: " + e.getMessage());
            }
            return null;
        }
    }


    public static ItemStack deserialize(String base64) {
        if (base64 == null || base64.isEmpty()) return null;

        try {
            byte[] data = Base64.getDecoder().decode(base64);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                 BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {

                Object obj = ois.readObject();

                if (obj instanceof ItemStack) {
                    return (ItemStack) obj;
                } else {
                    if (MainScript.instance != null) {
                        MainScript.instance.getLogger().warning("Deserialized object is not an ItemStack");
                    }
                    return null;
                }
            }
        } catch (IOException e) {
            if (MainScript.instance != null) {
                MainScript.instance.getLogger().warning("Failed to deserialize item (IO): " + e.getMessage());
            }
            return null;
        } catch (ClassNotFoundException e) {
            if (MainScript.instance != null) {
                MainScript.instance.getLogger().warning("Failed to deserialize item (class not found): " + e.getMessage());
                MainScript.instance.getLogger().warning("This may indicate version incompatibility - item will be skipped");
            }
            return null;
        } catch (IllegalArgumentException e) {
            if (MainScript.instance != null) {
                MainScript.instance.getLogger().warning("Failed to deserialize item (invalid data): " + e.getMessage());
            }
            return null;
        } catch (Exception e) {
            if (MainScript.instance != null) {
                MainScript.instance.getLogger().warning("Unexpected error during deserialization: " + e.getMessage());
                MainScript.instance.getLogger().warning("This item was likely created in a different Minecraft version and cannot be loaded");
            }
            return null;
        }
    }


    public static ItemStack deserializeWithFallback(String base64, ItemStack fallback) {
        ItemStack result = deserialize(base64);
        return result != null ? result : fallback;
    }

    public static void clearCache() {
        serializationCache.clear();
    }

    public static int getCacheSize() {
        return serializationCache.size();
    }
}
