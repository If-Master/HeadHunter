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
        }
    }

    public static ItemStack deserialize(String base64) {
        if (base64 == null || base64.isEmpty()) return null;

        try {
            byte[] data = Base64.getDecoder().decode(base64);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                 BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {

                return (ItemStack) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            if (MainScript.instance != null) {
                MainScript.instance.getLogger().warning("Failed to deserialize item: " + e.getMessage());
            }
            return null;
        }
    }

    public static void clearCache() {
        serializationCache.clear();
    }

    public static int getCacheSize() {
        return serializationCache.size();
    }
}
